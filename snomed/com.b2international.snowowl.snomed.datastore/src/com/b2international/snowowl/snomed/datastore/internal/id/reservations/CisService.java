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
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
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
import com.b2international.snowowl.snomed.datastore.internal.id.beans.CisCredentials;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.UncheckedTimeoutException;

/**
 * Component Identifier Service (CIS) methods
 */
public class CisService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CisService.class);

	//job poll timeout
	private static final int TIMEOUT_IN_SEC = 3;
	
	protected static final String JSON_JOB_ID_KEY = "id"; //$NON-NLS-N$

	protected static final String JSON_JOB_STATUS_KEY = "status"; //$NON-NLS-N$
	
	// the full URL
	protected String serviceUrl;
	
	protected ObjectMapper mapper = new ObjectMapper();
	protected HttpClient httpClient = new DefaultHttpClient();
	
	protected String externalIdGeneratorClientKey;
	
	/**
	 * Construct a CIS service with a service URL
	 */
	public CisService(String externalIdGeneratorUrl, String externalIdGeneratorPort,
			String externalIdGeneratorContextRoot, String externalIdGeneratorClientKey) {
		Preconditions.checkNotNull(externalIdGeneratorUrl, "External id generator URL is null.");
		Preconditions.checkNotNull(externalIdGeneratorClientKey, "Client key is null.");
		this.externalIdGeneratorClientKey = externalIdGeneratorClientKey;
		
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

			String responseString = EntityUtils.toString(response.getEntity());
			
			//wrap the returned body as an exception
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IdGeneratorException(responseString);
			}
			
			return responseString;
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpPost.releaseConnection();
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
			String responseString = EntityUtils.toString(response.getEntity());
			
			//wrap the returned body as an exception
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IdGeneratorException(responseString);
			}
			
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
	 * Executes a REST call and handles the response
	 * @param httpRequestBase
	 * @return reponse string
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	protected String handleRestCall(HttpRequestBase httpRequestBase) throws IOException {
		HttpResponse response = httpClient.execute(httpRequestBase);

		LOGGER.info("Response: {}", response);

		String responseString = EntityUtils.toString(response.getEntity());
		
		//wrap the returned body as an exception
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IdGeneratorException(responseString);
		}
		return responseString;
	}
	
	/**
	 * Executes a REST call and handles the response
	 * @param dataString
	 * @param httpEntityEnclosingRequestBase
	 * @return response string
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	protected String handleRestCall(String dataString, HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase) throws IOException {
		httpEntityEnclosingRequestBase.setEntity(new StringEntity(dataString, ContentType.create("application/json")));
		HttpResponse response = httpClient.execute(httpEntityEnclosingRequestBase);

		LOGGER.info("Response: '{}' for request body '{}'.", response, dataString);

		String responseString = EntityUtils.toString(response.getEntity());
		
		//wrap the returned body as an exception
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IdGeneratorException(responseString);
		}
		return responseString;
	}
	
	/**
	 * Polls the status of a job and only returns when the job is completed or timed out.
	 * 
	 * @return status integer
	 * @param id
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws InterruptedException 
	 */
	protected int pollJobStatus(String tokenString, String id) throws ClientProtocolException, IOException, InterruptedException  {
		
		Stopwatch stopWatch = Stopwatch.createStarted();
		
		int status = 0;
		
		HttpGet httpGet = null;
		while (status == 0) {
			long elapsedTimeInSeconds = stopWatch.elapsed(TimeUnit.SECONDS);
			
			httpGet = new HttpGet(serviceUrl + "/bulk/jobs/" + id + "?token=" + tokenString);
			HttpResponse response = httpClient.execute(httpGet);

			LOGGER.debug("Job polling request response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			
			//wrap the returned body as an exception
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IdGeneratorException(responseString);
			}
			
			LOGGER.debug("Jobs response: " + responseString);
			JsonNode root = mapper.readValue(responseString, JsonNode.class);
			status = root.get(JSON_JOB_STATUS_KEY).asInt();
			Thread.sleep(100);
			
			if (elapsedTimeInSeconds > TIMEOUT_IN_SEC) {
				LOGGER.info("Job status for job '{}' polling was timed out after {} seconds.", id, elapsedTimeInSeconds);
				
				//clean up as much as possible
				if (httpGet !=null) {
					httpGet.releaseConnection();
				}
				throw new UncheckedTimeoutException("Polling timed out after " + elapsedTimeInSeconds + " seconds");
			}
		}
		stopWatch.stop();
		stopWatch = null;
		return status;
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
		CisCredentials credentials = new CisCredentials(userName, password);
		return mapper.writeValueAsString(credentials);
	}
}
