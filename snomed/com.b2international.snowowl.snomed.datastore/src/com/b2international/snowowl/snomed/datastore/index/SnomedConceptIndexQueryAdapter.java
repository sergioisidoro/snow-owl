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
package com.b2international.snowowl.snomed.datastore.index;

import static com.b2international.snowowl.datastore.index.IndexUtils.getBooleanValue;

import javax.annotation.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Sort;

import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.index.IndexQueryBuilder;
import com.b2international.snowowl.datastore.index.IndexUtils;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.datastore.SnomedConceptIndexEntry;
import com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants;

/**
 * Common abstract superclass for SNOMED CT concept-related query adapters.
 *
 */
public abstract class SnomedConceptIndexQueryAdapter extends SnomedDslIndexQueryAdapter<SnomedConceptIndexEntry> {

	private static final long serialVersionUID = -3940497152360882631L;

	public static final int SEARCH_ACTIVE_CONCEPTS = 1 << 0;
	public static final int SEARCH_BY_CONCEPT_ID = 1 << 1;
	public static final int SEARCH_BY_FSN = 1 << 2;
	public static final int SEARCH_BY_LABEL = 1 << 3;
	public static final int SEARCH_BY_SYNONYM = 1 << 4;
	public static final int SEARCH_BY_OTHER = 1 << 5;
	public static final int SEARCH_STORAGE_KEY = 1 << 6;
	
	protected SnomedConceptIndexQueryAdapter(final String searchString, final int searchFlags, final String[] componentIds) {
		super(searchString, searchFlags, componentIds);
	}
	
	@Override
	public @Nullable Sort createSort() {
		return IndexUtils.DEFAULT_SORT;
	}
	
	@Override
	protected IndexQueryBuilder createIndexQueryBuilder() {
		return super.createIndexQueryBuilder()
				.requireExactTerm(SnomedIndexBrowserConstants.COMPONENT_TYPE, IndexUtils.intToPrefixCoded(SnomedTerminologyComponentConstants.CONCEPT_NUMBER))
				.requireExactTermIf(anyFlagSet(SEARCH_ACTIVE_CONCEPTS), SnomedIndexBrowserConstants.COMPONENT_ACTIVE, IndexUtils.intToPrefixCoded(1));
	}
	
	@Override
	public SnomedConceptIndexEntry buildSearchResult(final Document doc, final IBranchPath branchPath, final float score) {

		final String id = doc.get(SnomedIndexBrowserConstants.COMPONENT_ID);
		final String label = doc.get(SnomedIndexBrowserConstants.COMPONENT_LABEL);
		final String moduleId = doc.get(SnomedIndexBrowserConstants.CONCEPT_MODULE_ID);
		final IndexableField storageKeyField = doc.getField(SnomedIndexBrowserConstants.COMPONENT_STORAGE_KEY);
		final long storageKey = storageKeyField.numericValue().longValue();
		final byte flags = SnomedConceptIndexEntry.generateFlags(getBooleanValue(doc.getField(SnomedIndexBrowserConstants.COMPONENT_ACTIVE)), 
				getBooleanValue(doc.getField(SnomedIndexBrowserConstants.CONCEPT_PRIMITIVE)),
				getBooleanValue(doc.getField(SnomedIndexBrowserConstants.CONCEPT_EXHAUSTIVE)),
				getBooleanValue(doc.getField(SnomedIndexBrowserConstants.COMPONENT_RELEASED)));
		final String iconId = doc.get(SnomedIndexBrowserConstants.COMPONENT_ICON_ID);
		final long effectiveTime = IndexUtils.getLongValue(doc.getField(SnomedIndexBrowserConstants.CONCEPT_EFFECTIVE_TIME));
		final SnomedConceptIndexEntry conceptMini = new SnomedConceptIndexEntry(id, moduleId, label, iconId, score, storageKey, flags, effectiveTime);
		return conceptMini;
	}
}