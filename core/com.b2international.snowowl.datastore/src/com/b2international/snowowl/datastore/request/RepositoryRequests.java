/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.request;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.b2international.snowowl.datastore.request.repository.RepositoryGetRequestBuilder;
import com.b2international.snowowl.datastore.request.repository.RepositorySearchRequestBuilder;
import com.b2international.snowowl.datastore.request.system.ServerInfoGetRequestBuilder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * The central class of Snow Owl's terminology independent Java APIs.
 * @since 4.5
 */
@Api(
	tags= {"Admin"},
	produces="application/json"
)
@Path("/admin")
public final class RepositoryRequests {

	private RepositoryRequests() {}
	
	/**
	 * Returns the central class that provides access the server's branching features.
	 * @return central branching class with access to branching features
	 */
	public static Branching branching() {
		return new Branching();
	}
	
	/**
	 * Returns the central class that provides access the server's revision control
	 * merging features.
	 * @return central merging class with access to merging features
	 */
	public static Merging merging() {
		return new Merging();
	}
	
	/**
	 * Returns the central class that provides access the server's review features
	 * @return central review class with access to review features
	 */
	public static Reviews reviews() {
		return new Reviews();
	}
	
	public static CommitInfoRequests commitInfos() {
		return new CommitInfoRequests();
	}
	
	public static RepositoryBulkReadRequestBuilder prepareBulkRead() {
		return new RepositoryBulkReadRequestBuilder();
	}
	
	@ApiOperation(
		value="Returns all repositories",
		httpMethod="GET"
	)
	@GET
	@Path("/repositories")
	public static RepositorySearchRequestBuilder prepareSearch() {
		return new RepositorySearchRequestBuilder();
	}
	
	@ApiOperation(
		value="Returns a repository by ID",
		httpMethod="GET"
	)
	@GET
	@Path("/repositories/{id}")
	public static RepositoryGetRequestBuilder prepareGet(@ApiParam(name="id", required=true) String repositoryId) {
		return new RepositoryGetRequestBuilder(repositoryId);
	}

	@ApiOperation("Returns the current server information")
	@GET
	@Path("/info")
	public static ServerInfoGetRequestBuilder prepareGetServerInfo() {
		return new ServerInfoGetRequestBuilder();
	}
	
}
