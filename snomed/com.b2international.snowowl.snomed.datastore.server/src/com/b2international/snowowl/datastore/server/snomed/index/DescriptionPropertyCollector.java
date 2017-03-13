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
package com.b2international.snowowl.datastore.server.snomed.index;

import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.DESCRIPTION_CASE_SIGNIFICANCE_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.DESCRIPTION_CONCEPT_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.DESCRIPTION_MODULE_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.DESCRIPTION_TYPE_ID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.NumericDocValues;

import bak.pcj.LongCollection;

/**
 * Collector for gathering the container concept ID, the description type ID and the case significance concept IDs of each
 * description.
 */
public class DescriptionPropertyCollector extends ComponentPropertyCollector {

	private NumericDocValues conceptIds;
	private NumericDocValues typeIds;
	private NumericDocValues caseSignificanceIds;
	private NumericDocValues moduleIds;

	public DescriptionPropertyCollector(final LongCollection acceptedIds) {
		super(checkNotNull(acceptedIds, "acceptedIds"));
	}

	@Override
	protected void initDocValues(final AtomicReader leafReader) throws IOException {
		super.initDocValues(leafReader);
		conceptIds = leafReader.getNumericDocValues(DESCRIPTION_CONCEPT_ID);
		moduleIds = leafReader.getNumericDocValues(DESCRIPTION_MODULE_ID);
		typeIds = leafReader.getNumericDocValues(DESCRIPTION_TYPE_ID);
		caseSignificanceIds = leafReader.getNumericDocValues(DESCRIPTION_CASE_SIGNIFICANCE_ID);
	}

	@Override
	protected boolean isLeafCollectible() {
		return super.isLeafCollectible()
				&& conceptIds != null
				&& moduleIds != null
				&& typeIds != null
				&& caseSignificanceIds != null;
	}

	@Override
	protected long[] collectProperties(final int docId) {
		return new long[] { 
				conceptIds.get(docId), 
				moduleIds.get(docId), 
				typeIds.get(docId), 
				caseSignificanceIds.get(docId) 
		};
	}
}