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
package com.b2international.index.tx.query;

import com.b2international.index.query.Query.AfterSelectBuilder;
import com.b2international.index.query.Query.QueryBuilder;
import com.b2international.index.query.Query.SearchContextBuilder;
import com.b2international.index.tx.IndexBranch;

/**
 * @since 4.7
 */
public interface TransactionalQueryBuilder extends QueryBuilder, AfterSelectBuilder, SearchContextBuilder {

	/**
	 * Specifies the branch to restrict the query when executed.
	 * 
	 * @param branch - the branch to search on
	 * @return
	 */
	TransactionalQueryBuilder on(IndexBranch branch);
	
}