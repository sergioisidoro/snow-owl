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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.SnowOwlApplication;
import com.b2international.snowowl.core.config.SnowOwlConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration.IdGenerationSource;

/**
 * OSGI command contribution with command to test IHTSDO's external Id
 * generation service.
 *
 */
public class SnomedDatastoreCommandProvider implements CommandProvider {

	@Override
	public String getHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("---SNOMED service commands---\n");
		buffer.append(
				"\tsnomed ihtsdo_id_service_test - Checks the status of IHTSDO's external id generation service\n");
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

			if ("ihtsdo_id_service_test".equals(cmd)) {
				checkIdGenerationService(interpreter);
				return;
			}
			interpreter.println(getHelp());
		} catch (Exception ex) {
			interpreter.println(ex.getMessage());
		}
	}

	public synchronized void checkIdGenerationService(CommandInterpreter ci) {
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
		
		ci.println("Checking IHTSDO's external id generation service...");

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = null;
		try {
			if (StringUtils.isEmpty(externalIdGeneratorPort)) {
				httpGet = new HttpGet(externalIdGeneratorUrl + "/" + externalIdGeneratorContextRoot + "/testService");
			} else {
				httpGet = new HttpGet(externalIdGeneratorUrl + ":" + externalIdGeneratorPort 
						+ "/" + externalIdGeneratorContextRoot + "/testService");
			}
			ci.println("Request: " + httpGet.getRequestLine());
			HttpResponse response = httpClient.execute(httpGet);
			ci.println("---------------------------------------------------------");
			ci.println(response.getStatusLine());
			ci.println("Response: " + EntityUtils.toString(response.getEntity()));
		} catch (final Throwable t) {
			ci.println("Error: " + t.getMessage());
		} finally {
			httpGet.releaseConnection();
			httpClient.getConnectionManager().shutdown();
		}
	}

}