/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.provider;

import java.util.Collection;

import com.b2international.commons.platform.Extensions;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.fhir.core.LogicalId;
import com.b2international.snowowl.fhir.core.codesystems.OperationOutcomeCode;
import com.b2international.snowowl.fhir.core.model.valueset.ValueSet;
import com.google.common.collect.ImmutableList;

/**
 * Extension point interface for value set specific FHIR API support. 
 * 
 * @see 'com.b2international.snowowl.fhir.core.valueSetProvider' for the extension point definition
 * @since 6.4
 */
public interface IValueSetApiProvider extends IFhirApiProvider {
	
	/**
	 * Registry that reads and instantiates FHIR API supporting services registered by the actual terminology plug-ins.
	 * 
	 * @since 6.4
	 */
	enum Registry {
		
		INSTANCE;
		
		private final static String FHIR_EXTENSION_POINT = "com.b2international.snowowl.fhir.core.valueSetProvider"; //$NON-NLS-N$
		private final Collection<IValueSetApiProvider> providers;
		
		private Registry() {
			this.providers = ImmutableList.copyOf(Extensions.getExtensions(FHIR_EXTENSION_POINT, IValueSetApiProvider.class));
		}
		
		public static Collection<IValueSetApiProvider> getProviders() {
			return INSTANCE.providers;
		}
		
		/**
		 * Returns the matching {@link IValueSetApiProvider} for the given path (repository:branchPath).
		 * @param logical code system path (e.g.icd10Store:20140101)
		 * @return FHIR value set provider
		 * @throws com.b2international.snowowl.fhir.core.exceptions.BadRequestException - if provider is not found with the given path
		 */
		public static IValueSetApiProvider getValueSetProvider(LogicalId logicalId) {
			return getProviders().stream()
				.filter(provider -> provider.isSupported(logicalId))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Did not find FHIR module for managing value set: " + logicalId, OperationOutcomeCode.MSG_NO_MODULE, "system=" + logicalId));
		}
		
		/**
		 * Returns the matching {@link IValueSetApiProvider} for the given URI.
		 * @param uriValue
		 * @return FHIR value setprovider
		 */
		public static IValueSetApiProvider getValueSetProvider(String uriValue) {
			return getProviders().stream()
				.filter(provider -> provider.isSupported(uriValue))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Did not find FHIR module for managing value set: " + uriValue, OperationOutcomeCode.MSG_NO_MODULE, "system=" + uriValue));
		}
	}
	
	/**
	 * Returns the value sets supported by this provider.
	 * TODO: move this to a different extension. (probably an extension definition per resource)
	 * @return collection of value sets supported
	 */
	Collection<ValueSet> getValueSets();

	/**
	 * Returns the value set for the passed in logical id (repositoryId:branchPath/valueSetId)
	 * @param logicalId
	 * @return {@link ValueSet}
	 * @throws BadRequestException if the value set is not supported by this provider
	 */
	ValueSet getValueSet(LogicalId logicalId);

}