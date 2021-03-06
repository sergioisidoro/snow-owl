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
package com.b2international.snowowl.snomed.validation.detail;

import static com.b2international.snowowl.core.terminology.ComponentCategory.CONCEPT;
import static com.b2international.snowowl.core.terminology.ComponentCategory.DESCRIPTION;
import static com.b2international.snowowl.core.terminology.ComponentCategory.RELATIONSHIP;
import static com.b2international.snowowl.core.terminology.ComponentCategory.SET_MEMBER;
import static com.b2international.snowowl.snomed.validation.detail.SnomedValidationIssueDetailExtension.SnomedIssueDetailFilterFields.COMPONENT_EFFECTIVE_TIME_END;
import static com.b2international.snowowl.snomed.validation.detail.SnomedValidationIssueDetailExtension.SnomedIssueDetailFilterFields.COMPONENT_EFFECTIVE_TIME_START;
import static com.b2international.snowowl.snomed.validation.detail.SnomedValidationIssueDetailExtension.SnomedIssueDetailFilterFields.COMPONENT_MODULE_ID;
import static com.b2international.snowowl.snomed.validation.detail.SnomedValidationIssueDetailExtension.SnomedIssueDetailFilterFields.COMPONENT_STATUS;
import static com.b2international.snowowl.snomed.validation.detail.SnomedValidationIssueDetailExtension.SnomedIssueDetailFilterFields.CONCEPT_STATUS;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.b2international.commons.options.Options;
import com.b2international.index.Hits;
import com.b2international.index.query.Expression;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.index.query.Query;
import com.b2international.index.query.Query.QueryBuilder;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.core.validation.issue.ValidationIssue;
import com.b2international.snowowl.core.validation.issue.ValidationIssueDetailExtension;
import com.b2international.snowowl.snomed.common.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDocument;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * @since 6.4
 */
public class SnomedValidationIssueDetailExtension implements ValidationIssueDetailExtension {

	public final static class SnomedIssueDetailFilterFields {

		private SnomedIssueDetailFilterFields() {};
		
		public static final String COMPONENT_STATUS = "componentStatus";
		public static final String COMPONENT_MODULE_ID = "componentModuleId";
		public static final String COMPONENT_EFFECTIVE_TIME_START = "effectiveTimeStart";
		public static final String COMPONENT_EFFECTIVE_TIME_END = "effectiveTimeEnd";
		public static final String CONCEPT_STATUS = "conceptStatus";
		
	}
	
	private static final int SCROLL_SIZE = 50_000;
	
	@Override
	public void prepareQuery(ExpressionBuilder queryBuilder, Options options) {
		if (options.containsKey(COMPONENT_STATUS)) {
			final Boolean isActive = options.get(COMPONENT_STATUS, Boolean.class);
			queryBuilder.filter(Expressions.match(COMPONENT_STATUS, isActive));
		}

		if (options.containsKey(COMPONENT_MODULE_ID)) {
			final Collection<String> moduleIds = options.getCollection(COMPONENT_MODULE_ID, String.class);
			queryBuilder.filter(Expressions.matchAny(COMPONENT_MODULE_ID, moduleIds));
		}
		
		if (options.containsKey(CONCEPT_STATUS)) {
			final Boolean isConceptActive = options.get(CONCEPT_STATUS, Boolean.class);
			queryBuilder.filter(Expressions.match(CONCEPT_STATUS, isConceptActive));
		}
		
		if (options.containsKey(COMPONENT_EFFECTIVE_TIME_START) || options.containsKey(COMPONENT_EFFECTIVE_TIME_END)) {
			final Long start = options.get(COMPONENT_EFFECTIVE_TIME_START, Long.class);
			final Long end = options.get(COMPONENT_EFFECTIVE_TIME_END, Long.class);

			final Expression effectiveTimeExpression = Expressions.matchRange(SnomedDocument.Fields.EFFECTIVE_TIME,
				start == null ? 0L : start,
				end == null ? Long.MAX_VALUE : end
			);
			
			queryBuilder.filter(effectiveTimeExpression);
		}

	}
	
	@Override
	public void extendIssues(BranchContext context, Collection<ValidationIssue> issues) {
		extendIssueDetails(context, issues); // XXX adds labels for description issues
		extendConceptIssueLabels(context, issues);
		extendRelationshipIssueLabels(context, issues);
	}

	private void extendIssueDetails(BranchContext context, Collection<ValidationIssue> issues) {
		final RevisionSearcher searcher = context.service(RevisionSearcher.class);
		
		final Multimap<String, ValidationIssue> issuesByComponentId = Multimaps.index(issues, issue -> issue.getAffectedComponent().getComponentId());

		final Multimap<ComponentCategory, String> issueComponentIdsByComponentCategory = HashMultimap.create();
		issues.stream().forEach(issue -> {
			final ComponentCategory componentCategory = getComponentCategory(issue.getAffectedComponent().getTerminologyComponentId());
			issueComponentIdsByComponentCategory.put(componentCategory, issue.getAffectedComponent().getComponentId());
		});
		
		final Multimap<String, String> issueIdsByConceptIds = HashMultimap.create();
		final Set<String> alreadyFetchedConceptIds  = Sets.newHashSet();
		for (ComponentCategory category : issueComponentIdsByComponentCategory.keySet()) {
			final Query<String[]> query = buildQuery(category, issueComponentIdsByComponentCategory.get(category));
			
			for (Hits<String[]> hits : searcher.scroll(query)) {
				for (String[] hit : hits) {
					String id = hit[0];
					String status = hit[1];
					String moduleId = hit[2];
					issuesByComponentId.get(id).forEach(validationIssue -> {
						validationIssue.setDetails(COMPONENT_STATUS, status);
						validationIssue.setDetails(COMPONENT_MODULE_ID, moduleId);
						if (CONCEPT == category) {
							validationIssue.setDetails(CONCEPT_STATUS, status);
							validationIssue.setDetails(SnomedDocument.Fields.EFFECTIVE_TIME, Long.parseLong(hit[3]));
							alreadyFetchedConceptIds.add(id);
						} else if (DESCRIPTION == category || RELATIONSHIP == category) {
							validationIssue.setDetails(SnomedDocument.Fields.EFFECTIVE_TIME, Long.parseLong(hit[3]));
							final String containerConceptId = hit[4];
							if (!Strings.isNullOrEmpty(containerConceptId) && (!issueIdsByConceptIds.containsKey(containerConceptId) || !alreadyFetchedConceptIds.contains(containerConceptId))) {
								issueIdsByConceptIds.put(containerConceptId, id);
							}
							// in case of description just add the already fetched term as label to the issue, concepts and relationship will get their 
							if (DESCRIPTION == category) {
								validationIssue.setAffectedComponentLabels(Collections.singletonList(hit[5]));
							}
						}
					});
				}
			}
		}
		
		if (!issueIdsByConceptIds.isEmpty()) {
			final Query<String[]> conceptStatusQuery = Query.select(String[].class)
					.from(SnomedConceptDocument.class)
					.fields(SnomedConceptDocument.Fields.ID, SnomedConceptDocument.Fields.ACTIVE)
					.where(SnomedConceptDocument.Expressions.ids(issueIdsByConceptIds.keySet()))
					.limit(SCROLL_SIZE)
					.build();
			
			for (Hits<String[]> hits : searcher.scroll(conceptStatusQuery)) {
				for (String[] hit : hits) {
					Collection<String> issueIds = issueIdsByConceptIds.get(hit[0]);
					issueIds.stream().forEach(id -> {
						issuesByComponentId.get(id).forEach(validationIssue -> validationIssue.setDetails(CONCEPT_STATUS, hit[1]));
					});
				}
			}
		}
	}
	
	private void extendConceptIssueLabels(BranchContext context, Collection<ValidationIssue> issues) {
		final RevisionSearcher searcher = context.service(RevisionSearcher.class);
		
		final List<ValidationIssue> conceptIssues = issues.stream()
				.filter(issue -> SnomedTerminologyComponentConstants.CONCEPT_NUMBER == issue.getAffectedComponent().getTerminologyComponentId())
				.collect(Collectors.toList());
		
		if (conceptIssues.isEmpty()) {
			return;
		}
		
		final Multimap<String, ValidationIssue> issuesByConceptId = Multimaps.index(conceptIssues, issue -> issue.getAffectedComponent().getComponentId());

		final Set<String> synonymIds = SnomedRequests.prepareGetSynonyms().build().execute(context).stream().map(SnomedConcept::getId).collect(Collectors.toSet());
		
		final Multimap<String, String> affectedComponentLabelsByConcept = HashMultimap.create();

		searcher.scroll(Query.select(String[].class)
			.from(SnomedDescriptionIndexEntry.class)
			.fields(SnomedDescriptionIndexEntry.Fields.CONCEPT_ID, SnomedDescriptionIndexEntry.Fields.TERM)
			.where(Expressions.builder()
				.filter(SnomedDescriptionIndexEntry.Expressions.active())
				.filter(SnomedDescriptionIndexEntry.Expressions.concepts(issuesByConceptId.keySet()))
				.filter(SnomedDescriptionIndexEntry.Expressions.types(ImmutableSet.<String>builder()
						.add(Concepts.FULLY_SPECIFIED_NAME)
						.addAll(synonymIds)
						.build()))
				.build()
			)
			.limit(SCROLL_SIZE)
			.build())
			.forEach(hits -> {
				for (String[] hit : hits) {
					affectedComponentLabelsByConcept.put(hit[0], hit[1]);
				}
			});

		if (!affectedComponentLabelsByConcept.isEmpty()) {
			issuesByConceptId.values().forEach(issue -> {
				final Collection<String> labels = affectedComponentLabelsByConcept.get(issue.getAffectedComponent().getComponentId());
				issue.setAffectedComponentLabels(ImmutableList.copyOf(labels));
			});
		}
	}
	
	private void extendRelationshipIssueLabels(BranchContext context, Collection<ValidationIssue> issues) {
		final RevisionSearcher searcher = context.service(RevisionSearcher.class);
		
		final List<ValidationIssue> relationshipIssues = issues.stream()
				.filter(issue -> SnomedTerminologyComponentConstants.RELATIONSHIP_NUMBER == issue.getAffectedComponent().getTerminologyComponentId())
				.collect(Collectors.toList());
		
		if (relationshipIssues.isEmpty()) {
			return;
		}
		
		final Multimap<String, ValidationIssue> issuesByRelationshipId = Multimaps.index(relationshipIssues, issue -> issue.getAffectedComponent().getComponentId());

		final Set<String> synonymIds = SnomedRequests.prepareGetSynonyms().build().execute(context).stream().map(SnomedConcept::getId).collect(Collectors.toSet());
		
		final Set<String> conceptsToFetch = newHashSet();

		searcher.scroll(Query.select(String[].class)
			.from(SnomedRelationshipIndexEntry.class)
			.fields(SnomedRelationshipIndexEntry.Fields.SOURCE_ID, SnomedRelationshipIndexEntry.Fields.TYPE_ID, SnomedRelationshipIndexEntry.Fields.DESTINATION_ID)
			.where(SnomedRelationshipIndexEntry.Expressions.ids(issuesByRelationshipId.keySet()))
			.limit(SCROLL_SIZE)
			.build())
			.forEach(hits -> {
				for (String[] hit : hits) {
					conceptsToFetch.add(hit[0]);
					conceptsToFetch.add(hit[1]);
					conceptsToFetch.add(hit[2]);
				}
			});
		
		final Multimap<String, String> affectedComponentLabelsByConcept = HashMultimap.create();
		
		searcher.scroll(Query.select(String[].class)
				.from(SnomedDescriptionIndexEntry.class)
				.fields(SnomedDescriptionIndexEntry.Fields.CONCEPT_ID, SnomedDescriptionIndexEntry.Fields.TERM)
				.where(Expressions.builder()
					.filter(SnomedDescriptionIndexEntry.Expressions.active())
					.filter(SnomedDescriptionIndexEntry.Expressions.concepts(conceptsToFetch))
					.filter(SnomedDescriptionIndexEntry.Expressions.types(ImmutableSet.<String>builder()
							.add(Concepts.FULLY_SPECIFIED_NAME)
							.addAll(synonymIds)
							.build()))
					.build()
				)
				.limit(SCROLL_SIZE)
				.build())
				.forEach(hits -> {
					for (String[] hit : hits) {
						affectedComponentLabelsByConcept.put(hit[0], hit[1]);
					}
				});

		if (!affectedComponentLabelsByConcept.isEmpty()) {
			issuesByRelationshipId.values().forEach(issue -> {
				final Collection<String> labels = affectedComponentLabelsByConcept.get(issue.getAffectedComponent().getComponentId());
				issue.setAffectedComponentLabels(ImmutableList.copyOf(labels));
			});
		}
	}
	
	private Query<String[]> buildQuery(ComponentCategory category, Collection<String> issueIds) {
		final QueryBuilder<String[]> queryBuilder = Query.select(String[].class);
		switch (category) {
		case CONCEPT:
			queryBuilder.from(SnomedConceptDocument.class).fields(
					SnomedConceptDocument.Fields.ID,
					SnomedConceptDocument.Fields.ACTIVE,
					SnomedConceptDocument.Fields.MODULE_ID,
					SnomedDocument.Fields.EFFECTIVE_TIME
			);
			break;
		case DESCRIPTION:
			queryBuilder.from(SnomedDescriptionIndexEntry.class).fields(
					SnomedDescriptionIndexEntry.Fields.ID,
					SnomedDescriptionIndexEntry.Fields.ACTIVE,
					SnomedDescriptionIndexEntry.Fields.MODULE_ID,
					SnomedDocument.Fields.EFFECTIVE_TIME,
					SnomedDescriptionIndexEntry.Fields.CONCEPT_ID,
					SnomedDescriptionIndexEntry.Fields.TERM
			);
			break;
		case RELATIONSHIP:
			queryBuilder.from(SnomedRelationshipIndexEntry.class).fields(
					SnomedRelationshipIndexEntry.Fields.ID,
					SnomedRelationshipIndexEntry.Fields.ACTIVE,
					SnomedRelationshipIndexEntry.Fields.MODULE_ID,
					SnomedDocument.Fields.EFFECTIVE_TIME,
					SnomedRelationshipIndexEntry.Fields.SOURCE_ID
			);
			break;
		default:
			break;
		}
		return queryBuilder.where(SnomedDocument.Expressions.ids(issueIds)).limit(SCROLL_SIZE).build();
	}
	
	private ComponentCategory getComponentCategory(short terminologyComponentId) {
		if (SnomedTerminologyComponentConstants.CONCEPT_NUMBER == terminologyComponentId) {
			return CONCEPT;
		} else if (SnomedTerminologyComponentConstants.DESCRIPTION_NUMBER == terminologyComponentId) {
			return DESCRIPTION;
		} else if (SnomedTerminologyComponentConstants.RELATIONSHIP_NUMBER == terminologyComponentId) {
			return RELATIONSHIP;
		} else if (SnomedTerminologyComponentConstants.REFSET_MEMBER_NUMBER == terminologyComponentId) {
			return SET_MEMBER;
		} else {
			throw new UnsupportedOperationException("Unsupported terminology component id: " + terminologyComponentId);
		}
	}

	@Override
	public String getToolingId() {
		return SnomedTerminologyComponentConstants.TERMINOLOGY_ID;
	}
}