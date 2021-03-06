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
package com.b2international.snowowl.snomed.api.domain.exception;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

import com.b2international.commons.exceptions.NotFoundException;

/**
 * Thrown when a SNOMED CT import configuration with a particular identifier could not be found.
 */
public class SnomedImportConfigurationNotFoundException extends NotFoundException {

	private static final long serialVersionUID = 4991544292465337742L;

	/**
	 * Creates a new exception instance with the specified parameters.
	 * 
	 * @param importId the identifier of the import configuration
	 */
	public SnomedImportConfigurationNotFoundException(final UUID importId) {
		super("SNOMED CT import configuration", checkNotNull(importId, "importId").toString());
	}
}
