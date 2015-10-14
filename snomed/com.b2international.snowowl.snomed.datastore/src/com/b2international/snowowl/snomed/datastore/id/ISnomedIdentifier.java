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
package com.b2international.snowowl.snomed.datastore.id;

import com.b2international.snowowl.core.terminology.ComponentCategory;

/**
 * Interface to representing a SNOMED CT identifier.
 * The SNOMED CT id is a multi part id with parts such as:
 * 
 * Extension - namespace - partition - check-digit
 * More on SNOMED CT ids see 
 * {@link http://ihtsdo.org/fileadmin/user_upload/doc/download/doc_TechnicalImplementationGuide_Current-en-US_INT_20140813.pdf?ok}
 * 
 * @since 4.0
 */
public interface ISnomedIdentifier {

	/**
	 * Returns the item id part of this id
	 * @return
	 */
	public long getItemId();

	/**
	 * Returns the namespace of this id
	 * @return
	 */
	public String getNamespace();

	/**
	 * Returns the partition id of this id
	 * @return
	 */
	public int getPartitionIdentifier();

	/**
	 * Returns the single digit checksum of this id
	 * @return
	 */
	public int getCheckDigit();
	
	/**
	 * Returns the internal component identifier of this id
	 * @return
	 */
	public int getComponentIdentifier();

	/**
	 * Returns the component category (e.g. concept, relationship, etc.)
	 * of the component represented by this id
	 * @return
	 */
	public ComponentCategory getComponentCategory();
	
}
