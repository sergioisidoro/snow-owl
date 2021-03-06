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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.b2international.commons.CompareUtils;
import com.b2international.commons.Pair;
import com.b2international.commons.extension.Component;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.datastore.session.IApplicationSessionManager;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * @since 7.0
 */
@Component
@picocli.CommandLine.Command(
	name = "users",
	header = "Displays and manages user sessions",
	description = "Displays information about user sessions and provides functionality to disconnect them from the server or prevent them from logging in, when required",
	subcommands = {
		HelpCommand.class,
		UsersCommand.LoginCommand.class
	}
)
public final class UsersCommand extends Command {

	@Override
	public void run(CommandLineStream out) {
		final List<Pair<String, String>> info = newArrayList(ApplicationContext.getInstance().getService(IApplicationSessionManager.class).getConnectedSessionInfo());
		
		if (CompareUtils.isEmpty(info)) {
			out.println("No users are connected to the server.");
		} else {
			Collections.sort(info, (o1, o2) -> Strings.nullToEmpty(o1.getA()).compareTo(Strings.nullToEmpty(o2.getA())));
			
			for (final Pair<String, String> pair : info) {
				out.println("User: %s | session ID: %s", pair.getA(), pair.getB());
			}
		}
	}

	@picocli.CommandLine.Command(
		name = "login",
		header = "Manages non-administrator logins.",
		description = "Enables/disables login for new, non-administrator sessions."
	)
	private static final class LoginCommand extends Command {

		private static final Set<String> ALLOWED_SUBCOMMANDS = ImmutableSet.of("enabled", "disabled", "status"); 
		
		@Parameters(paramLabel = "enabled|disabled|status", description = "Command to check ")
		String subCommand;
		
		@Override
		public void run(CommandLineStream out) {
			if (!ALLOWED_SUBCOMMANDS.contains(subCommand.toLowerCase())) {
				out.print("Command usage: session login [enabled|disabled|status]");
				return;
			}
			
			IApplicationSessionManager applicationSessionManager = ApplicationContext.getInstance().getService(IApplicationSessionManager.class);
			if (subCommand.equalsIgnoreCase("status")) {
				out.println("Non-administrative logins are currently %s.", (applicationSessionManager.isLoginEnabled() ? "enabled" : "disabled"));
				return;
			}

			final boolean loginEnabled = subCommand.equalsIgnoreCase("enabled");
			applicationSessionManager.enableLogins(loginEnabled);
			out.println("%s non-administrative logins.", (loginEnabled ? "Enabled" : "Disabled"));
		}
		
	}

}
