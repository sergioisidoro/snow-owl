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

import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;

/**
 * RF2 exporter for SNOMED CT relationships.
 */
public abstract class AbstractSnomedRelationshipExporter extends SnomedCoreExporter<SnomedRelationshipIndexEntry> {

	protected AbstractSnomedRelationshipExporter(final SnomedExportConfiguration configuration) {
		super(configuration, SnomedRelationshipIndexEntry.class);
	}

	@Override
	public String transform(final SnomedRelationshipIndexEntry doc) {
		final StringBuilder sb = new StringBuilder();
		sb.append(doc.getId());
		sb.append(HT);
		sb.append(formatEffectiveTime(doc.getEffectiveTime()));
		sb.append(HT);
		sb.append(doc.isActive() ? "1" : "0");
		sb.append(HT);
		sb.append(doc.getModuleId());
		sb.append(HT);
		sb.append(doc.getSourceId());
		sb.append(HT);
		sb.append(doc.getDestinationId());
		sb.append(HT);
		sb.append(doc.getGroup());
		sb.append(HT);
		sb.append(doc.getTypeId());
		sb.append(HT);
		sb.append(doc.getCharacteristicTypeId());
		sb.append(HT);
		sb.append(doc.getModifierId());
		return sb.toString();
	}

	@Override
	public String[] getColumnHeaders() {
		return SnomedRf2Headers.RELATIONSHIP_HEADER;
	}
}
