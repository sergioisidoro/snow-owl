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
package com.b2international.snowowl.datastore.version;

import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;

import com.b2international.snowowl.datastore.index.diff.CompareResult;

/**
 * Service for exporting the result of a version compare operation.
 *
 */
public interface VersionCompareExporterService {

	/**
	 * Exports the given {@link CompareResult compare result} argument to the given output stream.
	 * @param result the result to export/serialize.
	 * @param os the stream the write on the result.
	 * @param monitor the monitor for the export process. Can be {@code null}. If {@code null} the operation 
	 * cannot be canceled.
	 */
	void export(final CompareResult result, final OutputStream os, final IProgressMonitor monitor);
	
}