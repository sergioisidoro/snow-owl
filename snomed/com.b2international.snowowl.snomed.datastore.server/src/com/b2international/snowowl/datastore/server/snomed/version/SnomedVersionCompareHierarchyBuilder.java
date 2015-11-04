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
package com.b2international.snowowl.datastore.server.snomed.version;

import static com.b2international.commons.ChangeKind.UNCHANGED;
import static com.b2international.snowowl.core.ApplicationContext.getServiceForClass;
import static com.b2international.snowowl.datastore.cdo.CDOUtils.NO_STORAGE_KEY;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.ROOT_CONCEPT;
import static com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants.CONCEPT_NUMBER;
import static com.b2international.snowowl.snomed.datastore.services.SnomedConceptNameProvider.INSTANCE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;

import java.util.Collection;
import java.util.Comparator;

import com.b2international.snowowl.core.api.IComponentIconIdProvider;
import com.b2international.snowowl.core.api.IComponentNameProvider;
import com.b2international.snowowl.core.api.browser.ExtendedComponentProvider;
import com.b2international.snowowl.core.api.browser.SuperTypeIdProvider;
import com.b2international.snowowl.datastore.index.diff.CompareResult;
import com.b2international.snowowl.datastore.index.diff.NodeDiff;
import com.b2international.snowowl.datastore.index.diff.NodeDiffImpl;
import com.b2international.snowowl.datastore.index.diff.VersionCompareConfiguration;
import com.b2international.snowowl.datastore.server.version.VersionCompareHierarchyBuilderImpl;
import com.b2international.snowowl.datastore.version.NodeDiffPredicate;
import com.b2international.snowowl.snomed.datastore.SnomedConceptIconIdProvider;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.SnomedCachingSuperTypeIdProvider;
import com.google.common.base.Predicate;

/**
 * Version compare hierarchy builder implementation for the SNOMED&nbsp;CT ontology.
 */
public class SnomedVersionCompareHierarchyBuilder extends VersionCompareHierarchyBuilderImpl {

	private static final String INACTIVE_SNOMED_CT_CONCEPTS_LABEL = "Inactive SNOMED CT Concepts";
	private static final String INACTIVE_SNOMED_CT_CONCEPTS_ID = "-1";
	private static final IComponentIconIdProvider<String> ICON_ID_PROVIDER = new SnomedConceptIconIdProvider();
	private static final Comparator<NodeDiff> INACTIVE_ALWAYS_LAST_LABEL_COMPARATOR = new Comparator<NodeDiff>() {
		@Override public int compare(final NodeDiff o1, final NodeDiff o2) {
			if (o1 != o2) {
				if (INACTIVE_SNOMED_CT_CONCEPTS_ID.equals(o1.getId())) {
					return 1;
				} else if (INACTIVE_SNOMED_CT_CONCEPTS_ID.equals(o2.getId())) {
					return -1;
				}
			}
			return LABEL_COMPARATOR.compare(o1, o2);
		}
	}; 

	private final Predicate<NodeDiff> topLevelNodePredicate = new NodeDiffPredicate() {
		@Override public boolean apply(final NodeDiff nodeDiff) {
			return isTopLevel(checkNotNull(nodeDiff, "nodeDiff"));
		}
	};
	
	private final SuperTypeIdProvider<String> idProvider = new SnomedCachingSuperTypeIdProvider();
	
	@Override
	protected IComponentIconIdProvider<String> getIconIdProvider() {
		return ICON_ID_PROVIDER;
	}
	
	@Override
	protected IComponentNameProvider getNameProvider() {
		return INSTANCE;
	}
	
	@Override
	protected short getTerminologyComponentId() {
		return CONCEPT_NUMBER;
	}
	
	@Override
	public boolean isRoot(final NodeDiff node) {
		return ROOT_CONCEPT.equals(checkNotNull(node, "node").getId()) 
				|| INACTIVE_SNOMED_CT_CONCEPTS_ID.equals(node.getId());
	}

	@Override
	public CompareResult createCompareResult(final VersionCompareConfiguration configuration, final Collection<NodeDiff> changedNodes) {
		
		checkNotNull(configuration, "configuration");
		checkNotNull(changedNodes, "changedNodes");
		
		boolean hasInactiveNode = false;
		final NodeDiffImpl fakeGroupNode = createFakeInactiveGroupNode();
		for (final NodeDiff diff : changedNodes) {
			if (null == diff.getParent() && !isRoot(diff)) {
				((NodeDiffImpl) diff).setParent(fakeGroupNode);
				fakeGroupNode.addChild(diff);
				hasInactiveNode = true;
			}
		}
		
		if (hasInactiveNode) {
			changedNodes.add(fakeGroupNode);
		}
		
		return super.createCompareResult(configuration, changedNodes);
	}

	@Override
	protected Predicate<NodeDiff> getNodeFilterPredicate() {
		return and(super.getNodeFilterPredicate(), not(topLevelNodePredicate));
	}

	@Override
	protected ExtendedComponentProvider getExtendedComponentProvider() {
		return getServiceForClass(SnomedTerminologyBrowser.class);
	}

	@Override
	protected SuperTypeIdProvider<String> getSuperTypeIdProvider() {
		return idProvider;
	}
	
	@Override
	protected Comparator<NodeDiff> getComparator() {
		return INACTIVE_ALWAYS_LAST_LABEL_COMPARATOR;
	}

	private boolean isTopLevel(final NodeDiff diff) {
		return null != diff.getParent() && isRoot(diff.getParent());
	}
	
	private NodeDiffImpl createFakeInactiveGroupNode() { 
		return new NodeDiffImpl(CONCEPT_NUMBER, NO_STORAGE_KEY, INACTIVE_SNOMED_CT_CONCEPTS_ID, 
			INACTIVE_SNOMED_CT_CONCEPTS_LABEL, ROOT_CONCEPT, null, UNCHANGED);
	}
	
}