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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.IDisposableService;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.BulkGenerationData;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.GenerationData;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.IdRecord;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.SctId;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.Token;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.CisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

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
		
		try {
			String generationDataString = getGenerationDataString(componentCategory);
			return generateId(generationDataString);
		} catch (JsonProcessingException e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
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

		try {
			String generationDataString = getGenerationDataString(namespaceString, componentCategory);
			return generateId(generationDataString);
		} catch (JsonProcessingException e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
		}
	}
	
	/**
	 * Generates an id for a request JSON data.
	 * @param dataString
	 * @return generated identifier
	 */
	protected String generateId(String dataString) {

		String jsonTokenString = null;
		HttpPost httpPost = null;
		
		try {
			jsonTokenString = login();
			String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
			
			httpPost = new HttpPost(serviceUrl + "/" + "sct/generate?token=" + tokenString);
			LOGGER.info("Retrieving a generated id. Request: {}", httpPost.getRequestLine());

			//try {
			httpPost.setEntity(new StringEntity(dataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);
			LOGGER.debug("Response: {}", response.getStatusLine());
			
			String responseString = EntityUtils.toString(response.getEntity());
			String sctId = mapper.readValue(responseString, SctId.class).getSctid();

			LOGGER.info("Generated concept id: {} ", sctId);
			return sctId;
		} catch (IOException e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
		} finally {
			//try to log out if we logged in
			if (jsonTokenString !=null) {
				logout(jsonTokenString);
			}
			LOGGER.debug("Releasing the connection.");
			if (httpPost != null) {
				httpPost.releaseConnection();
			}
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	
	public Collection<String> generateIds(int quantity, ComponentCategory componentCategory) {
		try {
			String generationDataString = getBulkGenerationDataString(quantity, componentCategory);
			return generateIds(generationDataString);
		} catch (JsonProcessingException e) {
			throw new IdGeneratorException("Exception when trying to bulk generate core ids.", e);
		}
	}

	public Collection<String> generateIds(int quantity, ComponentCategory componentCategory, String namespaceString) {
		try {
			String generationDataString = getBulkGenerationDataString(quantity, namespaceString, componentCategory);
			return generateIds(generationDataString);
		} catch (JsonProcessingException e) {
			throw new IdGeneratorException("Exception when trying to bulk generate extension ids.", e);
		}
	}
	
	public Collection<String> generateIds(String generationDataString) {

		String jsonTokenString = null;
		HttpPost httpPost = null;
		HttpGet httpGet = null;
		
		try {
			jsonTokenString = login();
			String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();

			LOGGER.info("Generation data: {}.", generationDataString);

			// create the job
			httpPost = new HttpPost(serviceUrl + "/sct/bulk/generate?token=" + tokenString);
			httpPost.setEntity(new StringEntity(generationDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);

			LOGGER.debug("Bulk generation job creation response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			JsonNode root = mapper.readValue(responseString, JsonNode.class);
			String id = root.get(JSON_JOB_ID_KEY).asText();
			LOGGER.debug("Job id: {}.", id);

			//poll the job until the results are ready
			pollJobStatus(tokenString, id);

			// getting the records
			httpGet = new HttpGet(serviceUrl + "/bulk/jobs/" + id + "/records?token=" + tokenString);
			response = httpClient.execute(httpGet);

			LOGGER.debug("Bulk generation record retrival response: {}", response.getStatusLine());

			responseString = EntityUtils.toString(response.getEntity());
			LOGGER.debug("Records response: {}.", responseString);
			IdRecord[] idRecords = mapper.readValue(responseString, IdRecord[].class);
			Collection<String> sctIds = Collections2.transform(Lists.newArrayList(idRecords), new Function<IdRecord, String>() {
				@Override
				public String apply(IdRecord input) {
					return input.getSctid();
				}
			});
			return sctIds;
		} catch (Exception e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
		} finally {
			// try to log out if we logged in
			if (jsonTokenString != null) {
				logout(jsonTokenString);
			}
			LOGGER.debug("Releasing the connections.");
			if (httpPost != null) {
				httpPost.releaseConnection();
			}
			
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
			//just in case the logout did not shut it down
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
	private String getGenerationDataString(String namespace, ComponentCategory componentCategory)
			throws JsonProcessingException {
		GenerationData generationData = new GenerationData();
		generationData.setSoftware(externalIdGeneratorClientKey);
		generationData.setNamespace(Integer.valueOf(namespace));
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
	
	/**
	 * @return
	 */
	private String getBulkGenerationDataString(int quantity, ComponentCategory componentCategory) throws JsonProcessingException {
		BulkGenerationData generationData = new BulkGenerationData();
		generationData.setSoftware(externalIdGeneratorClientKey);
		generationData.setQuantity(quantity);
		generationData.setComponentCategory(componentCategory);
		return mapper.writeValueAsString(generationData);
	}
	
	/**
	 * @return
	 */
	private String getBulkGenerationDataString(int quantity, String namespace, ComponentCategory componentCategory) throws JsonProcessingException {
		BulkGenerationData generationData = new BulkGenerationData();
		generationData.setSoftware(externalIdGeneratorClientKey);
		generationData.setNamespace(Integer.valueOf(namespace));
		generationData.setQuantity(quantity);
		generationData.setComponentCategory(componentCategory);
		return mapper.writeValueAsString(generationData);
	}
	
}
