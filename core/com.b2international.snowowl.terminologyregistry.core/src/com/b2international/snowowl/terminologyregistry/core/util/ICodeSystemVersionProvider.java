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
package com.b2international.snowowl.terminologyregistry.core.util;

import com.b2international.snowowl.core.api.IBranchPath;

/**
 * Component code system version should be determined based on the following
 * rules:
 * <ul>
 * 	<li>
 * 	When the component has been changed since its containing code system's last version,
 * 	then the provided version literal is ICodeSystemVersion.UNVERSIONED.</li>
 * 	<li>
 * 	When the component has been created since its containing code system's last version, 
 * 	then the provided version literal is ICodeSystemVersion.UNVERSIONED.</li>
 * 	<li>
 * 	When the component has not been changed since its containing code system's last version, 
 * 	then the provided version literal is the component's last version.</li>
 * </ul>
 * 
 * @since 3.1.0
 */
public interface ICodeSystemVersionProvider {

	/**
	 * Returns the component's container code system's last version. 
	 * Please note, that the last version depends on the currently active version of
	 * the component's container code system.
	 * 
	 * @param terminologyComponentId 
	 * @param codeSystemShortName the short name of the code system where the current component belongs to.
	 * @param componentId
	 * @param branchPathMap
	 * @return
	 */
	public String getVersion(String terminologyComponentId, final String codeSystemShortName, String componentId, IBranchPath branchPath);

}