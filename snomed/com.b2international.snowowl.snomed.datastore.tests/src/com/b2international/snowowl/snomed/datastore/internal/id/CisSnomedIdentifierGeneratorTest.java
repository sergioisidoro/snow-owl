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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.CisExistingIdsReservation;
import com.google.common.collect.Lists;

/**
 * Test to exercise the support for the external id generation service.
 */
@FixMethodOrder(MethodSorters.JVM)
public class CisSnomedIdentifierGeneratorTest {
	
	private final static String SERVICE_URL = "http://107.170.101.181:3000"; //$NON-NLS-N$
	private final static String SERVICE_PORT = ":3000"; //$NON-NLS-N$
	private final static String SERVICE_CONTEXT_ROOT = "api"; //$NON-NLS-N$
	private final static String SERVICE_CLIENT_KEY = "snow_owl_dev"; //$NON-NLS-N$
	private final static String B2I_NAMESPACE = "1000154"; //$NON-NLS-N$
	
	@Test
	public void bulkGenerateTest() {
		try {
		CisSnomedIdentifierGenerator generator = 
				new CisSnomedIdentifierGenerator(SERVICE_URL, SERVICE_PORT, SERVICE_CONTEXT_ROOT, SERVICE_CLIENT_KEY);
		
		Collection<String> generateIds = generator.generateIds(3, ComponentCategory.CONCEPT);
		for (String id : generateIds) {
			System.out.println("Generated id: " + id);
		}
		Assert.assertEquals(3, generateIds.size());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	//@Test
	public void bulkPublishTest() {
		CisExistingIdsReservation reservation = new CisExistingIdsReservation(SERVICE_URL, SERVICE_PORT, SERVICE_CONTEXT_ROOT, SERVICE_CLIENT_KEY);
		ArrayList<ISnomedIdentifier> componentsToPublish = Lists.newArrayList(SnomedIdentifier.of("356000"),
				SnomedIdentifier.of("357009"));
		reservation.publish(componentsToPublish);
	}
	
	//@Test
	public void generateExtensionConceptTest() {
		CisSnomedIdentifierGenerator generator = 
				new CisSnomedIdentifierGenerator(SERVICE_URL, SERVICE_PORT, SERVICE_CONTEXT_ROOT, SERVICE_CLIENT_KEY);
		
		String id = generator.generateId(ComponentCategory.CONCEPT, B2I_NAMESPACE);
		System.out.println("Id: " + id);
		Assert.assertNotNull(id);
	}
	
	//@Test
	public void generateCoreConceptTest() {
		CisSnomedIdentifierGenerator generator = 
				new CisSnomedIdentifierGenerator(SERVICE_URL, SERVICE_PORT, SERVICE_CONTEXT_ROOT, SERVICE_CLIENT_KEY);
		
		String id = generator.generateId(ComponentCategory.CONCEPT);
		System.out.println("Id: " + id);
		Assert.assertNotNull(id);
	}

}
