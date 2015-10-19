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

import java.io.Serializable;

public class IdGeneratorException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new id generation runtime exception without message nor
	 * cause.
	 */
	public IdGeneratorException() {
		super();
	}

	/**
	 * Constructs a new id generation runtime exception with a message.
	 * 
	 * @param message
	 *            the detailed message of the exception.
	 */
	public IdGeneratorException(final String message) {
		super(message);
	}

	/**
	 * Constructs a new id generation runtime exception instance with a cause
	 * {@link Throwable exception}.
	 * 
	 * @param throwable
	 *            the wrapped cause of this Snow Owl runtime exception instance.
	 */
	public IdGeneratorException(final Throwable throwable) {
		super(throwable);
	}

	/**
	 * Constructs a new id generation runtime exception with a detailed message
	 * and the cause {@link Throwable exception}.
	 * 
	 * @param message
	 *            the detailed message of the exception.
	 * @param throwable
	 *            the cause of this exception.
	 */
	public IdGeneratorException(final String message, final Throwable throwable) {
		super(message, throwable);
	}

}
