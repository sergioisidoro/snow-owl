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

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import com.b2international.snowowl.datastore.request.RepositoryRequests;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.collect.ImmutableList;

import io.swagger.annotations.Api;
import springfox.documentation.schema.AlternateTypeRule;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * @since 6.5
 */
@Configuration
@PropertySource("classpath:com/b2international/snowowl/rest/api-configuration.properties")
public class RestApiSwaggerConfig implements BeanFactoryPostProcessor {

	@Value("${api.termsOfServiceUrl}")
	private String apiTermsOfServiceUrl;
	
	@Value("${api.contact}")
	private String apiContact;
	
	@Value("${api.license}")
	private String apiLicense;
	
	@Value("${api.licenseUrl}")
	private String apiLicenseUrl;
	
	static final List<Class<?>> apis = ImmutableList.of(RepositoryRequests.class);
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		final TypeResolver resolver = new TypeResolver();
		for (Class<?> api : apis) {
			final String apiGroup = api.getAnnotation(Api.class).tags()[0];
			Docket docket = new Docket(DocumentationType.SWAGGER_2)
			  .select()
			  	.apis(handler -> {
			  		return true;
			  	})
			  	.paths(path -> {
			  		return true;
			  	})
			  	.build()
			  .groupName(apiGroup.toLowerCase())
			  .useDefaultResponseMessages(false)
			  .ignoredParameterTypes(Principal.class)
			  .genericModelSubstitutes(ResponseEntity.class, DeferredResult.class)
			  .alternateTypeRules(new AlternateTypeRule(resolver.resolve(UUID.class), resolver.resolve(String.class)))
			  .apiInfo(new ApiInfo("Snow Owl " + apiGroup + " API", "TODO docs", "TODO version", apiTermsOfServiceUrl, new Contact("B2i Healthcare", apiLicenseUrl, apiContact), apiLicense, apiLicenseUrl, Collections.emptyList()));
			beanFactory.registerSingleton(api.getSimpleName(), docket);
		}
	}

}
