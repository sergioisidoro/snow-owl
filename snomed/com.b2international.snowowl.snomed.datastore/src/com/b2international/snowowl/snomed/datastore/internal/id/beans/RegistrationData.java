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

/**
 * Bean to represent id registration data used by IHTSDO's
 * ID generation service.
 * Instances of this class are meant to be serialized and deserialized
 * to and from Json.
 *
 *	{
 * 		"sctId" : "string"
 * 		"namespace": 0,
 * 		"systemId": "string",
 * 		"software": "string",
 * 		"comment": "string",
 *	}
 */
public class RegistrationData extends RequestData {
	
	private String sctId;
	
	private String systemId = "SnowOwl";
	
	/**
	 * Returns the SNOMED CT identifier to be registered
	 * @return
	 */
	public String getSctId() {
		return sctId;
	}

	/**
	 * Sets the SNOMED CT identifier to be registered
	 * @param sctId
	 */
	public void setSctId(String sctId) {
		this.sctId = sctId;
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
	
}
