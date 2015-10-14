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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

public class SnomedExternalIdentifierTest {
	
	@Before
	public void setup() throws ClientProtocolException, IOException {

//		CredentialsProvider credsProvider = new BasicCredentialsProvider();
//        credsProvider.setCredentials(
//                new AuthScope("httpbin.org", 80),
//                new UsernamePasswordCredentials("user", "passwd"));
		// httpclient.setDefaultCredentialsProvider(credsProvider)
		//         .build();
        DefaultHttpClient httpClient = new DefaultHttpClient();
       HttpGet httpGet = null;
        try {
             //httpGet = new HttpGet("http://httpbin.org/basic-auth/user/passwd");
        	httpGet = new HttpGet("http://107.170.101.181:3000/api/testService");
        	
            System.out.println("Executing request " + httpGet.getRequestLine());
            HttpResponse response = httpClient.execute(httpGet);
           // try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                System.out.println(EntityUtils.toString(response.getEntity()));
           // } finally {
                //response.close();
           // }
        } finally {
        	httpGet.releaseConnection();
        	httpClient.getConnectionManager().shutdown(); // Close the instance here
        }
        	//httpclient.close();
        	//httpclient.
       // }
				/*
	
		HttpClient httpclient = HttpCli
		HttpGet httpGet = new HttpGet("http://targethost/homepage");
		CloseableHttpResponse response1 = httpclient.execute(httpGet);
		// The underlying HTTP connection is still held by the response object
		// to allow the response content to be streamed directly from the network socket.
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager. 
		try {
		    System.out.println(response1.getStatusLine());
		    HttpEntity entity1 = response1.getEntity();
		    // do something useful with the response body
		    // and ensure it is fully consumed
		    EntityUtils.consume(entity1);
		} finally {
		    response1.close();
		}
		*/
	}
	
	@Test
	public void testTestEndpoint() {
		
	}

}
