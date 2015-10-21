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
package com.b2international.snowowl.snomed.datastore.index.update;

import com.b2international.snowowl.datastore.index.DocumentUpdaterBase;
import com.b2international.snowowl.snomed.Concept;
import com.b2international.snowowl.snomed.Description;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedDocumentBuilder;
import com.b2international.snowowl.snomed.datastore.index.mapping.SnomedMappings;
import com.b2international.snowowl.snomed.snomedrefset.SnomedLanguageRefSetMember;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * @since 4.3
 */
public class ConceptDescriptionUpdater extends DocumentUpdaterBase<SnomedDocumentBuilder> {

	private final Concept concept;

	public ConceptDescriptionUpdater(final Concept concept) {
		super(concept.getId());
		this.concept = concept;
	}

	@Override
	public void doUpdate(final SnomedDocumentBuilder doc) {
		doc.removeByPrefix(SnomedMappings.conceptTerm("").fieldName());

		for (final Description description : concept.getDescriptions()) {

			for (final SnomedLanguageRefSetMember languageRefSetMember : getActiveLanguageMembers(description)) {
				// concept_term_inactive_900000000000013009_900000000000509007_900000000000548007
				doc.conceptTerm(description.getTerm(), 
						getStatus(description.isActive()),
						description.getType().getId(), 
						languageRefSetMember.getRefSetIdentifierId(), 
						languageRefSetMember.getAcceptabilityId());
			}

			// concept_term_inactive_900000000000013009_en
			doc.conceptTerm(description.getTerm(), 
					getStatus(description.isActive()),
					description.getType().getId(),
					description.getLanguageCode());

			// concept_term_inactive_900000000000013009
			doc.conceptTerm(description.getTerm(), 
					getStatus(description.isActive()),
					description.getType().getId());
		}
	}

	private String getStatus(final boolean active) {
		return active ? "active" : "inactive";
	}

	private FluentIterable<SnomedLanguageRefSetMember> getActiveLanguageMembers(final Description description) {
		return FluentIterable.from(description.getLanguageRefSetMembers())
				.filter(new Predicate<SnomedLanguageRefSetMember>() {
					@Override public boolean apply(final SnomedLanguageRefSetMember member) { return member.isActive(); }
				});
	}
}
