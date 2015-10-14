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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sandbox to exercise the IHTSDO SNOMED CT identifier service endpoints.
 */
@FixMethodOrder(MethodSorters.JVM)
public class IhtsdoIdentifierServiceTest {

	class IhtsdoCredentials {

		private String username;
		private String password;

		public IhtsdoCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

	}
	
	private final static String SERVICE_URL = "http://107.170.101.181:3000/api";
	private final static String USERNAME = "snowowl-dev-b2i"; // $NON-NLS-N$
	private final static String PASSWORD = "hAAYLYMX5gc98SDEz9cr"; // $NON-NLS-N$

	private static String jsonTokenString;
	private static String tokenString;
	
	//@Test
	public void credentialJsonTest() throws JsonGenerationException, JsonMappingException, IOException {
		IhtsdoCredentials credentials = new IhtsdoCredentials(USERNAME, PASSWORD);
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(System.out, credentials);
	}
	
	//@Test
	public void tokenJsonTest() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			String string = "{\"token\":\"JMA8EeNs04EjRGNv8neXjA00\"}";
			System.out.println("Input: " + string);
			Token token = mapper.readValue(string, Token.class);
			System.out.println("Token: " + token.getToken());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getCredentialsString() throws JsonGenerationException, JsonMappingException, IOException {
		IhtsdoCredentials credentials = new IhtsdoCredentials(USERNAME, PASSWORD);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(credentials);
	}

	@Before
	public void loginTest() {
		System.out.println("------------ Logging in ------------");
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(SERVICE_URL + "/" + "login");
		System.out.println("Executing request: " + httpPost.getRequestLine());

		try {
			String credentialsString = getCredentialsString();
			System.out.println("Credentials: " + credentialsString);
			httpPost.setEntity(new StringEntity(credentialsString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);
			
			System.out.println(response.getStatusLine());
			
			jsonTokenString = EntityUtils.toString(response.getEntity());
			ObjectMapper mapper = new ObjectMapper();
			tokenString = mapper.readValue(jsonTokenString, Token.class).getToken();
			
			System.out.println("Json token: " + jsonTokenString + ", token: " + tokenString);
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Releasing the connection.");
			httpPost.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
		
	}
	
	@Test
	public void namespacesTest() {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(SERVICE_URL + "/users/" + USERNAME + "/namespaces?"
				+ "token=" + tokenString + "&username=" + USERNAME);
		System.out.println("----------------------------------------");
		System.out.println("Executing request: " + httpGet.getRequestLine());

		try {
			HttpResponse response = httpClient.execute(httpGet);
			System.out.println(response.getStatusLine());
			String responseString = EntityUtils.toString(response.getEntity());
			System.out.println("Response: " + responseString);
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Releasing the connection.");
			httpGet.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	@Test
	public void getConceptIdTest() {
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(SERVICE_URL + "/" + "sct/generate?token=" + tokenString);
		System.out.println("----------------------------------------");
		System.out.println("Executing request: " + httpPost.getRequestLine());

		try {
			String generationDataString = getGenerationDataString();
			System.out.println("Generation data: " + generationDataString);
			httpPost.setEntity(new StringEntity(generationDataString, ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);
			
			System.out.println(response.getStatusLine());
			
			String conceptId = EntityUtils.toString(response.getEntity());
			
			System.out.println("ConceptId: " + conceptId);
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Releasing the connection.");
			httpPost.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	/**
	 * @return
	 * @throws JsonProcessingException 
	 */
	private String getGenerationDataString() throws JsonProcessingException {
		GenerationData generationData = new GenerationData();
		generationData.setNamespace(12345);
		generationData.setPartitionId("01");
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(generationData);
	}

	@After
	public void logoutTest() {
		System.out.println("------------------ Logging out ------------------");
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(SERVICE_URL + "/" + "logout");

		System.out.println("Executing request: " + httpPost.getRequestLine());
		try {
			System.out.println("Token: " + jsonTokenString);
			httpPost.setEntity(new StringEntity(jsonTokenString,
					ContentType.create("application/json")));
			HttpResponse response = httpClient.execute(httpPost);
			
			System.out.println(response.getStatusLine());
			Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Releasing the connection.");
			httpPost.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
		
	}

	// @Before
	public void setup() throws ClientProtocolException, IOException {

		// CredentialsProvider credsProvider = new BasicCredentialsProvider();
		// credsProvider.setCredentials(
		// new AuthScope("httpbin.org", 80),
		// new UsernamePasswordCredentials("user", "passwd"));
		// httpclient.setDefaultCredentialsProvider(credsProvider)
		// .build();
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = null;
		try {
			// httpGet = new
			// HttpGet("http://httpbin.org/basic-auth/user/passwd");
			httpGet = new HttpGet("http://107.170.101.181:3000/api/login");

			System.out.println("Executing request " + httpGet.getRequestLine());
			HttpResponse response = httpClient.execute(httpGet);
			// try {
			System.out.println("----------------------------------------");
			System.out.println(response.getStatusLine());
			System.out.println(EntityUtils.toString(response.getEntity()));
			// } finally {
			// response.close();
			// }
		} finally {
			httpGet.releaseConnection();
			httpClient.getConnectionManager().shutdown(); // Close the instance
															// here
		}
		// httpclient.close();
		// httpclient.
		// }
		/*
		 * 
		 * HttpClient httpclient = HttpCli HttpGet httpGet = new
		 * HttpGet("http://targethost/homepage"); CloseableHttpResponse
		 * response1 = httpclient.execute(httpGet); // The underlying HTTP
		 * connection is still held by the response object // to allow the
		 * response content to be streamed directly from the network socket. //
		 * In order to ensure correct deallocation of system resources // the
		 * user MUST call CloseableHttpResponse#close() from a finally clause.
		 * // Please note that if response content is not fully consumed the
		 * underlying // connection cannot be safely re-used and will be shut
		 * down and discarded // by the connection manager. try {
		 * System.out.println(response1.getStatusLine()); HttpEntity entity1 =
		 * response1.getEntity(); // do something useful with the response body
		 * // and ensure it is fully consumed EntityUtils.consume(entity1); }
		 * finally { response1.close(); }
		 */
	}

}
