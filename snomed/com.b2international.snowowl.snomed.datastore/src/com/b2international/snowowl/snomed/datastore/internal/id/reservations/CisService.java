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
package com.b2international.snowowl.snomed.datastore.internal.id.reservations;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.internal.id.IdGeneratorException;
import com.b2international.snowowl.snomed.datastore.internal.id.IhtsdoCredentials;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * Component Identifier Service (CIS) methods
 */
public class CisService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CisService.class);
	
	// the full URL
	protected String serviceUrl;
	
	protected ObjectMapper mapper = new ObjectMapper();
	protected HttpClient httpClient = new DefaultHttpClient();
	
	/**
	 * Construct a CIS service with a service URL
	 */
	public CisService(String externalIdGeneratorUrl, String externalIdGeneratorPort,
			String externalIdGeneratorContextRoot) {
		Preconditions.checkNotNull(externalIdGeneratorUrl, "External id generator URL is null.");
		serviceUrl = externalIdGeneratorUrl;

		if (externalIdGeneratorPort != null) {
			serviceUrl = serviceUrl + ":" + externalIdGeneratorPort;
		}
		
		if (externalIdGeneratorContextRoot != null) {
			serviceUrl = serviceUrl + "/" + externalIdGeneratorContextRoot;
		}
	}
	
	/**
	 * Logs in to the IHTSDO SNOMED CT id generation service 
	 * @return token representing the session
	 * @throws IOException
	 */
	protected String login() throws IOException {

		SnowOwlConfiguration snowOwlConfiguration = SnowOwlApplication.INSTANCE.getConfiguration();
		SnomedCoreConfiguration coreConfiguration = snowOwlConfiguration.getModuleConfig(SnomedCoreConfiguration.class);

		String userName = coreConfiguration.getExternalIdGeneratorUserName();
		String password = coreConfiguration.getExternalIdGeneratorPassword();

		HttpPost httpPost = new HttpPost(serviceUrl + "/" + "login");
		LOGGER.info("Logging in.  Executing request: {}", httpPost.getRequestLine());

		try {
			String credentialsString = getCredentialsString(userName, password);
			httpPost.setEntity(new StringEntity(credentialsString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);

			LOGGER.debug("Response: {}", response.getStatusLine());

			String jsonTokenString = EntityUtils.toString(response.getEntity());
			return jsonTokenString;
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpPost.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}

	}
	
	/**
	 * Logs out of the id generation session marked by the token. 
	 * @param jsonTokenString
	 */
	protected void logout(String jsonTokenString) {
		HttpPost httpPost = new HttpPost(serviceUrl + "/" + "logout");

		LOGGER.info("Logging out. Request: {}. Token: {}", httpPost.getRequestLine(), jsonTokenString);
		httpPost.setEntity(new StringEntity(jsonTokenString, ContentType.create("application/json")));
		HttpResponse response;
		try {
			response = httpClient.execute(httpPost);
			LOGGER.debug("Response: {}", response.getStatusLine());
		} catch (ClientProtocolException e) {
			throw new IdGeneratorException(e);
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpPost.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}

	}

	/**
	 * Returns the JSON representation of the credentials passed.
	 * @param userName
	 * @param password
	 * @return
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	protected String getCredentialsString(String userName, String password)
			throws JsonGenerationException, JsonMappingException, IOException {
		IhtsdoCredentials credentials = new IhtsdoCredentials(userName, password);
		return mapper.writeValueAsString(credentials);
	}


}
