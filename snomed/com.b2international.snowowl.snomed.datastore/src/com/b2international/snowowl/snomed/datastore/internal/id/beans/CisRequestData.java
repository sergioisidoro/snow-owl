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
 * Superclass to hold common bean properties for Json requests sent to the CIS. 
 *
 */
public class CisRequestData {

	private int namespace;
	
	private String software = "Snow Owl";
	
	private String comment = "Requested by Snow Owl";
	
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

}
