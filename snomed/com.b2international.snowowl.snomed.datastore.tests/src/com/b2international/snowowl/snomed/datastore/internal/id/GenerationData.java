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

/**
 *{
 * "namespace": 0,
 * "partitionId": "string",
 * "systemId": "string",
 * "software": "string",
 * "comment": "string",
 * "generateLegacyIds": "false"
}
 */
public class GenerationData {
	
	private int namespace;
	
	private String partitionId;
	
	private String systemId = "";
	
	private String software = "Snow Owl";
	
	private String comment = "Testing";
	
	private String generateLegacyIds = "false";
	
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
		return partitionId;
	}

	/**
	 * @param partitionId the partitionId to set
	 */
	public void setPartitionId(String partitionId) {
		this.partitionId = partitionId;
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

	
}
