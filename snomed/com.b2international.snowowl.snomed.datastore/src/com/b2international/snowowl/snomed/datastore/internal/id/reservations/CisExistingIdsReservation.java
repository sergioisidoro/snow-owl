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
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.StringUtils;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier;
import com.b2international.snowowl.snomed.datastore.id.reservations.DynamicReservation;
import com.b2international.snowowl.snomed.datastore.internal.id.IdGeneratorException;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.BulkPublishData;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.BulkRegisterData;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.BulkRegisterData.Record;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.RegistrationData;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.ReleaseAndPublishData;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.SctId;
import com.b2international.snowowl.snomed.datastore.internal.id.beans.Token;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

/**
 * Reservation that delegates work to the third-party CIS.
 */
public class CisExistingIdsReservation extends CisService implements DynamicReservation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CisExistingIdsReservation.class);
	
	public static final String NAME = "cis_exisiting_ids_reservation"; //$NON-NLS-N$
	
	/**
	 * @param externalIdGeneratorUrl
	 * @param externalIdGeneratorPort
	 * @param externalIdGeneratorContextRoot
	 */
	public CisExistingIdsReservation(String externalIdGeneratorUrl, String externalIdGeneratorPort,
			String externalIdGeneratorContextRoot, String externalIdGeneratorClientKey) {
		super(externalIdGeneratorUrl, externalIdGeneratorPort, externalIdGeneratorContextRoot, externalIdGeneratorClientKey);
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.reservations.Reservation#includes(com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier)
	 */
	@Override
	public boolean includes(ISnomedIdentifier identifier) {
		
		Preconditions.checkNotNull(identifier, "SNOMED CT identifier is null");
		String componentId = identifier.toString();
		
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

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.reservations.DynamicReservation#register(com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier)
	 */
	@Override
	public void register(ISnomedIdentifier snomedIdentifier) {
		Preconditions.checkNotNull(snomedIdentifier, "SNOMED CT identifier is null");

		String sctId = snomedIdentifier.toString();
		String namespace = snomedIdentifier.getNamespace();
		
		String jsonTokenString = null;
		LOGGER.info("Create reservation, with name: {} for id {}.", NAME, sctId);
		
		try {
		jsonTokenString = login();
		String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
		
		if (StringUtils.isEmpty(namespace)) {
			String reservationDataString = getRegistrationtionDataString(sctId);
			registerId(reservationDataString, tokenString);
		} else {
			String reservationDataString = getRegistrationtionDataString(sctId, namespace);
			registerId(reservationDataString, tokenString);
		}
		
		} catch (IOException e) {
			throw new IdGeneratorException("Exception when trying to register SNOMED CT id + " + sctId, e);
		} finally {
			//try to log out if we logged in
			if (jsonTokenString !=null) {
				logout(jsonTokenString);
			}
		}				
		
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.reservations.DynamicReservation#unregister(com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier)
	 */
	@Override
	public void unregister(ISnomedIdentifier snomedIdentifier) {
		Preconditions.checkNotNull(snomedIdentifier, "SNOMED CT identifier is null");

		String sctId = snomedIdentifier.toString();
		String namespace = snomedIdentifier.getNamespace();
		
		String jsonTokenString = null;
		LOGGER.info("Create reservation, with name: {} for id {}.", NAME, sctId);
		
		try {
		jsonTokenString = login();
		String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
		
		if (StringUtils.isEmpty(namespace)) {
			String releaseData = getReleaseOrPublishDataString(sctId);
			releaseId(releaseData, tokenString);
		} else {
			String releaseData = getReleaseOrPublishDataString(sctId, namespace);
			releaseId(releaseData, tokenString);
		}
		
		} catch (IOException e) {
			throw new IdGeneratorException("Exception when trying to register SNOMED CT id + " + sctId, e);
		} finally {
			//try to log out if we logged in
			if (jsonTokenString !=null) {
				logout(jsonTokenString);
			}
		}		
		
	}
	
	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.reservations.DynamicReservation#register(java.util.Collection)
	 */
	@Override
	public void register(Collection<ISnomedIdentifier> snomedIdentifiers) {
		String jsonTokenString = null;
		HttpPost httpPost = null;
		HttpGet httpGet = null;
		
		try {
			String bulkRegisterDataString = getBulkRegisterDataString(snomedIdentifiers);
			jsonTokenString = login();
			String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();

			LOGGER.info("Bulk register data: {}.", bulkRegisterDataString);

			// create the job
			httpPost = new HttpPost(serviceUrl + "/sct/bulk/register?token=" + tokenString);
			httpPost.setEntity(new StringEntity(bulkRegisterDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);

			LOGGER.info("Bulk register job response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			JsonNode root = mapper.readValue(responseString, JsonNode.class);
			String id = root.get(JSON_JOB_ID_KEY).asText();
			LOGGER.info("Register job id: {}.", id);

			//poll the job until the results are ready
			pollJobStatus(tokenString, id);

			// getting the records
			httpGet = new HttpGet(serviceUrl + "/bulk/jobs/" + id + "/records?token=" + tokenString);
			response = httpClient.execute(httpGet);

			LOGGER.debug("Bulk publication record retrival response: {}", response.getStatusLine());

			responseString = EntityUtils.toString(response.getEntity());
			LOGGER.debug("Records response: {}.", responseString);
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

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.reservations.DynamicReservation#unregister(java.util.Collection)
	 */
	@Override
	public void unregister(Collection<ISnomedIdentifier> snomedIdentifiers) {
		String jsonTokenString = null;
		HttpPut httpPut = null;
		HttpGet httpGet = null;
		
		try {
			String bulkReleaseDataString = getBulkPublishOrReleaseDataString(snomedIdentifiers);
			jsonTokenString = login();
			String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();

			LOGGER.info("Bulk release data: {}.", bulkReleaseDataString);

			// create the job
			httpPut = new HttpPut(serviceUrl + "/sct/bulk/release?token=" + tokenString);
			httpPut.setEntity(new StringEntity(bulkReleaseDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPut);

			LOGGER.info("Bulk release job response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			JsonNode root = mapper.readValue(responseString, JsonNode.class);
			String id = root.get(JSON_JOB_ID_KEY).asText();
			LOGGER.info("Register job id: {}.", id);

			//poll the job until the results are ready
			pollJobStatus(tokenString, id);

			// getting the records
			httpGet = new HttpGet(serviceUrl + "/bulk/jobs/" + id + "/records?token=" + tokenString);
			response = httpClient.execute(httpGet);

			LOGGER.debug("Bulk release record retrival response: {}", response.getStatusLine());

			responseString = EntityUtils.toString(response.getEntity());
			LOGGER.debug("Records response: {}.", responseString);
		} catch (Exception e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
		} finally {
			// try to log out if we logged in
			if (jsonTokenString != null) {
				logout(jsonTokenString);
			}
			LOGGER.debug("Releasing the connections.");
			if (httpPut != null) {
				httpPut.releaseConnection();
			}
			
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
			//just in case the logout did not shut it down
			httpClient.getConnectionManager().shutdown();
		}
		
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.reservations.DynamicReservation#publish(com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier)
	 */
	@Override
	public void publish(ISnomedIdentifier snomedIdentifier) {
		Preconditions.checkNotNull(snomedIdentifier, "SNOMED CT identifier is null");

		String sctId = snomedIdentifier.toString();
		String namespace = snomedIdentifier.getNamespace();
		
		String jsonTokenString = null;
		LOGGER.info("Publishing with CIS component with id {}.", sctId);
		
		try {
		jsonTokenString = login();
		String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
		
		if (StringUtils.isEmpty(namespace)) {
			String publishData = getReleaseOrPublishDataString(sctId);
			publishId(publishData, tokenString);
		} else {
			String publishData = getReleaseOrPublishDataString(sctId, namespace);
			publishId(publishData, tokenString);
		}
		
		} catch (IOException e) {
			throw new IdGeneratorException("Exception when trying to register SNOMED CT id + " + sctId, e);
		} finally {
			//try to log out if we logged in
			if (jsonTokenString !=null) {
				logout(jsonTokenString);
			}
		}		
	}

	/* (non-Javadoc)
	 * @see com.b2international.snowowl.snomed.datastore.id.reservations.DynamicReservation#publish(java.util.Collection)
	 */
	@Override
	public void publish(Collection<ISnomedIdentifier> snomedIdentifiers) {

		String jsonTokenString = null;
		HttpPut httpPut = null;
		HttpGet httpGet = null;
		
		try {
			String generationDataString = getBulkPublishOrReleaseDataString(snomedIdentifiers);
			jsonTokenString = login();
			String tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();

			LOGGER.info("Publish data: {}.", generationDataString);

			// create the job
			httpPut = new HttpPut(serviceUrl + "/sct/bulk/publish?token=" + tokenString);
			httpPut.setEntity(new StringEntity(generationDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPut);

			LOGGER.info("Bulk publish job response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			JsonNode root = mapper.readValue(responseString, JsonNode.class);
			String id = root.get(JSON_JOB_ID_KEY).asText();
			LOGGER.info("Job id: {}.", id);

			//poll the job until the results are ready
			pollJobStatus(tokenString, id);

			// getting the records
			httpGet = new HttpGet(serviceUrl + "/bulk/jobs/" + id + "/records?token=" + tokenString);
			response = httpClient.execute(httpGet);

			LOGGER.debug("Bulk publication record retrival response: {}", response.getStatusLine());

			responseString = EntityUtils.toString(response.getEntity());
			LOGGER.debug("Records response: {}.", responseString);
		} catch (Exception e) {
			throw new IdGeneratorException("Exception when calling the external id generator service.", e);
		} finally {
			// try to log out if we logged in
			if (jsonTokenString != null) {
				logout(jsonTokenString);
			}
			LOGGER.debug("Releasing the connections.");
			if (httpPut != null) {
				httpPut.releaseConnection();
			}
			
			if (httpGet != null) {
				httpGet.releaseConnection();
			}
			//just in case the logout did not shut it down
			httpClient.getConnectionManager().shutdown();
		}
	}

	/*
	 * Registers an id with CIS.
	 */
	private void registerId(String reservationDataString, String tokenString) {
		HttpPost httpPost = new HttpPost(serviceUrl + "/" + "sct/register?token=" + tokenString);
		LOGGER.info("Registering an id with CIS. Request: {}", httpPost.getRequestLine());

		try {
			httpPost.setEntity(new StringEntity(reservationDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);

			LOGGER.debug("Response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			String sctId = mapper.readValue(responseString, SctId.class).getSctid();

			LOGGER.debug("Registered concept, id: {} ", sctId);
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpPost.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	/*
	 * Releases an already assigned id from CIS.
	 */
	private void releaseId(String releaseDataString, String tokenString) {
		HttpPut httpPut = new HttpPut(serviceUrl + "/" + "sct/release?token=" + tokenString);
		LOGGER.info("Releasing an id from CIS. Request: {}", httpPut.getRequestLine());

		try {
			httpPut.setEntity(new StringEntity(releaseDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPut);

			LOGGER.debug("Response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			String sctId = mapper.readValue(responseString, SctId.class).getSctid();

			LOGGER.debug("Released concept, id: {} ", sctId);
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpPut.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	/*
	 * Publishes an already assigned id from CIS.
	 */
	private void publishId(String publishDataString, String tokenString) {
		HttpPut httpPut = new HttpPut(serviceUrl + "/" + "sct/publish?token=" + tokenString);
		LOGGER.info("Publishing an id within CIS. Request: {}", httpPut.getRequestLine());

		try {
			httpPut.setEntity(new StringEntity(publishDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPut);

			LOGGER.debug("Response: {}", response.getStatusLine());

			String responseString = EntityUtils.toString(response.getEntity());
			String sctId = mapper.readValue(responseString, SctId.class).getSctid();

			LOGGER.debug("Published concept, id: {} ", sctId);
		} catch (IOException e) {
			throw new IdGeneratorException(e);
		} finally {
			LOGGER.debug("Releasing the connection.");
			httpPut.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	/*
	 * JSON body data for extension id registration
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getRegistrationtionDataString(String sctId, String namespace)
			throws JsonProcessingException {
		RegistrationData registrationData = new RegistrationData();
		registrationData.setSoftware(externalIdGeneratorClientKey);
		registrationData.setSctId(sctId);
		registrationData.setNamespace(Integer.valueOf(namespace));
		return mapper.writeValueAsString(registrationData);
	}
	
	/*
	 * JSON body data for core id registration
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getRegistrationtionDataString(String sctId)
			throws JsonProcessingException {
		RegistrationData registrationData = new RegistrationData();
		registrationData.setSoftware(externalIdGeneratorClientKey);
		registrationData.setSctId(sctId);
		return mapper.writeValueAsString(registrationData);
	}
	
	/*
	 * JSON body data for extension id registration
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getReleaseOrPublishDataString(String sctId, String namespace)
			throws JsonProcessingException {
		ReleaseAndPublishData data = new ReleaseAndPublishData();
		data.setSoftware(externalIdGeneratorClientKey);
		data.setSctId(sctId);
		data.setNamespace(Integer.valueOf(namespace));
		return mapper.writeValueAsString(data);
	}
	
	/*
	 * JSON body data for core id registration
	 * @return JSON string
	 * @throws JsonProcessingException
	 */
	private String getReleaseOrPublishDataString(String sctId)
			throws JsonProcessingException {
		ReleaseAndPublishData data = new ReleaseAndPublishData();
		data.setSoftware(externalIdGeneratorClientKey);
		data.setSctId(sctId);
		return mapper.writeValueAsString(data);
	}
	
	/*
	 * @return
	 * @throws JsonProcessingException 
	 */
	private String getBulkPublishOrReleaseDataString(Collection<ISnomedIdentifier> snomedIdentifiers) throws JsonProcessingException {
		BulkPublishData generationData = new BulkPublishData();
		generationData.setSoftware(externalIdGeneratorClientKey);
		
		Collection<String> sctIds = Collections2.transform(snomedIdentifiers, new Function<ISnomedIdentifier, String>() {

			@Override
			public String apply(ISnomedIdentifier input) {
				return input.toString();
			}
		});
		generationData.setSctids(sctIds);
		return mapper.writeValueAsString(generationData);
	}
	
	/*
	 * @return
	 * @throws JsonProcessingException 
	 */
	private String getBulkRegisterDataString(Collection<ISnomedIdentifier> snomedIdentifiers) throws JsonProcessingException {
		BulkRegisterData registerData = new BulkRegisterData();
		registerData.setSoftware(externalIdGeneratorClientKey);
		
		for (ISnomedIdentifier snomedIdentifier : snomedIdentifiers) {
			Record record = registerData.new Record();
			record.setSctid(snomedIdentifier.toString());
			registerData.addRecord(record);
		}
		return mapper.writeValueAsString(registerData);
	}


}
