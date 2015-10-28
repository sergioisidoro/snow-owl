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
package com.b2international.snowowl.snomed.datastore;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration.IdGenerationSource;
import com.b2international.snowowl.snomed.datastore.internal.id.IdGeneratorException;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.CisCredentials;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.Token;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OSGI command contribution with command to test IHTSDO's external Id
 * generation service.
 *
 */
public class SnomedDatastoreCommandProvider implements CommandProvider {

	private HttpClient httpClient = new DefaultHttpClient();
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Override
	public String getHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("---SNOMED service commands---\n");
		buffer.append(
				"\tsnomed external_id_service_test - Checks the status of CIS, IHTSDO's external id generation service\n");
		return buffer.toString();
	}

	/**
	 * Snomed CT service related commands.
	 * Reflective template method declaratively registered. Needs to start with
	 * "_".
	 * 
	 * @param interpreter
	 */
	public void _snomed(CommandInterpreter interpreter) {
		try {
			String cmd = interpreter.nextArgument();

			if ("external_id_service_test".equals(cmd)) {
				checkIdGenerationService(interpreter);
				return;
			}
			interpreter.println(getHelp());
		} catch (Exception ex) {
			interpreter.println(ex.getMessage());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	public synchronized void checkIdGenerationService(CommandInterpreter ci) throws JsonParseException, JsonMappingException, IOException {
		SnowOwlConfiguration snowOwlConfiguration = SnowOwlApplication.INSTANCE.getConfiguration();
		SnomedCoreConfiguration coreConfiguration = snowOwlConfiguration.getModuleConfig(SnomedCoreConfiguration.class);
		
		IdGenerationSource idGenerationSource = coreConfiguration.getIdGenerationSource();
		if (idGenerationSource == null) {
			ci.println("Id generation source is not set in the configuration file.");
			return;
		}
		if (idGenerationSource == IdGenerationSource.INTERNAL) {
			ci.println("Status: OK.\nId generation source is set to INTERNAL.");
			return;
		}
		
		String externalIdGeneratorUrl = coreConfiguration.getExternalIdGeneratorUrl();
		String externalIdGeneratorPort = coreConfiguration.getExternalIdGeneratorPort();
		String externalIdGeneratorContextRoot = coreConfiguration.getExternalIdGeneratorContextRoot();
		
		String serviceUrl = externalIdGeneratorUrl;

		if (externalIdGeneratorPort != null) {
			serviceUrl = serviceUrl + ":" + externalIdGeneratorPort;
		}
		
		if (externalIdGeneratorContextRoot != null) {
			serviceUrl = serviceUrl + "/" + externalIdGeneratorContextRoot;
		}
		ci.println("Checking IHTSDO's external id generation service, by logging in and logging out.");

		String jsonTokenString = login(serviceUrl, ci);
		stats(serviceUrl, jsonTokenString, ci);
		logout(serviceUrl, jsonTokenString, ci);
	}

	/**
	 * Logs in to the IHTSDO SNOMED CT id generation service 
	 * @return token representing the session
	 * @throws IOException
	 */
	protected String login(String serviceUrl, CommandInterpreter ci) {

		SnowOwlConfiguration snowOwlConfiguration = SnowOwlApplication.INSTANCE.getConfiguration();
		SnomedCoreConfiguration coreConfiguration = snowOwlConfiguration.getModuleConfig(SnomedCoreConfiguration.class);
		
		String userName = coreConfiguration.getExternalIdGeneratorUserName();
		String password = coreConfiguration.getExternalIdGeneratorPassword();

		HttpPost httpPost = new HttpPost(serviceUrl + "/" + "login");
		ci.println("Logging in.  Executing request: " +  httpPost.getRequestLine());

		try {
			String credentialsString = getCredentialsString(userName, password);
			httpPost.setEntity(new StringEntity(credentialsString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);

			ci.println("Response: " + response.getStatusLine());

			String jsonTokenString = EntityUtils.toString(response.getEntity());
			return jsonTokenString;
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			httpPost.releaseConnection();
		}

	}
	
	/**
	 * Retrieves the stats for the user
	 * @param serviceUrl
	 * @param jsonTokenString
	 * @param ci
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	protected void stats(String serviceUrl, String jsonTokenString, CommandInterpreter ci) throws JsonParseException, JsonMappingException, IOException {
		
		SnowOwlConfiguration snowOwlConfiguration = SnowOwlApplication.INSTANCE.getConfiguration();
		SnomedCoreConfiguration coreConfiguration = snowOwlConfiguration.getModuleConfig(SnomedCoreConfiguration.class);
		
		String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
		HttpGet httpGet = new HttpGet(serviceUrl + "/" + "stats?token=" + tokenString + "&username=" + coreConfiguration.getExternalIdGeneratorUserName());
		ci.println("User stats.  Executing request: " +  httpGet.getRequestLine());
		HttpResponse response;
		try {
			response = httpClient.execute(httpGet);
			ci.println("Response: " + response.getStatusLine());
			String message = EntityUtils.toString(response.getEntity());
			System.out.println("Stats: " + message);
			
		} catch (ClientProtocolException e) {
			throw new IdGeneratorException(e);
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			httpGet.releaseConnection();
		}
		
	}
	
	/**
	 * Logs out of the id generation session marked by the token. 
	 * @param jsonTokenString
	 * @param ci 
	 */
	protected void logout(String serviceUrl, String jsonTokenString, CommandInterpreter ci) {

		HttpPost httpPost = new HttpPost(serviceUrl + "/" + "logout");
		ci.println("Logging out. Request: " + httpPost.getRequestLine() + ", token: " + jsonTokenString);
		httpPost.setEntity(new StringEntity(jsonTokenString, ContentType.create("application/json")));
		HttpResponse response;
		try {
			response = httpClient.execute(httpPost);
			ci.println("Response: " + response.getStatusLine());
		} catch (ClientProtocolException e) {
			throw new IdGeneratorException(e);
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			httpPost.releaseConnection();
		}
	}
	
	private String getCredentialsString(String userName, String password)
			throws JsonGenerationException, JsonMappingException, IOException {
		CisCredentials credentials = new CisCredentials(userName, password);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(credentials);
	}


}