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
package com.b2international.index.lucene;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;

/**
 * @since 4.3 
 * @param <T> - the type of the field
 */
public class StoredOnlyDocValuesLongIndexField<T extends Number> extends IndexFieldDelegate<T> implements NumericDocValuesIndexField<T> {

	public StoredOnlyDocValuesLongIndexField(StoredIndexField<T> delegate) {
		super(delegate);
	}

	@Override
	public void addTo(Document doc, T value) {
		super.addTo(doc, value);
		doc.add(toDocValuesField(value));
	}
	
	@Override
	public NumericDocValuesField toDocValuesField(T value) {
		if (value instanceof Long) {
			return new NumericDocValuesField(fieldName(), (long) value);
		} else if (value instanceof Integer) {
			return new NumericDocValuesField(fieldName(), (int) value);
		} else {
			throw new IllegalArgumentException("Integer and Long types only");
		}
	}

	@Override
	public NumericDocValues getDocValues(LeafReader reader) throws IOException {
		return reader.getNumericDocValues(fieldName());
	}

}
