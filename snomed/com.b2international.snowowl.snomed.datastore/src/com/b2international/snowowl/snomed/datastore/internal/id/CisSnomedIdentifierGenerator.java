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

import com.b2international.snowowl.core.IDisposableService;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.GenerationData;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.SctId;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.Token;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.CisService;
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
public class CisSnomedIdentifierGenerator extends CisService implements ISnomedIdentifierGenerator, IDisposableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CisSnomedIdentifierGenerator.class);

	private boolean isDisposed;

	/**
	 * @param reservationService 
	 * @param externalIdGeneratorUrl
	 * @param externalIdGeneratorPort
	 * @param externalIdGeneratorContextRoot
	 */
	public CisSnomedIdentifierGenerator(String externalIdGeneratorUrl, String externalIdGeneratorPort,
			String externalIdGeneratorContextRoot, String externalIdGeneratorClientKey) {
		super(externalIdGeneratorUrl, externalIdGeneratorPort, externalIdGeneratorContextRoot, externalIdGeneratorClientKey);
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

			String responseString = EntityUtils.toString(response.getEntity());
			String sctId = mapper.readValue(responseString, SctId.class).getSctid();

			LOGGER.debug("Generated concept id: {} ", sctId);
			return sctId;
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpPost.releaseConnection();
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
	 * JSON body data for extension id generation
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getGenerationDataString(int namespace, ComponentCategory componentCategory)
			throws JsonProcessingException {
		GenerationData generationData = new GenerationData();
		generationData.setSoftware(externalIdGeneratorClientKey);
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
		generationData.setSoftware(externalIdGeneratorClientKey);
		generationData.setComponentCategory(componentCategory);
		return mapper.writeValueAsString(generationData);
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
