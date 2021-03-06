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
package com.b2international.snowowl.core.config;

import org.osgi.service.prefs.PreferencesService;

import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.api.IClientPreferencesCallback;
import com.b2international.snowowl.core.api.preferences.PreferenceBase;

/**
 * Configuration class for clients with information about the CDO server connection.
 * 
 */
public class ClientPreferences extends PreferenceBase implements IClientPreferencesCallback {
	
	private static final String EMPTY_STRING = "";

	private final static String NODE_NAME = "cdo.config.client";
	
	public static final String KEY_CDO_URL = "cdo.url";
	public static final String KEY_CDO_URL_HISTORY = "cdo.url.history";
	
	public static final String KEY_LAST_ACTIVE_TASK_ID = "last.active.task.id";
	public static final String DEFAULT_LAST_ACTIVE_TASK_ID = "com.b2international.snowowl.no.active.task.id";
	
	/**
	 * Creates a new {@link ClientPreferences} instance using the specified {@link PreferencesService}. Preference
	 * keys will be persisted in a subnode of the {@link PreferencesService#getSystemPreferences() root system node}.
	 * 
	 * @param preferencesService the preferences service to use (may not be {@code null}).
	 */
	public ClientPreferences(final PreferencesService preferencesService) {
		super(preferencesService, NODE_NAME);
	}

	/**
	 * @return a "|" character-separated list of recently used repository URLs.
	 */
	public String getCDOUrlHistory() {
		return preferences.get(KEY_CDO_URL_HISTORY, EMPTY_STRING);
	}
	
	public void setCDOUrlHistory(final String urlHistory) {
		put(KEY_CDO_URL_HISTORY, urlHistory);
	}
	
	/**
	 * @return the most recently used repository URL, or {@code null} if the client is in embedded mode, or the value
	 * was not set for some other reason.
	 */
	public String getCDOUrl() {
		return preferences.get(KEY_CDO_URL, null);
	}
	
	public void setCDOUrl(final String url) {
		put(KEY_CDO_URL, url);
	}
	
	/**
	 * @return {@code true} if the client is in embedded mode, {@code false} otherwise. Equivalent to checking whether
	 * {@link #getCDOUrl()} returns {@code null}.
	 */
	public boolean isClientEmbedded() {
		return getCDOUrl() == null;
	}
	
	/**
	 * @return the key of the task which was last activated in Snow Owl, or {@value #DEFAULT_LAST_ACTIVE_TASK_ID} if no
	 * task was recorded to be active, or the value was not set for some other reason.
	 */
	public String getLastActiveTaskId() {
		return preferences.get(KEY_LAST_ACTIVE_TASK_ID, DEFAULT_LAST_ACTIVE_TASK_ID);
	}
	
	public void setLastActiveTaskId(final String taskId) {
		preferences.put(KEY_LAST_ACTIVE_TASK_ID, taskId);
	}
	
	public void clearLastActiveTaskId() {
		setLastActiveTaskId(DEFAULT_LAST_ACTIVE_TASK_ID);
	}
	
	public boolean hasLastActiveTaskId() {
		final String lastActiveTaskId = getLastActiveTaskId(); 
		return !StringUtils.isEmpty(lastActiveTaskId) && !ClientPreferences.DEFAULT_LAST_ACTIVE_TASK_ID.equals(lastActiveTaskId);
	}

	@Override
	public void setUrlHistory(final String urlHistory) {
		setCDOUrlHistory(urlHistory);
	}

	@Override
	public void setUrl(final String url) {
		setCDOUrl(url);
	}
	
	protected void put(final String key, final String value) {
		if(value == null) {
			preferences.remove(key);
		} else {
			preferences.put(key, value);
		}
	}
}