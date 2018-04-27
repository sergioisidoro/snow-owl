/*
 * Copyright 2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.rest;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Empty subclass of {@link AbstractSecurityWebApplicationInitializer} that is required to initialize Spring Security properly.
 * 
 * @since 6.5
 */
public class RestApiSecurityInitializer extends AbstractSecurityWebApplicationInitializer {
}