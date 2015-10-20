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

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.b2international.snowowl.core.terminology.ComponentCategory;

/**
 * Test to exercise the support for the external id generation service.
 */
@FixMethodOrder(MethodSorters.JVM)
public class CisSnomedIdentifierGeneratorTest {
	
	private final static String SERVICE_URL = "http://107.170.101.181:3000"; //$NON-NLS-N$
	private final static String SERVICE_PORT = ":3000"; //$NON-NLS-N$
	private final static String SERVICE_CONTEXT_ROOT = "api"; //$NON-NLS-N$
	private final static String B2I_NAMESPACE = "1000154"; //$NON-NLS-N$
	
	@Test
	public void testExtensionConcept() {
		CisSnomedIdentifierGenerator generator = 
				new CisSnomedIdentifierGenerator(SERVICE_URL, SERVICE_PORT, SERVICE_CONTEXT_ROOT);
		
		String id = generator.generateId(ComponentCategory.CONCEPT, B2I_NAMESPACE);
		System.out.println("Id: " + id);
		Assert.assertNotNull(id);
	}
	
	@Test
	public void testCoreConcept() {
		CisSnomedIdentifierGenerator generator = 
				new CisSnomedIdentifierGenerator(SERVICE_URL, SERVICE_PORT, SERVICE_CONTEXT_ROOT);
		
		String id = generator.generateId(ComponentCategory.CONCEPT);
		System.out.println("Id: " + id);
		Assert.assertNotNull(id);
	}

}
