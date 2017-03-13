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
package com.b2international.snowowl.snomed.exporter.server.sandbox;

import static com.b2international.snowowl.datastore.index.IndexUtils.getIntValue;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.EXISTENTIAL_RESTRICTION_MODIFIER;
import static com.b2international.snowowl.snomed.SnomedConstants.Concepts.UNIVERSAL_RESTRICTION_MODIFIER;
import static com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants.RELATIONSHIP_NUMBER;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.COMPONENT_ACTIVE;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.COMPONENT_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_ATTRIBUTE_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_CHARACTERISTIC_TYPE_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_EFFECTIVE_TIME;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_GROUP;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_MODULE_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_OBJECT_ID;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_UNIVERSAL;
import static com.b2international.snowowl.snomed.datastore.browser.SnomedIndexBrowserConstants.RELATIONSHIP_VALUE_ID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;

import java.util.Set;

import org.apache.lucene.document.Document;

import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.exporter.server.ComponentExportType;

/**
 * RF2 exporter for SNOMED&nbsp;CT relationships.
 *
 */
public class SnomedRelationshipExporter extends SnomedCoreExporter {

	private static final Set<String> FIELDS_TO_LOAD = unmodifiableSet(newHashSet(
			COMPONENT_ID,
			RELATIONSHIP_EFFECTIVE_TIME,
			COMPONENT_ACTIVE,
			RELATIONSHIP_MODULE_ID,
			RELATIONSHIP_OBJECT_ID,
			RELATIONSHIP_VALUE_ID,
			RELATIONSHIP_GROUP,
			RELATIONSHIP_ATTRIBUTE_ID,
			RELATIONSHIP_CHARACTERISTIC_TYPE_ID,
			RELATIONSHIP_UNIVERSAL
		));
	
	public SnomedRelationshipExporter(final SnomedExportConfiguration configuration) {
		super(checkNotNull(configuration, "configuration"));
	}
	
	@Override
	public Set<String> getFieldsToLoad() {
		return FIELDS_TO_LOAD;
	}

	@Override
	public String transform(final Document doc) {
		final StringBuilder sb = new StringBuilder();
		sb.append(doc.get(COMPONENT_ID));
		sb.append(HT);
		sb.append(formatEffectiveTime(doc.getField(getEffectiveTimeField())));
		sb.append(HT);
		sb.append(getIntValue(doc.getField(COMPONENT_ACTIVE)));
		sb.append(HT);
		sb.append(doc.get(RELATIONSHIP_MODULE_ID));
		sb.append(HT);
		sb.append(doc.get(RELATIONSHIP_OBJECT_ID));
		sb.append(HT);
		sb.append(doc.get(RELATIONSHIP_VALUE_ID));
		sb.append(HT);
		sb.append(doc.get(RELATIONSHIP_GROUP));
		sb.append(HT);
		sb.append(doc.get(RELATIONSHIP_ATTRIBUTE_ID));
		sb.append(HT);
		sb.append(doc.get(RELATIONSHIP_CHARACTERISTIC_TYPE_ID));
		sb.append(HT);
		sb.append(getModifierValue(doc));
		return sb.toString();
	}

	@Override
	public ComponentExportType getType() {
		return ComponentExportType.RELATIONSHIP;
	}
	
	@Override
	public String[] getColumnHeaders() {
		return SnomedRf2Headers.RELATIONSHIP_HEADER;
	}
	
	@Override
	protected int getTerminologyComponentType() {
		return RELATIONSHIP_NUMBER;
	}

	@Override
	protected String getEffectiveTimeField() {
		return RELATIONSHIP_EFFECTIVE_TIME;
	}
	
	private String getModifierValue(final Document doc) {
		return 1 == getIntValue(doc.getField(RELATIONSHIP_UNIVERSAL)) 
				? UNIVERSAL_RESTRICTION_MODIFIER 
				: EXISTENTIAL_RESTRICTION_MODIFIER;
	}

}