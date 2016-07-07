/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.taxonomy;

import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedDocument.Expressions.active;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry.Expressions.characteristicTypeId;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry.Expressions.destinationIds;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry.Expressions.sourceIds;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry.Expressions.typeId;

import java.io.IOException;

import com.b2international.collections.longs.LongCollection;
import com.b2international.collections.longs.LongSet;
import com.b2international.commons.Pair;
import com.b2international.commons.collect.LongSets;
import com.b2international.index.Hits;
import com.b2international.index.query.Expressions;
import com.b2international.index.query.Query;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.datastore.ICDOCommitChangeSet;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.datastore.IsAStatementWithId;
import com.b2international.snowowl.snomed.datastore.SnomedIsAStatementWithId;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRelationshipIndexEntry;

/**
 * @since 4.7
 */
public final class Taxonomies {

	private Taxonomies() {
	}
	
	public static Taxonomy inferred(RevisionSearcher searcher, ICDOCommitChangeSet commitChangeSet, LongCollection conceptIds) {
		return buildTaxonomy(searcher, commitChangeSet, conceptIds, CharacteristicType.INFERRED_RELATIONSHIP);
	}
	
	public static Taxonomy stated(RevisionSearcher searcher, ICDOCommitChangeSet commitChangeSet, LongCollection conceptIds) {
		return buildTaxonomy(searcher, commitChangeSet, conceptIds, CharacteristicType.STATED_RELATIONSHIP);
	}

	private static Taxonomy buildTaxonomy(RevisionSearcher searcher, ICDOCommitChangeSet commitChangeSet, LongCollection conceptIds, CharacteristicType characteristicType) {
		try {
			final String characteristicTypeId = characteristicType.getConceptId();
			final Query<SnomedRelationshipIndexEntry> query = Query.select(SnomedRelationshipIndexEntry.class)
					.where(Expressions.builder()
							.must(active())
							.must(typeId(Concepts.IS_A))
							.must(characteristicTypeId(characteristicTypeId))
							.must(sourceIds(LongSets.toStringSet(conceptIds)))
							.must(destinationIds(LongSets.toStringSet(conceptIds)))
							.build())
					.limit(Integer.MAX_VALUE)
					.build();
			final Hits<SnomedRelationshipIndexEntry> hits = searcher.search(query);
			
			final IsAStatementWithId[] statements = new SnomedIsAStatementWithId[hits.getTotal()];
			int i = 0;
			for (SnomedRelationshipIndexEntry hit : hits) {
				statements[i] = new SnomedIsAStatementWithId(Long.parseLong(hit.getSourceId()), Long.parseLong(hit.getDestinationId()), Long.parseLong(hit.getId()));
				i++;
			}
			
			final ISnomedTaxonomyBuilder oldTaxonomy = new SnomedTaxonomyBuilder(conceptIds, statements);
			final ISnomedTaxonomyBuilder newTaxonomy = new SnomedTaxonomyBuilder(conceptIds, statements);
			oldTaxonomy.build();
			new SnomedTaxonomyUpdateRunnable(searcher, commitChangeSet, newTaxonomy, characteristicTypeId).run();
			final Pair<LongSet, LongSet> diff = newTaxonomy.difference(oldTaxonomy);
			return new Taxonomy(newTaxonomy, oldTaxonomy, diff);
		} catch (IOException e) {
			throw new SnowowlRuntimeException(e);
		}
	}
	
}