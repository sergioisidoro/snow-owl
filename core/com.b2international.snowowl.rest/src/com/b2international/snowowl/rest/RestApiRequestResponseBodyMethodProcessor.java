package com.b2international.snowowl.rest;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

public class RestApiRequestResponseBodyMethodProcessor extends RequestResponseBodyMethodProcessor {

	public RestApiRequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters, ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {
		super(converters, manager, requestResponseBodyAdvice);
	}
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return super.supportsParameter(parameter);
	}
	
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

}
