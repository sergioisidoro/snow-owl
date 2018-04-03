/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.fhir.core.model.codesystem;

import java.util.Collection;

import javax.validation.Valid;

import com.b2international.snowowl.fhir.core.codesystems.CodeSystemHierarchyMeaning;
import com.b2international.snowowl.fhir.core.model.TerminologyResource;
import com.b2international.snowowl.fhir.core.model.dt.Code;
import com.b2international.snowowl.fhir.core.model.dt.Id;
import com.b2international.snowowl.fhir.core.model.dt.Identifier;
import com.b2international.snowowl.fhir.core.model.dt.Narrative;
import com.b2international.snowowl.fhir.core.model.dt.Uri;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * This class represents a FHIR code system.
 * The CodeSystem resource is used to declare the existence of a code system, and its key properties:

 * <ul>
 * <li>Identifying URL and version
 * <li>Description, Copyright, publication date, and other metadata
 * <li>Some key properties of the code system itself - whether it's case sensitive, version safe, and whether it defines a compositional grammar
 * <li>What filters can be used in value sets that use the code system in a ValueSet.compose element
 * <li>What properties the concepts defined by the code system
 * </ul>
 * @see <a href="https://www.hl7.org/fhir/codesystem.html">FHIR:CodeSystem</a>
 * @since 6.3
 */
@ApiModel("CodeSystem")
public class CodeSystem extends TerminologyResource {
	
	//FHIR header "resourceType" : "CodeSystem",
	@JsonProperty
	private String resourceType = "CodeSystem";
	
	@JsonProperty
	private Code hierarchyMeaning;
	
	@JsonProperty
	private String publisher;
	
	/*
	 * The properties supported by this code system
	 */
	@Valid
	@JsonProperty("property")
	private Collection<SupportedConceptProperty> properties;
	
	/*
	 * Concepts in the code system
	 */
	@Valid
	@JsonProperty("concept")
	private Collection<Concept> concepts;
	
	public CodeSystem(Id id, Code language, Narrative text, Uri url, Identifier identifier, String version, String name, 
			String title, Code status, String publisher, String description, 
			Code hierarchyMeaning, Collection<SupportedConceptProperty> properties, Collection<Concept> concepts) {
		
		super(id, language, text, url, identifier, version, name, title, status, publisher, description);
		this.hierarchyMeaning = hierarchyMeaning;
		this.properties = properties;
		this.concepts = concepts;
	}
	
	public static Builder builder(String codeSystemId) {
		return new Builder(codeSystemId);
	}

	public static class Builder extends TerminologyResource.Builder<Builder, CodeSystem> {

		private Code hierarchyMeaning;
		
		private Collection<Concept> concepts = Sets.newHashSet();
		
		private Collection<SupportedConceptProperty> properties = Lists.newArrayList();
		
		public Builder(String codeSystemId) {
			super(codeSystemId);
		}
		
		@Override
		protected Builder getSelf() {
			return this;
		}

		public Builder hierarchyMeaning(CodeSystemHierarchyMeaning codeSystemHierarchyMeaning) {
			this.hierarchyMeaning = codeSystemHierarchyMeaning.getCode();
			return getSelf();
		}
		
		public Builder addProperty(SupportedConceptProperty property) {
			this.properties.add(property);
			return getSelf();
		}
		
		public Builder addConcept(Concept concept)  {
			this.concepts.add(concept);
			return getSelf();
		}
		
		@Override
		protected CodeSystem doBuild() {
			return new CodeSystem(id, language, text, url, identifier, version, name, title, status, publisher, description, hierarchyMeaning, properties, concepts);
		}
	}
		
}