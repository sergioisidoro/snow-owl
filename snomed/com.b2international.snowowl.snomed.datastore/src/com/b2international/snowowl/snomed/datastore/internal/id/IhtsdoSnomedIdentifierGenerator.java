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
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator;
import com.google.common.base.Preconditions;

/**
 * Snomed CT identifier generator that delegates the id generation
 * to the external RESTful service hosted by IHTSDO.
 * More information on the service can be found here:
 * {@link https://confluence.ihtsdotools.org/display/TOOLS/IHTSDO+Component+Identifier+Service} 
 */
public class IhtsdoSnomedIdentifierGenerator implements ISnomedIdentifierGenerator {

	private String externalIdGeneratorUrl;
	private String externalIdGeneratorPort;
	private String externalIdGeneratorContextRoot;

	/**
	 * @param externalIdGeneratorUrl
	 * @param externalIdGeneratorPort
	 * @param externalIdGeneratorContextRoot
	 */
	public IhtsdoSnomedIdentifierGenerator(String externalIdGeneratorUrl, String externalIdGeneratorPort,
			String externalIdGeneratorContextRoot) {
		Preconditions.checkNotNull(externalIdGeneratorUrl, "External id generator URL is null.");
		this.externalIdGeneratorUrl = externalIdGeneratorUrl;
		this.externalIdGeneratorPort = externalIdGeneratorPort;
		this.externalIdGeneratorContextRoot = externalIdGeneratorContextRoot;
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator#generateId(com.b2international.snowowl.core.terminology.ComponentCategory)
	 */
	@Override
	public String generateId(ComponentCategory component) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator#generateId(com.b2international.snowowl.core.terminology.ComponentCategory, java.lang.String)
	 */
	@Override
	public String generateId(ComponentCategory component, String namespace) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IhtsdoSnomedIdentifierGenerator [externalIdGeneratorUrl=" + externalIdGeneratorUrl
				+ ", externalIdGeneratorPort=" + externalIdGeneratorPort + ", externalIdGeneratorContextRoot="
				+ externalIdGeneratorContextRoot + "]";
	}

}
