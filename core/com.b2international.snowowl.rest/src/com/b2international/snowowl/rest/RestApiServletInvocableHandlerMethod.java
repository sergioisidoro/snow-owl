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

import java.lang.reflect.Method;

import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.events.RequestBuilder;
import com.b2international.snowowl.core.request.SystemRequestBuilder;
import com.b2international.snowowl.eventbus.IEventBus;

/**
 * @since 6.5
 */
public class RestApiServletInvocableHandlerMethod extends ServletInvocableHandlerMethod {

	public RestApiServletInvocableHandlerMethod(Object handler, Method method) {
		super(handler, method);
	}

	public RestApiServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}
	
	@Override
	protected Object doInvoke(Object... args) throws Exception {
		RequestBuilder<ServiceProvider, ?> req = (RequestBuilder<ServiceProvider, ?>) super.doInvoke(args);
		if (req instanceof SystemRequestBuilder<?>) {
			return DeferredResults.wrap(
				((SystemRequestBuilder<?>) req).buildAsync()
					.execute(ApplicationContext.getServiceForClass(IEventBus.class))
			);
		}
		throw new UnsupportedOperationException("Unsupported request builder " + req);
	}
	
	@Override
	protected HttpStatus getResponseStatus() {
		return super.getResponseStatus();
	}

}
