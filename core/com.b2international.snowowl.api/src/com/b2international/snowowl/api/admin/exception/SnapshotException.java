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
package com.b2international.snowowl.api.admin.exception;

/**
 * Thrown when an operation related to snapshots fails for a supporting index.
 */
public abstract class SnapshotException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new snapshot exception instance.
	 */
	protected SnapshotException() {
		super();
	}

	/**
	 * Creates a new snapshot exception instance with the specified message.
	 * 
	 * @param message the exception message
	 */
	protected SnapshotException(final String message) {
		super(message);
	}
}