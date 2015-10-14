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
package com.b2international.snowowl.snomed.datastore.config;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SNOMED CT related application level configuration parameters.
 * 
 * @since 3.4
 */
public class SnomedCoreConfiguration {
	
	public enum IdGenerationSource {
		INTERNAL, //Snow Owl's internal random id generator
		EXTERNAL_IHTSDO //IHTSDO's external id generator
	}

	public static final String ELK_REASONER_ID = "org.semanticweb.elk.elk.reasoner.factory"; //$NON-NLS-1$
	private static final String DEFAULT_REASONER = ELK_REASONER_ID;
	public static final String DEFAULT_LANGUAGE = "en-gb"; //$NON-NLS-1$
	public static final int DEFAULT_MAXIMUM_REASONER_COUNT = 2;
	public static final int DEFAULT_MAXIMUM_REASONER_RESULTS = 10;
	public static final String DEFAULT_NAMESPACE = "";
	
	@Min(1)
	@Max(3)
	private int maxReasonerCount = DEFAULT_MAXIMUM_REASONER_COUNT;
	
	@Min(1)
	@Max(10)
	private int maxReasonerResults = DEFAULT_MAXIMUM_REASONER_RESULTS;
	
	@NotEmpty
	private String defaultReasoner = DEFAULT_REASONER;
	
	@NotEmpty
	private String language = DEFAULT_LANGUAGE;
	
	@NotNull
	private String defaultNamespace = DEFAULT_NAMESPACE;
	
	//IHTSDO's external Id generation configuration
	@JsonProperty("idGenerationSource")
	private IdGenerationSource idGenerationSource = IdGenerationSource.INTERNAL;
	
	@JsonProperty("externalIdGeneratorUrl")
	private String externalIdGeneratorUrl;
	
	@JsonProperty("externalIdGeneratorPort")
	private String externalIdGeneratorPort;
	
	@JsonProperty("externalIdGeneratorContextRoot")
	private String externalIdGeneratorContextRoot;
	
	private boolean concreteDomainSupport;
	private boolean showReasonerUsageWarning = true;
	
	/**
	 * @return the maxReasonerCount
	 */
	@JsonProperty
	public int getMaxReasonerCount() {
		return maxReasonerCount;
	}
	
	/**
	 * @param maxReasonerCount the maxReasonerCount to set
	 */
	@JsonProperty
	public void setMaxReasonerCount(int maxReasonerCount) {
		this.maxReasonerCount = maxReasonerCount;
	}
	
	/**
	 * @return the maxReasonerResults
	 */
	@JsonProperty
	public int getMaxReasonerResults() {
		return maxReasonerResults;
	}
	
	/**
	 * @param maxReasonerResults the maxReasonerResults to set
	 */
	@JsonProperty
	public void setMaxReasonerResults(int maxReasonerResults) {
		this.maxReasonerResults = maxReasonerResults;
	}
	
	/**
	 * @return the language code currently used for SNOMED CT
	 */
	@JsonProperty
	public String getLanguage() {
		return language;
	}
	
	/**
	 * @return the default namespace used for SNOMED CT
	 */
	@JsonProperty
	public String getDefaultNamespace() {
		return defaultNamespace;
	}
	
	/**
	 * @param defaultNamespace the default namespace to set
	 */
	@JsonProperty
	public void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}
	
	/**
	 * @param language the SNOMED CT language code to set
	 */
	@JsonProperty
	public void setLanguage(String language) {
		this.language = language;
	}
	
	/**
	 * @return the currently set default reasoner ID 
	 */
	@JsonProperty
	public String getDefaultReasoner() {
		return defaultReasoner;
	}
	
	/**
	 * @param defaultReasoner - the reasoner to set as default
	 */
	@JsonProperty
	public void setDefaultReasoner(String defaultReasoner) {
		this.defaultReasoner = defaultReasoner;
	}

	@JsonProperty("concreteDomainSupport")
	public boolean isConcreteDomainSupported() {
		return concreteDomainSupport;
	}
	
	@JsonProperty("concreteDomainSupport")
	public void setConcreteDomainSupported(boolean concreteDomainSupport) {
		this.concreteDomainSupport = concreteDomainSupport;
	}

	@JsonProperty("showReasonerUsageWarning")
	public boolean isShowReasonerUsageWarningEnabled() {
		return showReasonerUsageWarning ;
	}
	
	@JsonProperty("showReasonerUsageWarning")
	public void setShowReasonerUsageWarningEnabled(boolean showReasonerUsageWarning) {
		this.showReasonerUsageWarning = showReasonerUsageWarning;
	}

	public IdGenerationSource getIdGenerationSource() {
		return idGenerationSource;
	}

	public void setIdGenerationSource(IdGenerationSource idGenerationSource) {
		this.idGenerationSource = idGenerationSource;
	}

	/**
	 * Returns the root URL of the external id generator REST service
	 * @return
	 */
	public String getExternalIdGeneratorUrl() {
		return externalIdGeneratorUrl;
	}

	public void setExternalIdGeneratorUrl(String externalIdGeneratorUrl) {
		this.externalIdGeneratorUrl = externalIdGeneratorUrl;
	}

	/**
	 * Returns the port of the external id generator REST service
	 * @return
	 */
	public String getExternalIdGeneratorPort() {
		return externalIdGeneratorPort;
	}

	public void setExternalIdGeneratorPort(String externalIdGeneratorPort) {
		this.externalIdGeneratorPort = externalIdGeneratorPort;
	}

	/**
	 * Returns the context root of the external id generator's REST URL
	 * @return
	 */
	public String getExternalIdGeneratorContextRoot() {
		return externalIdGeneratorContextRoot;
	}

	public void setExternalIdGeneratorContextRoot(String externalIdGeneratorContextRoot) {
		this.externalIdGeneratorContextRoot = externalIdGeneratorContextRoot;
	}
	
}
