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
package com.b2international.snowowl.core.branch;

import com.b2international.commons.options.Metadata;
import com.b2international.index.revision.RevisionBranch;
import com.b2international.index.revision.RevisionBranch.BranchState;
import com.b2international.snowowl.core.api.IBranchPath;
import com.google.common.base.Strings;

/**
 * @since 5.0
 */
public final class BranchData implements Branch {

	private static final long serialVersionUID = -3522105063636381152L;
	private final long branchId;
	private final boolean isDeleted;
	private final Metadata metadata;
	private final String name;
	private final String parentPath;
	private final long baseTimestamp;
	private final long headTimestamp;
	private final BranchState state;
	private final IBranchPath branchPath;
	
	private Branches children;
	
	public BranchData(RevisionBranch branch, BranchState state, IBranchPath branchPath) {
		this(branch.getId(), branch.getName(), branch.getParentPath(), branch.getBaseTimestamp(), branch.getHeadTimestamp(), branch.isDeleted(), branch.metadata(), state, branchPath);
	}
	
	private BranchData(long branchId, String name, String parentPath, long baseTimestamp, long headTimestamp, boolean isDeleted, Metadata metadata, BranchState state, IBranchPath branchPath) {
		this.branchId = branchId;
		this.name = name;
		this.parentPath = parentPath;
		this.baseTimestamp = baseTimestamp;
		this.headTimestamp = headTimestamp;
		this.state = state;
		this.isDeleted = isDeleted;
		this.metadata = metadata;
		this.branchPath = branchPath;
	}
	
	@Override
	public long branchId() {
		return branchId;
	}

	@Override
	public boolean isDeleted() {
		return isDeleted;
	}

	@Override
	public Metadata metadata() {
		return metadata;
	}

	@Override
	public String path() {
		return Strings.isNullOrEmpty(parentPath) ? name : parentPath + Branch.SEPARATOR + name;
	}

	@Override
	public String parentPath() {
		return parentPath;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public long baseTimestamp() {
		return baseTimestamp;
	}

	@Override
	public long headTimestamp() {
		return headTimestamp;
	}

	@Override
	public BranchState state() {
		return state;
	}

	@Override
	public IBranchPath branchPath() {
		return branchPath;
	}

	@Override
	public Branches getChildren() {
		return children;
	}
	
	public void setChildren(Branches children) {
		this.children = children;
	}

}
