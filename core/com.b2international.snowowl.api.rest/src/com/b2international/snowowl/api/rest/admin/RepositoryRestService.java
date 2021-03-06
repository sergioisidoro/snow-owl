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
package com.b2international.snowowl.api.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.b2international.commons.collections.Collections3;
import com.b2international.commons.exceptions.ApiError;
import com.b2international.snowowl.api.admin.IRepositoryService;
import com.b2international.snowowl.api.admin.exception.LockException;
import com.b2international.snowowl.api.rest.AbstractRestService;
import com.b2international.snowowl.api.rest.domain.RestApiError;
import com.b2international.snowowl.api.rest.util.DeferredResults;
import com.b2international.snowowl.core.Repositories;
import com.b2international.snowowl.core.RepositoryInfo;
import com.b2international.snowowl.datastore.request.RepositoryRequests;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Spring controller for exposing repository related administration functionalities.
 * @since 7.0
 */
@Api(value = "Repositories", description="Repositories", tags = { "repositories" })
@RestController
@RequestMapping(value = "/repositories") 
public class RepositoryRestService extends AbstractRestService {
	
	@Autowired
	protected IRepositoryService repositoryService;

	@ExceptionHandler(LockException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody RestApiError handleLockException(final LockException e) {
		return RestApiError.of(ApiError.Builder.of(e.getMessage()).build()).build(HttpStatus.BAD_REQUEST.value());
	}

	@ApiOperation(
			value="Retrieve all repositories",
			notes="Retrieves all repositories that store terminology content.")
	@GetMapping(produces = { AbstractRestService.JSON_MEDIA_TYPE })
	public @ResponseBody DeferredResult<Repositories> getRepositories(
			@ApiParam
			@RequestParam(value="id", required=false)
			String[] idFilter) {
		return DeferredResults.wrap(RepositoryRequests.prepareSearch()
				.all()
				.filterByIds(idFilter == null ? null : Collections3.toImmutableSet(idFilter))
				.buildAsync()
				.execute(bus));
	}
	
	@ApiOperation(
		value="Retrieve a repository",
		notes="Retrieves a single repository by its identifier"
	)
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK", response = Void.class),
		@ApiResponse(code = 404, message = "Not found", response = RestApiError.class)
	})
	@GetMapping(value = "/{id}", produces = { AbstractRestService.JSON_MEDIA_TYPE })
	public @ResponseBody DeferredResult<RepositoryInfo> getRepository(
			@ApiParam("The repository identifier")
			@PathVariable("id")
			String id) {
		return DeferredResults.wrap(RepositoryRequests.prepareGet(id).buildAsync().execute(bus));
	}

	@ApiOperation(
			value="Lock all repositories",
			notes="Places a global lock, which prevents other users from making changes to any of the repositories "
					+ "while a backup is created. The call may block up to the specified timeout to acquire the lock; "
					+ "if timeoutMillis is set to 0, it returns immediately.")
	@ApiResponses({
		@ApiResponse(code=204, message="Lock successful"),
		@ApiResponse(code=409, message="Conflicting lock already taken"),
		@ApiResponse(code=400, message="Illegal timeout value, or locking-related issue")
	})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/lock")
	public void lockGlobal(
			@RequestParam(value="timeoutMillis", defaultValue="5000", required=false) 
			@ApiParam(value="lock timeout in milliseconds")
			final int timeoutMillis) {

		repositoryService.lockGlobal(timeoutMillis);
	}

	@ApiOperation(
			value="Unlock all repositories",
			notes="Releases a previously acquired global lock.")
	@ApiResponses({
		@ApiResponse(code=204, message="Unlock successful"),
		@ApiResponse(code=400, message="Unspecified unlock-related issue")
	})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/unlock")
	public void unlockGlobal() {
		repositoryService.unlockGlobal();
	}

	@ApiOperation(
			value="Lock single repository",
			notes="Places a repository-level lock, which prevents other users from making changes to the specified repository. "
					+ "The call may block up to the specified timeout to acquire the lock; if timeoutMillis is set to 0, "
					+ "it returns immediately.")
	@ApiResponses({
		@ApiResponse(code=204, message="Lock successful"),
		@ApiResponse(code=409, message="Conflicting lock already taken"),
		@ApiResponse(code=404, message="Repository not found"),
		@ApiResponse(code=400, message="Illegal timeout value, or locking-related issue")
	})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/{id}/lock")
	public void lockRepository(
			@PathVariable(value="id") 
			@ApiParam(value="The repository id")
			final String id, 

			@RequestParam(value="timeoutMillis", defaultValue="5000", required=false)
			@ApiParam(value="lock timeout in milliseconds")
			final int timeoutMillis) {

		repositoryService.lockRepository(id, timeoutMillis);
	}

	@ApiOperation(
			value="Unlock single repository",
			notes="Releases a previously acquired repository-level lock on the specified repository.")
	@ApiResponses({
		@ApiResponse(code=204, message="Unlock successful"),
		@ApiResponse(code=404, message="Repository not found"),
		@ApiResponse(code=400, message="Unspecified unlock-related issue")
	})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/{id}/unlock")
	public void unlockRepository(
			@ApiParam(value="The repository id")
			@PathVariable(value="id") 
			final String repositoryUuid) {

		repositoryService.unlockRepository(repositoryUuid);
	}
}
