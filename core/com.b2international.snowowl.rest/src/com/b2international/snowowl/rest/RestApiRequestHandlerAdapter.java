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

import java.util.List;

import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.DeferredResultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitterReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBodyReturnValueHandler;

import com.b2international.commons.ReflectionUtils;
import com.google.common.collect.ImmutableList;

import io.swagger.annotations.ApiOperation;

/**
 * @since 6.5
 */
public class RestApiRequestHandlerAdapter extends RequestMappingHandlerAdapter {

	@Override
	public void afterPropertiesSet() {
		setOrder(0);
		ContentNegotiationManager contentNegotiationManager = ReflectionUtils.getField(RequestMappingHandlerAdapter.class, this, "contentNegotiationManager");
		List<Object> requestResponseBodyAdvice = ReflectionUtils.getField(RequestMappingHandlerAdapter.class, this, "requestResponseBodyAdvice");
		setReturnValueHandlers(ImmutableList.of(
			new StreamingResponseBodyReturnValueHandler(),
			new ResponseBodyEmitterReturnValueHandler(getMessageConverters()),
			new HttpEntityMethodProcessor(getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice),
			new DeferredResultMethodReturnValueHandler(),
			new RestApiRequestResponseBodyMethodProcessor(getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice)
		));
		setCustomArgumentResolvers(ImmutableList.of(
			new ApiParamMethodArgumentResolver()
		));
		super.afterPropertiesSet();
	}
	
	@Override
	protected boolean supportsInternal(HandlerMethod handlerMethod) {
		return true;
	}
	
	@Override
	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
		if (handlerMethod.hasMethodAnnotation(ApiOperation.class)) {
			return new RestApiServletInvocableHandlerMethod(handlerMethod);
		}
		return super.createInvocableHandlerMethod(handlerMethod);
	}
	
}
