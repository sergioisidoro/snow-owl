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
package com.b2international.snowowl.snomed.datastore.internal.id;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Bean to represent id registration data used by IHTSDO's
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
 *	}
 */
public class RegistrationData {
	
	private int namespace;
	
	private String systemId = "";
	
	private String software = "Snow Owl";
	
	private String comment = "Requested by Snow Owl";
	
	@JsonIgnore
	private ComponentCategory componentCategory;
	
	/**
	 * @return the namespace
	 */
	public int getNamespace() {
		return namespace;
	}

	/**
	 * @param namespace the namespace to set
	 */
	public void setNamespace(int namespace) {
		this.namespace = namespace;
	}

	/**
	 * @return the partitionId
	 */
	public String getPartitionId() {
		final StringBuilder buf = new StringBuilder();
		if (namespace == 0) {
			buf.append('0');
		} else {
			buf.append('1');
		}
		// append the second part of the partition-identifier
		buf.append(getComponentCategory().ordinal());
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
	 * @return the software
	 */
	public String getSoftware() {
		return software;
	}

	/**
	 * @param software the software to set
	 */
	public void setSoftware(String software) {
		this.software = software;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	public ComponentCategory getComponentCategory() {
		return componentCategory;
	}

	public void setComponentCategory(ComponentCategory componentCategory) {
		this.componentCategory = componentCategory;
	}

	
}
