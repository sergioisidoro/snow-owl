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

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.setup.Environment;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration.IdGenerationSource;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator;
import com.b2international.snowowl.snomed.datastore.id.reservations.ISnomedIdentiferReservationService;
import com.b2international.snowowl.snomed.datastore.id.reservations.Reservations;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.CisExistingIdsReservation;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.SnomedIdentifierReservationServiceImpl;
import com.google.inject.Provider;

/**
 * Factory to create a SNOMED CT identifier generator
 */
public class SnomedIdentifierGeneratorFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnomedIdentifierGeneratorFactory.class);

	private static final String STORE_RESERVATIONS = "internal_store_reservations"; // $NON-NLS-N$

	/**
	 * Registers the id generator and reservation services based on the
	 * configuration.
	 * 
	 * @param configuration
	 * @param env
	 */
	public static void registerService(SnowOwlConfiguration configuration, Environment env) {

		SnomedCoreConfiguration coreConfiguration = configuration.getModuleConfig(SnomedCoreConfiguration.class);

		IdGenerationSource idGenerationSource = coreConfiguration.getIdGenerationSource();
		if (idGenerationSource == null) {
			String errorMessage = "Id generation source is not configured.";
			LOGGER.error(errorMessage);
			throw new NullPointerException(errorMessage);
		}

		//we reserve the already assigned ids from Snow Owl
		final ISnomedIdentiferReservationService reservationService = new SnomedIdentifierReservationServiceImpl();
		LOGGER.info("Snow Owl is configured to use internal id generation.");
		Provider<SnomedTerminologyBrowser> provider = new Provider<SnomedTerminologyBrowser>() {
			@Override
			public SnomedTerminologyBrowser get() {
				return ApplicationContext.getInstance().getService(SnomedTerminologyBrowser.class);
			}
		};
		reservationService.create(STORE_RESERVATIONS, Reservations.uniqueInStore(provider));
		
		//register the reservation service
		env.services().registerService(ISnomedIdentiferReservationService.class, reservationService);

		if (idGenerationSource == IdGenerationSource.INTERNAL) {

			final ISnomedIdentifierGenerator idGenerator = new DefaultSnomedIdentifierGenerator(reservationService);
			
			//register the internal id generator
			env.services().registerService(ISnomedIdentifierGenerator.class, idGenerator);

		} else if (idGenerationSource == IdGenerationSource.CIS) {

			
			String externalIdGeneratorUrl = coreConfiguration.getExternalIdGeneratorUrl();
			String externalIdGeneratorPort = coreConfiguration.getExternalIdGeneratorPort();
			String externalIdGeneratorContextRoot = coreConfiguration.getExternalIdGeneratorContextRoot();
			LOGGER.info("Snow Owl is configured to use ITHSDO's external id generation with URL: {}:{}/{}",
					externalIdGeneratorUrl, externalIdGeneratorPort, externalIdGeneratorContextRoot);
			CisSnomedIdentifierGenerator idGenerator = new CisSnomedIdentifierGenerator(externalIdGeneratorUrl,
					externalIdGeneratorPort, externalIdGeneratorContextRoot);

			reservationService.create(CisExistingIdsReservation.NAME, new CisExistingIdsReservation(externalIdGeneratorUrl,
					externalIdGeneratorPort, externalIdGeneratorContextRoot)); //$NON-NLS-N$
			
			//register the CIS id generator
			env.services().registerService(ISnomedIdentifierGenerator.class, idGenerator);
		} else {
			String errorMessage = String.format("Unknown id generation source configured: %s. ", idGenerationSource);
			LOGGER.error(errorMessage);
			throw new IllegalArgumentException(errorMessage);
		}
	}
}
