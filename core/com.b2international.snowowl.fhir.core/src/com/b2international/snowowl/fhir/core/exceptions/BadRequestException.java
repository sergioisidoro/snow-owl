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
package com.b2international.snowowl.fhir.core.exceptions;

import com.b2international.snowowl.fhir.core.codesystems.IssueType;
import com.b2international.snowowl.fhir.core.codesystems.OperationOutcomeCode;

/**
 * Thrown when a request contains incorrect parameters or is otherwise malformed.
 * 
 * @since 4.0
 */
public class BadRequestException extends FhirException {

	private static final long serialVersionUID = 1L;
	
	public BadRequestException(final String message, final Object...args) {
		super(message, args);
	}
	
	public BadRequestException(final String message, OperationOutcomeCode operationOutcomeCode, final Object...args) {
		super(message, operationOutcomeCode, args);
	}
	
	public BadRequestException(final String message, OperationOutcomeCode operationOutcomeCode, String location, final Object...args) {
		super(message, operationOutcomeCode, location, args);
	}
	
	public BadRequestException(final String message, String location, final Object...args) {
		super(message, location, args);
	}
	
	@Override
	public IssueType getIssueType() {
		return IssueType.INVALID;
	}
	
}