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
package com.b2international.snowowl.snomed.datastore.internal.id;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.IDisposableService;
import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.core.exceptions.NotImplementedException;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator;
import com.b2international.snowowl.snomed.datastore.id.reservations.ISnomedIdentiferReservationService;
import com.b2international.snowowl.snomed.datastore.id.reservations.Reservation;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.SingleIdReservation;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * SNOMED CT identifier generator that delegates the id generation to the
 * external RESTful service hosted by IHTSDO (CIS). More information on the service
 * can be found here:
 * {@link https://confluence.ihtsdotools.org/display/TOOLS/IHTSDO+Component+Identifier+Service}
 * 
 */
public class CisSnomedIdentifierGenerator implements ISnomedIdentifierGenerator, ISnomedIdentiferReservationService, IDisposableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CisSnomedIdentifierGenerator.class);

	private String externalIdGeneratorUrl;
	private String externalIdGeneratorPort;
	private String externalIdGeneratorContextRoot;
	
	private ObjectMapper mapper = new ObjectMapper();
	private HttpClient httpClient = new DefaultHttpClient();

	// the full URL
	private String serviceUrl;

	private boolean isDisposed;

	/**
	 * @param reservationService 
	 * @param externalIdGeneratorUrl
	 * @param externalIdGeneratorPort
	 * @param externalIdGeneratorContextRoot
	 */
	public CisSnomedIdentifierGenerator(String externalIdGeneratorUrl, String externalIdGeneratorPort,
			String externalIdGeneratorContextRoot) {
		Preconditions.checkNotNull(externalIdGeneratorUrl, "External id generator URL is null.");

		this.externalIdGeneratorUrl = externalIdGeneratorUrl;
		this.externalIdGeneratorPort = externalIdGeneratorPort;
		this.externalIdGeneratorContextRoot = externalIdGeneratorContextRoot;
		serviceUrl = externalIdGeneratorUrl;

		if (externalIdGeneratorPort != null) {
			serviceUrl = serviceUrl + ":" + externalIdGeneratorPort;
		}
		
		if (externalIdGeneratorContextRoot != null) {
			serviceUrl = serviceUrl + "/" + externalIdGeneratorContextRoot;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.b2international.snowowl.snomed.datastore.id.
	 * ISnomedIdentifierGenerator#generateId(com.b2international.snowowl.core.
	 * terminology.ComponentCategory)
	 */
	@Override
	public String generateId(ComponentCategory componentCategory) {
		Preconditions.checkNotNull(componentCategory, "Component category is null");

		String jsonTokenString = null;
		try {
			jsonTokenString = login();
			String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
			String generatedId = generateNonExtensionId(tokenString, componentCategory);
			return generatedId;
		} catch (IOException e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
		} finally {
			//try to log out if we logged in
			if (jsonTokenString !=null) {
				logout(jsonTokenString);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.b2international.snowowl.snomed.datastore.id.
	 * ISnomedIdentifierGenerator#generateId(com.b2international.snowowl.core.
	 * terminology.ComponentCategory, java.lang.String)
	 */
	@Override
	public String generateId(ComponentCategory componentCategory, String namespaceString) {

		Preconditions.checkNotNull(componentCategory, "Component category is null");
		Preconditions.checkNotNull(namespaceString, "Namespace is null");
		int namespace = Integer.valueOf(namespaceString);

		
		String jsonTokenString = null;
		try {
			jsonTokenString = login();
			String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
			String generatedId = genererateExtensionId(tokenString, namespace, componentCategory);
			return generatedId;
		} catch (IOException e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
		} finally {
			//try to log out if we logged in
			if (jsonTokenString !=null) {
				logout(jsonTokenString);
			}
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
	 * Generates an extension id using a session marked by the token.
	 * The id is generated for the namespace and component supplied.
	 * @param tokenString
	 * @param namespace
	 * @param componentCategory
	 * @return generated SNOMED CT identifier
	 * @throws JsonProcessingException 
	 */
	protected String genererateExtensionId(String tokenString, int namespace, ComponentCategory componentCategory) throws JsonProcessingException {
		String generationDataString = getGenerationDataString(namespace, componentCategory);
		return generateId(generationDataString, tokenString);
	}
	
	/**
	 * Generates an non-extension id using a session marked by the token.
	 * The id is generated for the component supplied.
	 * @param tokenString
	 * @param componentCategory
	 * @return generated SNOMED CT identifier
	 * @throws JsonProcessingException 
	 */
	protected String generateNonExtensionId(String tokenString, ComponentCategory componentCategory) throws JsonProcessingException {
		String generationDataString = getGenerationDataString(componentCategory);
		return generateId(generationDataString, tokenString);
	}

	/*
	 * Returns a new generated concept id
	 */
	private String generateId(String generationDataString, String tokenString) {

		HttpPost httpPost = new HttpPost(serviceUrl + "/" + "sct/generate?token=" + tokenString);
		LOGGER.info("Retrieving a generated id. Request: {}", httpPost.getRequestLine());

		try {
			httpPost.setEntity(new StringEntity(generationDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);

			LOGGER.debug("Response: {}", response.getStatusLine());

			String conceptId = EntityUtils.toString(response.getEntity());

			LOGGER.debug("Generated concept id: {} ", conceptId);
			return conceptId;
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

	private String getCredentialsString(String userName, String password)
			throws JsonGenerationException, JsonMappingException, IOException {
		IhtsdoCredentials credentials = new IhtsdoCredentials(userName, password);
		return mapper.writeValueAsString(credentials);
	}

	/*
	 * JSON body data for extension id generation
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getGenerationDataString(int namespace, ComponentCategory componentCategory)
			throws JsonProcessingException {
		GenerationData generationData = new GenerationData();
		generationData.setNamespace(namespace);
		generationData.setComponentCategory(componentCategory);
		return mapper.writeValueAsString(generationData);
	}
	
	/*
	 * JSON body data for core id generation
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getGenerationDataString(ComponentCategory componentCategory)
			throws JsonProcessingException {
		GenerationData generationData = new GenerationData();
		generationData.setComponentCategory(componentCategory);
		return mapper.writeValueAsString(generationData);
	}

	@Override
	public void create(String reservationName, Reservation reservation) {
		
		if (reservation instanceof SingleIdReservation) {
			SingleIdReservation singleIdReservation = (SingleIdReservation) reservation;
			ISnomedIdentifier snomedIdentifier = singleIdReservation.getSnomedIdentifier();
			String sctId = snomedIdentifier.toString();
			if (StringUtils.isEmpty(snomedIdentifier.getNamespace())) {
				//core register
			} else {
				//extension register
			}
		} else {
			throw new IllegalArgumentException("Could not reserve id for reservation type: " + reservation.getClass());
		}
		
		throw new NotImplementedException();
		
	}
	
	/*
	 * JSON body data for extension id registration
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getRegistrationDataString(int namespace, ComponentCategory componentCategory)
			throws JsonProcessingException {
		RegistrationData registrationData = new RegistrationData();
		registrationData.setNamespace(namespace);
		registrationData.setComponentCategory(componentCategory);
		return mapper.writeValueAsString(registrationData);
	}
	
	/*
	 * JSON body data for core id registration
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getRegistrationtionDataString(ComponentCategory componentCategory)
			throws JsonProcessingException {
		RegistrationData registrationData = new RegistrationData();
		registrationData.setComponentCategory(componentCategory);
		return mapper.writeValueAsString(registrationData);
	}

	@Override
	public Collection<Reservation> getReservations() {
		throw new NotImplementedException();
	}

	@Override
	public Reservation getReservation(String reservationName) {
		throw new NotImplementedException();
	}

	@Override
	public void delete(String reservationName) {
		throw new NotImplementedException();
	}

	@Override
	public boolean isReserved(String componentId) {
		
		HttpGet httpGet = new HttpGet(serviceUrl + "/" + "sct/ids/" + componentId);

		LOGGER.info("Is reserved? Request: {}.", httpGet.getRequestLine());
		HttpResponse response;
		try {
			response = httpClient.execute(httpGet);
			LOGGER.debug("Response: {}", response.getStatusLine());
			
			String responseString = EntityUtils.toString(response.getEntity());
			ObjectMapper mapper = new ObjectMapper();
			SctId sctId = mapper.readValue(responseString, SctId.class);
			return !sctId.getStatus().equals("Available");
		} catch (ClientProtocolException e) {
			throw new IdGeneratorException(e);
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpGet.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
	}

	@Override
	public void dispose() {
		if (httpClient != null) {
			httpClient.getConnectionManager().shutdown();
			httpClient = null;
			isDisposed = true;
		}
	}

	@Override
	public boolean isDisposed() {
		return isDisposed;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IhtsdoSnomedIdentifierGenerator [externalIdGeneratorUrl=" + externalIdGeneratorUrl
				+ ", externalIdGeneratorPort=" + externalIdGeneratorPort + ", externalIdGeneratorContextRoot="
				+ externalIdGeneratorContextRoot + "]";
	}
	
}
