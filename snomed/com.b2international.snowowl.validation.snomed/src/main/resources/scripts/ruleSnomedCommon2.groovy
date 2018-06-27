package scripts

import com.b2international.index.Hits
import com.b2international.index.aggregations.Aggregation
import com.b2international.index.aggregations.AggregationBuilder
import com.b2international.index.query.Expression
import com.b2international.index.query.Expressions
import com.b2international.index.query.Query
import com.b2international.index.revision.RevisionSearcher
import com.b2international.snowowl.core.ComponentIdentifier
import com.b2international.snowowl.core.validation.issue.IssueDetail
import com.b2international.snowowl.snomed.SnomedConstants.Concepts
import com.b2international.snowowl.snomed.common.SnomedRf2Headers
import com.b2international.snowowl.snomed.common.SnomedTerminologyComponentConstants
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.Sets

RevisionSearcher searcher = ctx.service(RevisionSearcher.class)

Iterable<Hits<String>> activeConceptBatches = searcher.scroll(Query.select(String.class)
		.from(SnomedConceptDocument.class)
		.fields(SnomedConceptDocument.Fields.ID)
		.where(SnomedConceptDocument.Expressions.active())
		.limit(10_000)
		.build())

Set<String> activeConceptIds = Sets.newHashSet()

activeConceptBatches.each({ conceptBatch ->
	activeConceptIds.addAll(conceptBatch.getHits())
})

Expression activeFsnExpression = Expressions.builder()
		.filter(SnomedDescriptionIndexEntry.Expressions.active())
		.filter(SnomedDescriptionIndexEntry.Expressions.type(Concepts.FULLY_SPECIFIED_NAME))
		.filter(SnomedDescriptionIndexEntry.Expressions.concepts(activeConceptIds))
		.build()

Aggregation<SnomedDescriptionIndexEntry> activeDescriptionsByOriginalTerm = searcher
		.aggregate(AggregationBuilder
		.bucket("ruleSnomedCommon2", SnomedDescriptionIndexEntry.class)
		.query(activeFsnExpression)
		.onFieldValue(SnomedDescriptionIndexEntry.Fields.ORIGINAL_TERM)
		.minBucketSize(2))

List<IssueDetail> issueDetails = Lists.newArrayList()

activeDescriptionsByOriginalTerm.getBuckets()
		.values()
		.each({ bucket ->
			bucket.each({ description ->
				issueDetails.add(new IssueDetail(
					ComponentIdentifier.of(SnomedTerminologyComponentConstants.DESCRIPTION_NUMBER, description.getId()),
					ImmutableMap.of(
						SnomedRf2Headers.FIELD_MODULE_ID, description.getModuleId(),
						SnomedRf2Headers.FIELD_ACTIVE, description.isActive())))
		})
})

return issueDetails
