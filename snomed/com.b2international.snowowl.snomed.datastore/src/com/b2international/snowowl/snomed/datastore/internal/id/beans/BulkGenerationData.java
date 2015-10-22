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

import java.util.Collection;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

/**
 * 
 * Bean to represent bulk id generation data used by IHTSDO's
 * ID generation service.
 * Instances of this class are meant to be serialized and deserialized
 * to and from Json.
 *  {
 *	  "namespace": 0,
 *	  "partitionId": "string",
 *	  "quantity": 0,
 *	  "systemIds": [
 *	    "string"
 * 	  ],
 *	  "software": "string",
 *	  "comment": "string",
 *	  "generateLegacyIds": "false"
 *	  }
 */
public class BulkGenerationData extends CisRequestData {

	//the number of ids to be generated
	private int quantity;
	
	private Collection<String> systemIds = Lists.newArrayList();

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
	 * @return the systemIds
	 */
	public Collection<String> getSystemIds() {
		return systemIds;
	}

	/**
	 * @param systemIds the systemIds to set
	 */
	public void setSystemIds(Collection<String> systemIds) {
		this.systemIds = systemIds;
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

	/**
	 * Returns the requested number of ids to be generated
	 * @return
	 */
	public int getQuantity() {
		return quantity;
	}

	/**
	 * Sets the number of ids to be generated
	 * @param quantity
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
}
