/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.snomed.index.init;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.datastore.index.DocIdCollector;
import com.b2international.snowowl.datastore.index.DocIdCollector.DocIdsIterator;
import com.b2international.snowowl.datastore.index.IndexUtils;
import com.b2international.snowowl.datastore.server.index.IndexServerService;
import com.b2international.snowowl.datastore.server.snomed.index.init.ImportIndexServerService.TermType;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants;
import com.b2international.snowowl.snomed.datastore.browser.SnomedIndexQueries;
import com.b2international.snowowl.snomed.datastore.index.SnomedIndexService;
import com.b2international.snowowl.snomed.datastore.services.ISnomedComponentService;
import com.google.common.collect.ImmutableSet;

/**
 * 
 */
public class IndexBasedImportIndexServiceFeeder implements IImportIndexServiceFeeder {

	/*
	 * (non-Javadoc)
	 * @see com.b2international.snowowl.datastore.server.snomed.index.init.IImportIndexServiceFeeder#initContent(com.b2international.snowowl.datastore.server.snomed.index.init.ImportIndexServerService, com.b2international.snowowl.core.api.IBranchPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initContent(final ImportIndexServerService service, final IBranchPath branchPath, final IProgressMonitor monitor) {

		final ISnomedComponentService componentService = ApplicationContext.getInstance().getServiceChecked(ISnomedComponentService.class);
		
		registerDescriptionProperties(service, branchPath, componentService);
		service.commit();
		
		registerAcceptability(service, branchPath);
		service.commit();
	}

	private static final Set<String> LANGUAGE_MEMBER_FIELDS_TO_LOAD = ImmutableSet.of(
			SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_UUID,
			SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_REFERENCE_SET_ID,
			SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_REFERENCED_COMPONENT_ID,
			SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_ACCEPTABILITY_ID,
			SnomedIndexBrowserConstants.COMPONENT_ACTIVE
	);
	
	@SuppressWarnings("unchecked")
	private void registerAcceptability(final ImportIndexServerService service, final IBranchPath branchPath) {
		
		@SuppressWarnings("rawtypes")
		final IndexServerService indexService = (IndexServerService) ApplicationContext.getInstance().getServiceChecked(SnomedIndexService.class);
		final Query memberQuery = SnomedIndexQueries.LANGUAGE_REFSET_MEMBER_TYPE_QUERY;
		
		ReferenceManager<IndexSearcher> manager = null;
		IndexSearcher searcher = null;
		
		try {
			
			manager = indexService.getManager(branchPath);
			searcher = manager.acquire();

			final int maxDoc = indexService.maxDoc(branchPath);
			final DocIdCollector collector = DocIdCollector.create(maxDoc);
			
			indexService.search(branchPath, memberQuery, collector);
			final DocIdsIterator itr = collector.getDocIDs().iterator();
			
			while (itr.next()) {
				
				final Document doc = searcher.doc(itr.getDocID(), LANGUAGE_MEMBER_FIELDS_TO_LOAD);
				
				final String memberId = doc.get(SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_UUID);
				final String refSetId = doc.get(SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_REFERENCE_SET_ID);
				final String descriptionId = doc.get(SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_REFERENCED_COMPONENT_ID);
				final String acceptabilityId = doc.get(SnomedIndexBrowserConstants.REFERENCE_SET_MEMBER_ACCEPTABILITY_ID);
				
				final boolean preferred = Concepts.REFSET_DESCRIPTION_ACCEPTABILITY_PREFERRED.equals(acceptabilityId);
				final boolean active = IndexUtils.getBooleanValue(doc.getField(SnomedIndexBrowserConstants.COMPONENT_ACTIVE));
				
				service.registerAcceptability(descriptionId, refSetId, memberId, preferred, active);
			}
			
		} catch (final IOException e) {
			throw new SnowowlRuntimeException(e);
		} finally {
			
			if (null != manager && null != searcher) {
				
				try {
					manager.release(searcher);
				} catch (final IOException e) {
					throw new SnowowlRuntimeException(e);
				}
			}
		}
	}

	private void registerDescriptionProperties(final ImportIndexServerService service, final IBranchPath branchPath, final ISnomedComponentService componentService) {
		
		final String[][] descriptionProperties = componentService.getAllDescriptionProperties(branchPath);
		
		if (null == descriptionProperties) {
			return;
		}
		
		final Set<String> synonymAndDescendantIds = componentService.getSynonymAndDescendantIds(branchPath);
		
		for (final String[] property : descriptionProperties) {
			
			final String descriptionId = property[0];
			final String conceptId = property[1];
			final TermType termType = getTermType(synonymAndDescendantIds, property[2]);
			final String term = property[3];
			final CDOID descriptionCdoId = CDOIDUtil.createLong(Long.parseLong(property[4]));
			
			service.registerComponent(descriptionId, descriptionCdoId);
			
			// XXX: Assumes that nothing has been added to this import index service yet, as inactive descriptions are not populated at all.
			service.registerDescription(descriptionId, conceptId, term, termType, true);
		}
	}

	private TermType getTermType(final Set<String> synonymAndDescendantIds, final String typeId) {

		if (Concepts.FULLY_SPECIFIED_NAME.equals(typeId)) {
			return TermType.FSN;
		} else if (synonymAndDescendantIds.contains(typeId)) {
			return TermType.SYNONYM_AND_DESCENDANTS;
		} else {
			return TermType.OTHER;
		}
	}
}
