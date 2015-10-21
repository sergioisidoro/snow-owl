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
package com.b2international.snowowl.snomed.datastore.internal.id.beans;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Bean to represent id generation data used by IHTSDO's
 * ID generation service.
 * Instances of this class are meant to be serialized and deserialized
 * to and from Json.
 *
 *	{
 * 		"namespace": 0,
 * 		"partitionId": "string",
 * 		"systemId": "string",
 * 		"software": "string",
 * 		"comment": "string",
 * 		"generateLegacyIds": "false"
 *	}
 */
public class GenerationData extends CisRequestData {
	
	private String systemId = "";

	private String generateLegacyIds = "false";
	
	@JsonIgnore
	private ComponentCategory componentCategory;

	/**
	 * @return the partitionId
	 */
	public String getPartitionId() {
		final StringBuilder buf = new StringBuilder();
		if (getNamespace() == 0) {
			buf.append('0');
		} else {
			buf.append('1');
		}
		// append the second part of the partition-identifier
		buf.append(componentCategory.ordinal());
		return buf.toString();
	}
	
	/**
	 * @return the systemId
	 */
	public String getSystemId() {
		return systemId;
	}

	/**
	 * @param systemId the systemId to set
	 */
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
	
	/**
	 * @return the generateLegacyIds
	 */
	public String getGenerateLegacyIds() {
		return generateLegacyIds;
	}

	/**
	 * @param generateLegacyIds the generateLegacyIds to set
	 */
	public void setGenerateLegacyIds(String generateLegacyIds) {
		this.generateLegacyIds = generateLegacyIds;
	}
	
	/**
	 * Returns the {@link ComponentCategory} of the id to be generated
	 * 
	 * @return
	 */
	public ComponentCategory getComponentCategory() {
		return componentCategory;
	}

	/**
	 * Sets the {@link ComponentCategory} of the id to be generated
	 * @param componentCategory
	 */
	public void setComponentCategory(ComponentCategory componentCategory) {
		this.componentCategory = componentCategory;
	}
	
}
