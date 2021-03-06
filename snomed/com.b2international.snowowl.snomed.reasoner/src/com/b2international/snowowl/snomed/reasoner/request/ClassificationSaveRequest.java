/*
 * Copyright 2017-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.reasoner.request;

import java.util.Set;

import org.hibernate.validator.constraints.NotEmpty;

import com.b2international.commons.exceptions.BadRequestException;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.core.events.AsyncRequest;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.datastore.request.IndexReadRequest;
import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.b2international.snowowl.datastore.request.job.JobRequests;
import com.b2international.snowowl.snomed.reasoner.domain.ClassificationStatus;
import com.b2international.snowowl.snomed.reasoner.domain.ClassificationTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

/**
 * @since 5.7
 */
final class ClassificationSaveRequest implements Request<RepositoryContext, String> {

	static final Set<ClassificationStatus> SAVEABLE_STATUSES = ImmutableSet.of(
			ClassificationStatus.COMPLETED, 
			ClassificationStatus.SAVE_FAILED);

	@JsonProperty
	@NotEmpty
	private final String classificationId;

	@JsonProperty
	@NotEmpty
	private final String userId;

	ClassificationSaveRequest(final String classificationId, final String userId) {
		this.classificationId = classificationId;
		this.userId = userId;
	}

	@Override
	public String execute(final RepositoryContext context) {
		final Request<RepositoryContext, ClassificationTask> classificationRequest = ClassificationRequests
				.prepareGetClassification(classificationId)
				.build();
		
		final ClassificationTask classification = new IndexReadRequest<>(classificationRequest)
				.execute(context);

		final String branchPath = classification.getBranch();
		
		final Request<RepositoryContext, Branch> branchRequest = RepositoryRequests.branching()
				.prepareGet(branchPath)
				.build();
		
		final Branch branch = new IndexReadRequest<>(branchRequest)
				.execute(context);

		if (!SAVEABLE_STATUSES.contains(classification.getStatus())) {
			throw new BadRequestException("Classification '%s' is not in the expected state to start saving changes.", classificationId);
		}

		if (classification.getTimestamp() < branch.headTimestamp()) {
			throw new BadRequestException("Classification '%s' is stale (recorded timestamp: %s, current timestamp of branch '%s': %s).", 
					classificationId, 
					classification.getTimestamp(),
					branchPath,
					branch.headTimestamp());
		}

		final AsyncRequest<?> saveRequest = new SaveJobRequestBuilder()
				.setClassificationId(classificationId)
				.setUserId(userId)
				.build(context.id(), branchPath);

		return JobRequests.prepareSchedule()
				.setUser(userId)
				.setRequest(saveRequest)
				.setDescription(String.format("Saving classification changes on %s", branch))
				.build()
				.execute(context);
	}
}
