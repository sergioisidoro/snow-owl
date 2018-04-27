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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo.BuilderConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @since 6.5
 */
public class RestApiRequestHandlerMapping extends RequestMappingHandlerMapping {
	
	private final List<Class<?>> apis;

	public RestApiRequestHandlerMapping(List<Class<?>> apis) {
		this.apis = apis;
		setOrder(0);
	}
	
	@Override
	protected void initHandlerMethods() {
		for (Class<?> api : apis) {
			if (isHandler(api)) {
				detectHandlerMethods(api);
			}
		}
	}
	
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Api.class);
	}
	
	@Override
	protected void detectHandlerMethods(Object handler) {
		final Class<?> userType = (Class<?>) handler;
		
		Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(userType,
				(MetadataLookup<RequestMappingInfo>) method -> {
					try {
						return getMappingForMethod(method, userType);
					} catch (Throwable ex) {
						throw new IllegalStateException("Invalid mapping on handler class [" +
								userType.getName() + "]: " + method, ex);
					}
				});

		if (logger.isDebugEnabled()) {
			logger.debug(methods.size() + " request handler methods found on " + userType + ": " + methods);
		}
		for (Map.Entry<Method, RequestMappingInfo> entry : methods.entrySet()) {
			Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), userType);
			RequestMappingInfo mapping = entry.getValue();
			registerHandlerMethod(handler, invocableMethod, mapping);
		}
	}
	
	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMappingInfo info = createMethodMapping(method);
		if (info != null) {
			RequestMappingInfo typeInfo = createTypeMapping(handlerType);
			if (typeInfo != null) {
				info = typeInfo.combine(info);
			}
		}
		return info;
	}
	
	private RequestMappingInfo createTypeMapping(Class<?> handlerType) {
		if (handlerType.isAnnotationPresent(Api.class)) {
			Api api = handlerType.getAnnotation(Api.class);
			
			final String[] paths;
			if (handlerType.isAnnotationPresent(Path.class)) {
				paths = new String[] {handlerType.getAnnotation(Path.class).value()};
			} else {
				paths = new String[] {"/"};
			}
			
			String[] consumes = new String[0];
			if (!api.consumes().isEmpty()) {
				consumes = api.consumes().split(",");
			}
			
			String[] produces = new String[0];
			if (!api.produces().isEmpty()) {
				produces = api.produces().split(",");
			}
			
			return RequestMappingInfo
					.paths(resolveEmbeddedValuesInPatterns(paths))
					.methods(getHttpMethods(handlerType))
					.consumes(consumes)
					.produces(produces)
					.mappingName(handlerType.getName())
					.options((BuilderConfiguration) com.b2international.commons.ReflectionUtils.getField(RequestMappingHandlerMapping.class, this, "config"))
					.build();
		}
		return null;
	}

	private RequestMappingInfo createMethodMapping(Method method) {
		if (method.isAnnotationPresent(ApiOperation.class)) {
			final ApiOperation operationSpec = method.getAnnotation(ApiOperation.class);
			
			final String[] paths;
			if (method.isAnnotationPresent(Path.class)) {
				paths = new String[] {method.getAnnotation(Path.class).value()};
			} else {
				paths = new String[] {"/"};
			}
			
			String[] consumes = new String[0];
			if (!operationSpec.consumes().isEmpty()) {
				consumes = operationSpec.consumes().split(",");
			}
			
			String[] produces = new String[0];
			if (!operationSpec.produces().isEmpty()) {
				produces = operationSpec.produces().split(",");
			}
			
			return RequestMappingInfo
				.paths(resolveEmbeddedValuesInPatterns(paths))
				.methods(getHttpMethods(method))
				.consumes(consumes)
				.produces(produces)
				.mappingName(method.getName())
				.options((BuilderConfiguration) com.b2international.commons.ReflectionUtils.getField(RequestMappingHandlerMapping.class, this, "config"))
				.build();
		}
		return null;
	}

	private RequestMethod[] getHttpMethods(Object object) {
		final Annotation[] annotations;
		if (object instanceof Method) {
			final Method method = (Method) object;
			annotations = method.getAnnotations();
		} else if (object instanceof Class<?>) {
			final Class<?> type = (Class<?>) object;
			annotations = type.getAnnotations();
		} else {
			throw new IllegalArgumentException("Cannot extract HTTP method from " + object);
		}
		return Arrays.stream(annotations)
			.filter(this::isHttpAnnotation)
			.map(annotation -> annotation.annotationType().getSimpleName())
			.map(RequestMethod::valueOf)
			.toArray(size -> new RequestMethod[size]);
	}
	
	private boolean isHttpAnnotation(Annotation annotation) {
		return annotation instanceof GET
				|| annotation instanceof POST
				|| annotation instanceof PUT
				|| annotation instanceof DELETE
				|| annotation instanceof HEAD;
				
	}
	
}
