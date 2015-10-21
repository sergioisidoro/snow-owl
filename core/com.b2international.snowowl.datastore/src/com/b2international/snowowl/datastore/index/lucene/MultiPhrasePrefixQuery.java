/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.b2international.snowowl.datastore.index.lucene;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PrefixTermsEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ToStringUtils;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;

/**
 * A variant of {@link MultiPhraseQuery} that matches prefixes in the specified relative positions.
 * <p>
 * Taken from Elasticsearch, with slight cleanup and modifications.
 * 
 * @since 4.4
 */
public class MultiPhrasePrefixQuery extends Query {

	private String field;
	private final List<Term[]> termArrays = newArrayList();
	private final List<Integer> positions = newArrayList();
	private int maxExpansions = 350;

	private int slop = 0;

	/**
	 * Sets the phrase slop for this query.
	 *
	 * @see org.apache.lucene.search.PhraseQuery#setSlop(int)
	 */
	public void setSlop(final int slop) {
		this.slop = slop;
	}

	/**
	 * Retrieves the phrase slop for this query.
	 *
	 * @see org.apache.lucene.search.PhraseQuery#getSlop()
	 */
	public int getSlop() {
		return slop;
	}

	public void setMaxExpansions(final int maxExpansions) {
		this.maxExpansions = maxExpansions;
	}

	/**
	 * Add a single term at the next position in the phrase.
	 *
	 * @see org.apache.lucene.search.PhraseQuery#add(Term)
	 */
	public void add(final Term term) {
		add(new Term[] { term });
	}

	/**
	 * Add multiple prefixes at the next position in the phrase. Any term matching 
	 * these prefixes will result in a match.
	 *
	 * @see org.apache.lucene.search.PhraseQuery#add(Term)
	 */
	public void add(final Term[] terms) {
		final int position;

		if (positions.isEmpty()) {
			position = 0;
		} else {
			position = positions.get(positions.size() - 1) + 1;
		}

		add(terms, position);
	}

	/**
	 * Allows to specify the relative position of prefixes within the phrase.
	 *
	 * @param terms
	 * @param position
	 * @see org.apache.lucene.search.PhraseQuery#add(Term, int)
	 */
	public void add(final Term[] terms, final int position) {
		if (termArrays.size() == 0) {
			field = terms[0].field();
		}

		for (int i = 0; i < terms.length; i++) {
			if (terms[i].field() != field) {
				throw new IllegalArgumentException(String.format("All phrase terms must be in the same field (%s): %s", field, terms[i]));
			}
		}

		termArrays.add(terms);
		positions.add(position);
	}

	/**
	 * Returns the list of prefixes in this multi-phrase prefix query.
	 * 
	 * Do not modify the arrays in the list.
	 */
	public List<Term[]> getTermArrays() {
		return Collections.unmodifiableList(termArrays);
	}

	/**
	 * Returns the relative positions of terms in this multi-phrase prefix query.
	 */
	public int[] getPositions() {
		return Ints.toArray(positions);
	}

	@Override
	public Query rewrite(final IndexReader reader) throws IOException {
		if (termArrays.isEmpty()) {
			return new BooleanQuery();
		}

		final int size = termArrays.size();
		final Set<Term> terms = newHashSet();

		final MultiPhraseQuery query = new MultiPhraseQuery();
		query.setSlop(slop);

		for (int i = 0; i < size; i++) {
			final Term[] termsToExpand = termArrays.get(i);
			final int position = positions.get(i);

			for (final Term term : termsToExpand) {
				getPrefixTerms(terms, term, reader);
				if (terms.size() >= maxExpansions) {
					break;
				}
			}

			if (terms.isEmpty()) {
				return new BooleanQuery();
			}

			query.add(Iterables.toArray(terms, Term.class), position);
			terms.clear();
		}

		return query.rewrite(reader);
	}

	private void getPrefixTerms(final Set<Term> terms, final Term prefix, final IndexReader reader) throws IOException {
		TermsEnum termsEnum = null;

		for (final AtomicReaderContext leaf : reader.leaves()) {

			final Terms _terms = leaf.reader().terms(field);
			if (_terms == null) {
				continue;
			}

			termsEnum = _terms.iterator(termsEnum);

			if (prefix.bytes().length > 0) {
				termsEnum = new PrefixTermsEnum(termsEnum, prefix.bytes());
			}

			for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next()) {
				terms.add(new Term(field, BytesRef.deepCopyOf(term)));
				if (terms.size() >= maxExpansions) {
					return;
				}
			}
		}
	}

	@Override
	public final String toString(final String f) {
		final StringBuilder buffer = new StringBuilder();

		if (field == null || !field.equals(f)) {
			buffer.append(field);
			buffer.append(":");
		}

		buffer.append("\"");

		final Iterator<Term[]> i = termArrays.iterator();
		while (i.hasNext()) {
			final Term[] terms = i.next();

			if (terms.length > 1) {
				buffer.append("(");

				for (int j = 0; j < terms.length; j++) {
					buffer.append(terms[j].text());
					if (j < terms.length - 1) {
						buffer.append("* ");
					}
				}

				buffer.append("*)");

				if (i.hasNext()) {
					buffer.append(" ");
				}

			} else {
				buffer.append(terms[0].text());
				buffer.append("*");

				if (i.hasNext()) {
					buffer.append(" ");
				}
			}
		}
		buffer.append("\"");

		if (slop != 0) {
			buffer.append("~");
			buffer.append(slop);
		}

		buffer.append(ToStringUtils.boost(getBoost()));
		return buffer.toString();
	}

	/**
	 * Returns true if <code>o</code> is equal to this.
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof MultiPhrasePrefixQuery)) {
			return false;
		}

		final MultiPhrasePrefixQuery other = (MultiPhrasePrefixQuery) o;

		return this.getBoost() == other.getBoost()
				&& this.slop == other.slop
				&& termArraysEquals(this.termArrays, other.termArrays)
				&& this.positions.equals(other.positions);
	}

	/**
	 * Returns a hash code value for this object.
	 */
	@Override
	public int hashCode() {
		return Float.floatToIntBits(getBoost())
				^ slop
				^ termArraysHashCode()
				^ positions.hashCode()
				^ 0x4AC65113;
	}

	// Breakout calculation of the termArrays hashcode
	private int termArraysHashCode() {
		int hashCode = 1;
		for (final Term[] termArray : termArrays) {
			hashCode = 31 * hashCode + (termArray == null ? 0 : Arrays.hashCode(termArray));
		}

		return hashCode;
	}

	// Breakout calculation of the termArrays equals
	private boolean termArraysEquals(final List<Term[]> termArrays1, final List<Term[]> termArrays2) {
		if (termArrays1.size() != termArrays2.size()) {
			return false;
		}

		final ListIterator<Term[]> iterator1 = termArrays1.listIterator();
		final ListIterator<Term[]> iterator2 = termArrays2.listIterator();

		while (iterator1.hasNext()) {
			final Term[] termArray1 = iterator1.next();
			final Term[] termArray2 = iterator2.next();
			if (!(termArray1 == null ? termArray2 == null : Arrays.equals(termArray1, termArray2))) {
				return false;
			}
		}

		return true;
	}

	public String getField() {
		return field;
	}
}
