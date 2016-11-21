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
package com.b2international.snowowl.api.rest;

import java.security.Principal;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.b2international.snowowl.api.codesystem.domain.ICodeSystemVersionProperties;
import com.b2international.snowowl.api.rest.domain.ICodeSystemVersionPropertiesMixin;
import com.b2international.snowowl.eventbus.IEventBus;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import springfox.documentation.schema.AlternateTypeRule;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * The Spring configuration class for Snow Owl's internal REST services module.
 *
 * @since 1.0
 */
@Configuration
@EnableSwagger2
@EnableWebMvc
public class ServicesConfiguration extends WebMvcConfigurerAdapter {

	private ServletContext servletContext;

	private String apiVersion;

	private String apiTitle;
	private String apiDescription;
	private String apiTermsOfServiceUrl;
	private String apiContact;
	private String apiLicense;
	private String apiLicenseUrl;

	@Autowired
	public void setServletContext(final ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Autowired
	@Value("${api.version}")
	public void setApiVersion(final String apiVersion) {
		this.apiVersion = apiVersion;
	}

	@Autowired
	@Value("${api.title}")
	public void setApiTitle(final String apiTitle) {
		this.apiTitle = apiTitle;
	}

	@Autowired
	@Value("${api.description}")
	public void setApiDescription(final String apiDescription) {
		this.apiDescription = apiDescription;
	}

	@Autowired
	@Value("${api.termsOfServiceUrl}")
	public void setApiTermsOfServiceUrl(final String apiTermsOfServiceUrl) {
		this.apiTermsOfServiceUrl = apiTermsOfServiceUrl;
	}

	@Autowired
	@Value("${api.contact}")
	public void setApiContact(final String apiContact) {
		this.apiContact = apiContact;
	}

	@Autowired
	@Value("${api.license}")
	public void setApiLicense(final String apiLicense) {
		this.apiLicense = apiLicense;
	}

	@Autowired
	@Value("${api.licenseUrl}")
	public void setApiLicenseUrl(final String apiLicenseUrl) {
		this.apiLicenseUrl = apiLicenseUrl;
	}

	@Bean
	public IEventBus eventBus() {
		return com.b2international.snowowl.core.ApplicationContext.getInstance().getServiceChecked(IEventBus.class);
	}

	@Bean
	public Docket swaggerSpringMvcPlugin() {
		final TypeResolver resolver = new TypeResolver();
		return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .pathProvider(new RelativePathProvider(servletContext))
            .useDefaultResponseMessages(false)
            .ignoredParameterTypes(Principal.class, Void.class)
            .genericModelSubstitutes(ResponseEntity.class)
            .alternateTypeRules(new AlternateTypeRule(resolver.resolve(UUID.class), resolver.resolve(String.class)));
	}

	private ApiInfo apiInfo() {
		final Contact contact = new Contact("B2i Healthcare", "http://b2i.sg", apiContact);
		return new ApiInfo(apiTitle, apiDescription, apiVersion, apiTermsOfServiceUrl, contact, apiLicense, apiLicenseUrl);
	}

	@Bean
	public ObjectMapper objectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
		objectMapper.registerModule(new GuavaModule());
		objectMapper.setSerializationInclusion(Include.NON_EMPTY);
		final ISO8601DateFormat df = new ISO8601DateFormat();
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		objectMapper.setDateFormat(df);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.addMixInAnnotations(ICodeSystemVersionProperties.class, ICodeSystemVersionPropertiesMixin.class);
		return objectMapper;
	}

	@Override
	public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
		final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setWriteAcceptCharset(false);
		converters.add(stringConverter);

		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new ResourceHttpMessageConverter());

		final MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
		jacksonConverter.setObjectMapper(objectMapper());
		converters.add(jacksonConverter);
	}

	@Override
	public void configurePathMatch(final PathMatchConfigurer configurer) {
		configurer.setUseRegisteredSuffixPatternMatch(true);
		configurer.setPathMatcher(new AntPathWildcardMatcher());
	}
}