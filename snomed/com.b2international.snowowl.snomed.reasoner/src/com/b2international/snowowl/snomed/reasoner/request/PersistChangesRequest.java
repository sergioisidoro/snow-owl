/*
 * Copyright 2017-2018 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.reasoner.server.request;

import static com.b2international.snowowl.core.ApplicationContext.getServiceForClass;
import static com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContextDescriptions.CLASSIFY_WITH_REVIEW;
import static com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContextDescriptions.SAVE_CLASSIFICATION_RESULTS;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.collections.PrimitiveSets;
import com.b2international.collections.longs.LongCollection;
import com.b2international.collections.longs.LongSet;
import com.b2international.commons.exceptions.ApiError;
import com.b2international.index.revision.RevisionIndex;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.RepositoryManager;
import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.datastore.cdo.CDOServerCommitBuilder;
import com.b2international.snowowl.datastore.oplock.IOperationLockTarget;
import com.b2international.snowowl.datastore.oplock.OperationLockException;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreLockContext;
import com.b2international.snowowl.datastore.oplock.impl.DatastoreOperationLockException;
import com.b2international.snowowl.datastore.oplock.impl.IDatastoreOperationLockManager;
import com.b2international.snowowl.datastore.oplock.impl.SingleRepositoryAndBranchLockTarget;
import com.b2international.snowowl.datastore.server.snomed.index.ReasonerTaxonomyBuilder;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.common.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.datastore.ConcreteDomainFragment;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.SnomedEditingContext;
import com.b2international.snowowl.snomed.datastore.StatementFragment;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.id.SnomedNamespaceAndModuleAssigner;
import com.b2international.snowowl.snomed.datastore.id.SnomedNamespaceAndModuleAssignerProvider;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.b2international.snowowl.snomed.reasoner.server.classification.EquivalentConceptMerger;
import com.b2international.snowowl.snomed.reasoner.server.classification.ReasonerTaxonomy;
import com.b2international.snowowl.snomed.reasoner.server.diff.OntologyChangeRecorder;
import com.b2international.snowowl.snomed.reasoner.server.diff.concretedomain.ConcreteDomainPersister;
import com.b2international.snowowl.snomed.reasoner.server.diff.relationship.RelationshipPersister;
import com.b2international.snowowl.snomed.reasoner.server.normalform.ConceptConcreteDomainNormalFormGenerator;
import com.b2international.snowowl.snomed.reasoner.server.normalform.RelationshipNormalFormGenerator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * @since 5.7
 */
final class PersistChangesRequest implements Request<ServiceProvider, ApiError> {

	private static final Logger LOG = LoggerFactory.getLogger("reasoner");
	
	private static final long LOCK_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5L);

	@JsonProperty
	private final String classificationId;
	private final String userId;
	private final ReasonerTaxonomyBuilder taxonomyBuilder;

	private ReasonerTaxonomy taxonomy;
	private DatastoreLockContext lockContext;
	private IOperationLockTarget lockTarget;
	private LongSet statedDescendantsOfSmp;

	PersistChangesRequest(String classificationId, ReasonerTaxonomy taxonomy, ReasonerTaxonomyBuilder taxonomyBuilder, String userId) {
		this.classificationId = classificationId;
		this.taxonomy = taxonomy;
		this.taxonomyBuilder = taxonomyBuilder;
		this.userId = userId;
	}
	
	private boolean isConcreteDomainSupported() {
		return ApplicationContext.getInstance().getService(SnowOwlConfiguration.class).getModuleConfig(SnomedCoreConfiguration.class).isConcreteDomainSupported();
	}

	@Override
	public ApiError execute(ServiceProvider context) {
		IProgressMonitor monitor = context.service(IProgressMonitor.class);

		try {
			lockBeforeChanges();
			return persistChanges(context, monitor);
		} catch (Exception e) {
			return createApiError(e);
		} finally {
			monitor.done();
			cleanup();
		}
	}

	private ApiError createApiError(final Exception e) {
		String message = "Error while persisting classification changes on '" + taxonomy.getBranchPath() + "'.";
		LOG.error(message, e);
		return new ApiError.Builder(message).code(500).build();
	}

	private void lockBeforeChanges() {

		IBranchPath branchPath = taxonomy.getBranchPath();
		DatastoreLockContext localLockContext = createLockContext(userId);
		IOperationLockTarget localLockTarget = createLockTarget(branchPath);

		try {

			getLockManager().lock(localLockContext, LOCK_TIMEOUT_MILLIS, localLockTarget);
			lockContext = localLockContext;
			lockTarget = localLockTarget;

		} catch (OperationLockException | InterruptedException e) {
			DatastoreLockContext otherContext = null;
			if (e instanceof DatastoreOperationLockException) {
				otherContext = ((DatastoreOperationLockException) e).getContext(localLockTarget);
			}

			String reason = (null == otherContext) ? getDefaultContextDescription() : getContextDescription(otherContext);
			throw new DatastoreOperationLockException(reason);
		}
	}

	private ApiError persistChanges(ServiceProvider context, IProgressMonitor monitor) throws CommitException {

		if (null == taxonomy) {
			throw new IllegalStateException("Tried to run the same persist changes job twice.");
		}

		IBranchPath branchPath = taxonomy.getBranchPath();
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Persisting changes", 6);
		SnomedEditingContext editingContext = null;

		try {

			editingContext = new SnomedEditingContext(branchPath);

			SnomedNamespaceAndModuleAssigner namespaceAndModuleAssigner = context.service(SnomedNamespaceAndModuleAssignerProvider.class).get();
			LOG.info("Reasoner service will use the {} class for relationship/concrete domain namespace and module assignement.", namespaceAndModuleAssigner.getClass().getSimpleName());
			applyChanges(subMonitor, editingContext, namespaceAndModuleAssigner);
			fixEquivalences(editingContext);

			CDOTransaction editingContextTransaction = editingContext.getTransaction();
			editingContext.preCommit();

			new CDOServerCommitBuilder(userId, "Classified ontology.", editingContextTransaction)
					.parentContextDescription(SAVE_CLASSIFICATION_RESULTS)
					.commitOne(subMonitor.newChild(2));

			// register reserved IDs
			namespaceAndModuleAssigner.registerAllocatedIds();
			
			return new ApiError.Builder("OK").code(200).build();
		} catch (CommitException e) {
			if (editingContext != null) {
				editingContext.releaseIds();
			}
			throw e;
		} finally {
			if (editingContext != null) {
				editingContext.close();
			}
		}
	}

	private void applyChanges(SubMonitor subMonitor, SnomedEditingContext editingContext, SnomedNamespaceAndModuleAssigner namespaceAndModuleAssigner) {
		final OntologyChangeRecorder<StatementFragment> relationshipRecorder = new OntologyChangeRecorder<>();
		final OntologyChangeRecorder<ConcreteDomainFragment> concreteDomainRecorder = new OntologyChangeRecorder<>();
		recordChanges(subMonitor, relationshipRecorder, concreteDomainRecorder);
	
		namespaceAndModuleAssigner.allocateRelationshipIdsAndModules(relationshipRecorder.getAddedSubjects().keys(), editingContext);
		applyRelationshipChanges(editingContext, relationshipRecorder, namespaceAndModuleAssigner);
		
		if (isConcreteDomainSupported()) {
			namespaceAndModuleAssigner.allocateConcreteDomainModules(concreteDomainRecorder.getAddedSubjects().keySet(), editingContext);
			applyConcreteDomainChanges(editingContext, concreteDomainRecorder, namespaceAndModuleAssigner);
		}
	}

	private void recordChanges(final SubMonitor subMonitor,
			final OntologyChangeRecorder<StatementFragment> relationshipRecorder,
			final OntologyChangeRecorder<ConcreteDomainFragment> concreteDomainRecorder) {
		final RelationshipNormalFormGenerator relationshipGenerator = new RelationshipNormalFormGenerator(taxonomy, taxonomyBuilder);
		relationshipGenerator.collectNormalFormChanges(subMonitor.newChild(1), relationshipRecorder);
	
		final ConceptConcreteDomainNormalFormGenerator conceptConcreteDomainGenerator = new ConceptConcreteDomainNormalFormGenerator(taxonomy, taxonomyBuilder);
		conceptConcreteDomainGenerator.collectNormalFormChanges(subMonitor.newChild(1), concreteDomainRecorder);
	}

	private void applyRelationshipChanges(SnomedEditingContext editingContext, OntologyChangeRecorder<StatementFragment> relationshipRecorder, SnomedNamespaceAndModuleAssigner namespaceAndModuleAssigner) {
		final RelationshipPersister relationshipPersister = new RelationshipPersister(editingContext, namespaceAndModuleAssigner);
		
		for (Entry<String, StatementFragment> addedFragments : relationshipRecorder.getAddedSubjects().entries()) {
			relationshipPersister.handleAddedSubject(addedFragments.getKey(), addedFragments.getValue());
		}
		
		for (Entry<String, StatementFragment> removedFragments : relationshipRecorder.getRemovedSubjects().entries()) {
			relationshipPersister.handleRemovedSubject(removedFragments.getKey(), removedFragments.getValue());
		}
	}

	private void applyConcreteDomainChanges(SnomedEditingContext editingContext, OntologyChangeRecorder<ConcreteDomainFragment> concreteDomainRecorder, SnomedNamespaceAndModuleAssigner namespaceAndModuleAssigner) {
		final ConcreteDomainPersister concreteDomainPersister = new ConcreteDomainPersister(editingContext, namespaceAndModuleAssigner);
		
		for (Entry<String, ConcreteDomainFragment> addedFragments : concreteDomainRecorder.getAddedSubjects().entries()) {
			concreteDomainPersister.handleAddedSubject(addedFragments.getKey(), addedFragments.getValue());
		}
		
		for (Entry<String, ConcreteDomainFragment> removedFragments : concreteDomainRecorder.getRemovedSubjects().entries()) {
			concreteDomainPersister.handleRemovedSubject(removedFragments.getKey(), removedFragments.getValue());
		}
	}

	private void fixEquivalences(SnomedEditingContext editingContext) {
		final IBranchPath branchPath = taxonomy.getBranchPath();
		final List<LongCollection> equivalenciesToFix = Lists.newArrayList();
		
		for (LongCollection equivalentSet : taxonomy.getEquivalentConceptIds()) {
			long firstConceptId = equivalentSet.iterator().next();
			String firstConceptIdString = Long.toString(firstConceptId);
	
			// FIXME: make equivalence set to fix user-selectable, only subtype of SMP can be auto-merged
			if (isSubTypeOfSMP(branchPath, firstConceptIdString)) {
				equivalenciesToFix.add(equivalentSet);
			}
		}
	
		if (!equivalenciesToFix.isEmpty()) {
			new EquivalentConceptMerger(editingContext, equivalenciesToFix).fixEquivalencies();
		}
	}

	private boolean isSubTypeOfSMP(IBranchPath branchPath, String subTypeId) {
		if (statedDescendantsOfSmp == null) {
			statedDescendantsOfSmp = SnomedRequests.prepareGetConcept(Concepts.GENERATED_SINGAPORE_MEDICINAL_PRODUCT)
					.setExpand("statedDescendants(limit:"+Integer.MAX_VALUE+",direct:false)")
					.build(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath.getPath())
					.execute(ApplicationContext.getServiceForClass(IEventBus.class))
					.then(new Function<SnomedConcept, LongSet>() {
						@Override
						public LongSet apply(SnomedConcept input) {
							LongSet descendantIds = PrimitiveSets.newLongOpenHashSetWithExpectedSize(input.getStatedDescendants().getTotal());
							for (SnomedConcept descendant : input.getStatedDescendants()) {
								descendantIds.add(Long.parseLong(descendant.getId()));
							}
							return descendantIds;
						}
					})
					.fail(new Function<Throwable, LongSet>() {
						@Override
						public LongSet apply(Throwable input) {
							return PrimitiveSets.newLongOpenHashSet();
						}
					})
					.getSync();
		}
		return statedDescendantsOfSmp.contains(Long.parseLong(subTypeId));
	}

	private void cleanup() {
		try {

			if (null != lockContext && null != lockTarget) {
				getLockManager().unlock(lockContext, lockTarget);
			}

		} finally {
			lockContext = null;
			lockTarget = null;
			taxonomy = null;
		}		
	}

	private String getDefaultContextDescription() {
		return "of concurrent activity";
	}

	private String getContextDescription(DatastoreLockContext otherContext) {
		return otherContext.getUserId() + " is currently " + otherContext.getDescription();
	}

	private SingleRepositoryAndBranchLockTarget createLockTarget(IBranchPath branchPath) {
		return new SingleRepositoryAndBranchLockTarget(SnomedDatastoreActivator.REPOSITORY_UUID, branchPath);
	}

	private DatastoreLockContext createLockContext(String userId) {
		return new DatastoreLockContext(userId, SAVE_CLASSIFICATION_RESULTS, CLASSIFY_WITH_REVIEW);
	}

	private static IDatastoreOperationLockManager getLockManager() {
		return getServiceForClass(IDatastoreOperationLockManager.class);
	}

	private static RevisionIndex getIndex() {
		return getServiceForClass(RepositoryManager.class)
				.get(SnomedDatastoreActivator.REPOSITORY_UUID)
				.service(RevisionIndex.class);
	}

}