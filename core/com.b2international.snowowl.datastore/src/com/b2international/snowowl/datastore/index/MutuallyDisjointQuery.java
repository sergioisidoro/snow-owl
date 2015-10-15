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
package com.b2international.snowowl.datastore.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.Query;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

/**
 * A variant of {@link DisjunctionMaxQuery} that enforces mutual exclusivity by building a filter
 * chain of all filters found in disjuncts.
 * <p>
 * All added disjuncts will be converted to a {@link FilteredQuery} with a chained filter, combining
 * all previous disjunct filters with an AND_NOT operator, and including this disjunct's filter part
 * with an AND operator if the disjunct itself was a FilteredQuery originally.
 * 
 * @since 4.4
 */
public class MutuallyDisjointQuery extends DisjunctionMaxQuery {

	public MutuallyDisjointQuery(final float tieBreakerMultiplier) {
		super(tieBreakerMultiplier);
	}

	public MutuallyDisjointQuery(final List<Query> disjuncts, final float tieBreakerMultiplier) {
		super(disjuncts, tieBreakerMultiplier);
	}

	@Override
	public void add(final Collection<Query> disjuncts) {
		for (final Query disjunct : disjuncts) {
			add(disjunct);
		}
	}

	@Override
	public void add(final Query query) {
		final Iterable<Query> allDisjuncts = Iterables.concat(getDisjuncts(), Collections.singleton(query));
		final Filter[] filters = FluentIterable.from(allDisjuncts)
				.filter(FilteredQuery.class)
				.transform(new Function<FilteredQuery, Filter>() { @Override public Filter apply(final FilteredQuery input) {
					return input.getFilter();
				}})
				.toArray(Filter.class);

		if (filters.length < 1) {
			super.add(query);
		} else {
			final int[] ops = new int[filters.length];
			Arrays.fill(ops, 0, ops.length - 1, ChainedFilter.ANDNOT);
			ops[ops.length - 1] = ChainedFilter.AND;

			super.add(new FilteredQuery(extractQuery(query), new ChainedFilter(filters, ops)));
		}
	}

	private static Query extractQuery(final Query query) {
		return query instanceof FilteredQuery ? ((FilteredQuery) query).getQuery() : query;
	}
}
