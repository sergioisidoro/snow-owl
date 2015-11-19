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
package com.b2international.snowowl.snomed.datastore.server.request;

import java.util.Map;

import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.events.RequestBuilder;
import com.b2international.snowowl.datastore.request.CommitInfo;

/**
 * @since 4.5
 */
public final class QueryRefSetUpdateRequestBuilder implements RequestBuilder<TransactionContext, Void> {

	private final String repositoryId;
	
	private String referenceSetId;
	private String moduleId;
	
	QueryRefSetUpdateRequestBuilder(String repositoryId) {
		this.repositoryId = repositoryId;
	}
	
	public QueryRefSetUpdateRequestBuilder setModuleId(String moduleId) {
		this.moduleId = moduleId;
		return this;
	}
	
	public QueryRefSetUpdateRequestBuilder setReferenceSetId(String refSetId) {
		this.referenceSetId = refSetId;
		return this;
	}

	@Override
	public Request<TransactionContext, Void> build() {
		return new QueryRefSetUpdateRequest(referenceSetId, moduleId);
	}
	
	public Request<ServiceProvider, CommitInfo> build(String userId, String branch, String commitComment) {
		return SnomedRequests.prepareCommit(userId, branch).setBody(build()).setCommitComment(commitComment).build();
	}

	public QueryRefSetUpdateRequestBuilder setSource(Map<String, Object> source) {
		setModuleId((String) source.get("moduleId"));
		setReferenceSetId((String) source.get("referenceSetId"));
		return this;
	}

}
