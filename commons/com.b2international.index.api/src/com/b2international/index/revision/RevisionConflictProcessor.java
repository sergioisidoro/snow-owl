/*
 * Copyright 2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.revision;

import com.b2international.index.revision.StagingArea.RevisionPropertyDiff;

/**
 * @since 7.0
 */
public interface RevisionConflictProcessor {
	
	/**
	 * Checks if the specified {@link RevisionPropertyDiff} from the source change set conflicts with the corresponding {@code RevisionPropertyDiff} on the target.
	 * @param revisionId - the affected revision identifier
	 * @param sourceChange - the single-value change on the source
	 * @param targetChange - the single-value change on the target
	 * @return <ul>
	 * <li>{@code null} if a conflict should be reported;
	 * <li>a {@link RevisionPropertyDiff} containing the "winning" change otherwise.
	 * </ul>
	 */
	RevisionPropertyDiff handleChangedInSourceAndTarget(String revisionId, RevisionPropertyDiff sourceChange, RevisionPropertyDiff targetChange);
	
	/**
	 * Post-processes the resulting staging area before committing.
	 * 
	 * @param staging - the final state of the {@link StagingArea} before committing to the repository
	 */
	void postProcess(StagingArea staging);
	
	/**
	 * @since 7.0
	 */
	class Default implements RevisionConflictProcessor {
		
		@Override
		public RevisionPropertyDiff handleChangedInSourceAndTarget(String revisionId, RevisionPropertyDiff sourceChange, RevisionPropertyDiff targetChange) {
			return null;
		}
		
		@Override
		public void postProcess(StagingArea staging) {
		}
		
	}

}