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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration.IdGenerationSource;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator;

/**
 * Factory to create a Snomed CT identifier generator
 */
public class SnomedIdentifierGeneratorFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedIdentifierGeneratorFactory.class);
	
	private static ISnomedIdentifierGenerator instance;
	
	public static synchronized ISnomedIdentifierGenerator create() {
		if (instance !=null ) {
			return instance;
		} else {
			SnowOwlConfiguration snowOwlConfiguration = SnowOwlApplication.INSTANCE.getConfiguration();
			SnomedCoreConfiguration coreConfiguration = snowOwlConfiguration.getModuleConfig(SnomedCoreConfiguration.class);
			
			IdGenerationSource idGenerationSource = coreConfiguration.getIdGenerationSource();
			if (idGenerationSource == null) {
				String errorMessage = "Id generation source is not configured.";
				LOGGER.error(errorMessage);
				throw new NullPointerException(errorMessage);
			}
			
			if (idGenerationSource == IdGenerationSource.INTERNAL) {
				LOGGER.info("Snow Owl is configured to use internal id generation.");
				instance = new SnowOwlSnomedIdentifierGenerator();
				return instance;
			} else if (idGenerationSource == IdGenerationSource.EXTERNAL_IHTSDO) {
				String externalIdGeneratorUrl = coreConfiguration.getExternalIdGeneratorUrl();
				String externalIdGeneratorPort = coreConfiguration.getExternalIdGeneratorPort();
				String externalIdGeneratorContextRoot = coreConfiguration.getExternalIdGeneratorContextRoot();
				instance = new IhtsdoSnomedIdentifierGenerator(externalIdGeneratorUrl, externalIdGeneratorPort, externalIdGeneratorContextRoot);
				System.out.println("core config: " + coreConfiguration);
				return instance;
			} else {
				String errorMessage = String.format("Unknown id generation source configured: %s. ", idGenerationSource);
				LOGGER.error(errorMessage);
				throw new IllegalArgumentException(errorMessage);
			}
		}
	}
}
