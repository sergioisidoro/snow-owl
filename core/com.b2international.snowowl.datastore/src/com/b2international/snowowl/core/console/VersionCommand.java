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
package com.b2international.snowowl.core.console;

import org.eclipse.osgi.framework.console.CommandInterpreter;

import com.b2international.commons.extension.Component;
import com.b2international.snowowl.datastore.request.RepositoryRequests;

/**
 * @since 7.0
 */
@Component
public final class VersionCommand extends Command {

	@Override
	public void run(CommandInterpreter interpreter) {
		String version = RepositoryRequests.prepareGetServerInfo().buildAsync()
				.execute(getBus())
				.getSync()
				.version();
		interpreter.println(version);
	}

	@Override
	public String getCommand() {
		return "--version";
	}

	@Override
	public String getDescription() {
		return "returns the current version";
	}
	
}