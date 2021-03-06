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
package com.b2international.snowowl.fhir.tests.serialization.dt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.b2international.snowowl.fhir.core.FhirConstants;
import com.b2international.snowowl.fhir.core.codesystems.IdentifierUse;
import com.b2international.snowowl.fhir.core.codesystems.IssueSeverity;
import com.b2international.snowowl.fhir.core.codesystems.IssueType;
import com.b2international.snowowl.fhir.core.codesystems.NarrativeStatus;
import com.b2international.snowowl.fhir.core.codesystems.OperationOutcomeCode;
import com.b2international.snowowl.fhir.core.codesystems.QuantityComparator;
import com.b2international.snowowl.fhir.core.exceptions.ValidationException;
import com.b2international.snowowl.fhir.core.model.Extension;
import com.b2international.snowowl.fhir.core.model.IntegerExtension;
import com.b2international.snowowl.fhir.core.model.Issue;
import com.b2international.snowowl.fhir.core.model.Issue.Builder;
import com.b2international.snowowl.fhir.core.model.dt.Code;
import com.b2international.snowowl.fhir.core.model.dt.CodeableConcept;
import com.b2international.snowowl.fhir.core.model.dt.Coding;
import com.b2international.snowowl.fhir.core.model.dt.ContactPoint;
import com.b2international.snowowl.fhir.core.model.dt.Identifier;
import com.b2international.snowowl.fhir.core.model.dt.Instant;
import com.b2international.snowowl.fhir.core.model.dt.Narrative;
import com.b2international.snowowl.fhir.core.model.dt.Period;
import com.b2international.snowowl.fhir.core.model.dt.Quantity;
import com.b2international.snowowl.fhir.core.model.dt.Range;
import com.b2international.snowowl.fhir.core.model.dt.Reference;
import com.b2international.snowowl.fhir.core.model.dt.Signature;
import com.b2international.snowowl.fhir.core.model.dt.SimpleQuantity;
import com.b2international.snowowl.fhir.core.model.dt.Uri;
import com.b2international.snowowl.fhir.tests.FhirExceptionIssueMatcher;
import com.b2international.snowowl.fhir.tests.FhirTest;

/**
 * 
 * Tests for complex data type serialization
 * 
 * @see https://www.hl7.org/fhir/datatypes.html
 * @since 6.6
 */
public class ComplexDataTypeSerializationTest extends FhirTest {
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	private Builder builder = Issue.builder()
			.code(IssueType.INVALID)
			.severity(IssueSeverity.ERROR)
			.diagnostics("1 validation error");
	
	@Test
	public void codingTest() throws Exception {
		
		Coding coding = Coding.builder()
			.code("1234")
			.system("http://snomed.info/sct")
			.version("20180131")
			.build();
		
		String expected = "{\"code\":\"1234\","
				+ "\"system\":\"http://snomed.info/sct\","
				+ "\"version\":\"20180131\"}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(coding));
	}
	
	@Test
	public void invalidCodingTest() throws Exception {
		
		Issue expectedIssue = builder.addLocation("Coding.code.codeValue")
				.codeableConceptWithDisplay(OperationOutcomeCode.MSG_PARAM_INVALID, "Parameter 'code.codeValue' content is invalid []. "
						+ "Violation: must match \"[^\\s]+([\\s]?[^\\s]+)*\".")
				.build();
		
		exception.expect(ValidationException.class);
		exception.expectMessage("1 validation error");
		exception.expect(FhirExceptionIssueMatcher.issue(expectedIssue));
		
		Coding.builder()
				.code("")
				.system("http://snomed.info/sct")
				.version("20180131")
				.build();
	}
	
	@Test
	public void codeableConceptTest() throws Exception {
		
		Coding coding = Coding.builder()
				.code("1234")
				.system("http://snomed.info/sct")
				.version("20180131")
				.build();
		
		CodeableConcept cc = CodeableConcept.builder()
				.addCoding(coding)
				.text("text")
				.build();
		
		printPrettyJson(cc);
		
		String expected = "{\"text\":\"text\","
				+ "\"coding\":"
					+ "[{\"code\":\"1234\","
					+ "\"system\":\"http://snomed.info/sct\","
					+ "\"version\":\"20180131\"}"
					+ "]"
				+ "}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(cc));
		
	}
	
	@Test
	public void narrativeTest() throws Exception {
		
		Narrative narrative = Narrative.builder()
				.div("<div>This is text</div>")
				.status(NarrativeStatus.GENERATED)
				.build();
		
		printPrettyJson(narrative);
		
		String expected = "{\"status\":\"generated\"," + 
				"\"div\":\"<div>This is text</div>\"}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(narrative));
	}
	
	@Test
	public void incorrentNarrativeTest() throws Exception {
		
		Issue expectedIssue = builder.addLocation("Narrative.div")
				.codeableConceptWithDisplay(OperationOutcomeCode.MSG_PARAM_INVALID, "Parameter 'div' content is invalid [<div>]. Violation: div content is invalid, minimally should be <div></div>.")
				.build();
		
		exception.expect(ValidationException.class);
		exception.expectMessage("1 validation error");
		exception.expect(FhirExceptionIssueMatcher.issue(expectedIssue));

		Narrative.builder()
			.div("<div>")
			.status(NarrativeStatus.GENERATED)
			.build();
	}
	
	@Test
	public void quantityTest() throws Exception {
		
		Quantity quantity = Quantity.builder()
			.value(12.3)
			.unit("mg")
			.system("uri:LOINC")
			.code("code")
			.comparator(QuantityComparator.GREATER_OR_EQUAL_TO)
			.build();
		
		printPrettyJson(quantity);
		
		String expected = "{\"value\":12.3,"
				+ "\"comparator\":\">=\","
				+ "\"unit\":\"mg\""
				+ ",\"system\":\"uri:LOINC\","
				+ "\"code\":\"code\"}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(quantity));
	}
	
	@Test
	public void incorrectSimpleQuantityTest() throws Exception {
		
		Issue expectedIssue = builder.addLocation("SimpleQuantity.comparator")
				.codeableConceptWithDisplay(OperationOutcomeCode.MSG_PARAM_INVALID, "Parameter 'comparator' content is invalid [Code [codeValue=>=]]. Violation: must be null.")
				.build();
		
		exception.expect(ValidationException.class);
		exception.expectMessage("1 validation error");
		exception.expect(FhirExceptionIssueMatcher.issue(expectedIssue));
		
		SimpleQuantity.builder()
			.value(12.3)
			.unit("mg")
			.system("uri:LOINC")
			.code("code")
			.comparator(QuantityComparator.GREATER_OR_EQUAL_TO)
			.build();
	}
	
	@Test
	public void periodTest() throws Exception {
		
		DateFormat df = new SimpleDateFormat(FhirConstants.DATE_TIME_FORMAT);
		Date startDate = df.parse(TEST_DATE_STRING);
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		
		Period period = new Period(startDate, cal.getTime());
		
		printPrettyJson(period);
		
		String expected = "{\"start\":\"2018-03-23T07:49:40+0000\"," + 
				"\"end\":\"2018-03-24T07:49:40+0000\"}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(period));
	}
	
	@Test
	public void rangeTest() throws Exception {
		
		SimpleQuantity low = SimpleQuantity.builder()
			.value(12.3)
			.unit("mg")
			.system("uri:LOINC")
			.code("code1")
			.build();
		
		SimpleQuantity high = (SimpleQuantity) SimpleQuantity.builder()
				.value(120.3)
				.unit("mg")
				.system("uri:LOINC")
				.code("code1")
				.build();
			
		
		Range range = new Range(low, high);
		
		printPrettyJson(range);
		
		String expected = "{\"low\":{\"value\":12.3,\"unit\":\"mg\",\"system\":\"uri:LOINC\",\"code\":\"code1\"},"
				+ "\"high\":{\"value\":120.3,\"unit\":\"mg\",\"system\":\"uri:LOINC\",\"code\":\"code1\"}}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(range));
	}
	
	@Test
	public void referenceTest() throws Exception {
		
		Identifier identifier = Identifier.builder()
			.system("system")
			.build();
		
		Reference reference = new Reference("reference url", identifier, "displayString");
		
		printPrettyJson(reference);
		
		String expected = "{\"reference\":\"reference url\"," + 
				"\"identifier\":{\"system\":\"system\"}," + 
				"\"display\":\"displayString\"}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(reference));
	}
	
	@Test
	public void extensionTest() throws Exception {
		
		@SuppressWarnings("rawtypes")
		Extension extension = new IntegerExtension("url", 1);
		
		printPrettyJson(extension);
		
		String expected = "{\"url\":\"url\",\"valueInteger\":1}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(extension));
	}
	
	@Test
	public void contactPointTest() throws Exception {
		
		ContactPoint cp = ContactPoint.builder()
				.id("element_id")
				.addExtension(new IntegerExtension("url", 1))
				.addExtension(new IntegerExtension("url2", 2))
				.period(new Period(null, null))
				.rank(1)
				.system("system")
				.value("value")
				.build();
		
		printPrettyJson(cp);
		
		String expected = "{\"id\":\"element_id\","
				+ "\"system\":\"system\","
				+ "\"value\":\"value\","
				+ "\"rank\":1,"
				+ "\"period\":{},"
				+ "\"extension\":"
					+ "[{\"url\":\"url\",\"valueInteger\":1},{\"url\":\"url2\",\"valueInteger\":2}]"
				+ "}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(cp));
	}
	
	@Test
	public void identifierTest() throws Exception {
		
		DateFormat df = new SimpleDateFormat(FhirConstants.DATE_TIME_FORMAT);
		Date startDate = df.parse(TEST_DATE_STRING);
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		
		Identifier refIdentifier = Identifier.builder()
				.system("system")
				.build();
		
		Period period = new Period(startDate, cal.getTime());
		
		Reference reference = new Reference("reference url", refIdentifier, "displayString");
		
		Coding coding = Coding.builder()
			.code("codingCode")
			.display("codingDisplay")
			.build();
		
		CodeableConcept codeableConcept = CodeableConcept.builder()
				.addCoding(coding)
				.text("codingText")
				.build();
		
		Identifier identifier = Identifier.builder()
			.use(IdentifierUse.OFFICIAL)
			.type(codeableConcept)
			.period(period)
			.system("system")
			.value("value")
			.assigner(reference)
			.build();
		
		String expected = "{\"use\":\"official\","
				+ "\"type\":"
					+ "{\"text\":\"codingText\","
					+ "\"coding\":"
						+ "[{\"code\":\"codingCode\","
						+ "\"display\":\"codingDisplay\"}"
						+ "]},"
					+ "\"system\":\"system\","
					+ "\"value\":\"value\","
					+ "\"period\":"
						+ "{\"start\":\"2018-03-23T07:49:40+0000\","
						+ "\"end\":\"2018-03-24T07:49:40+0000\"}"
					+ ",\"assigner\":"
						+ "{\"reference\":\"reference url\","
						+ "\"identifier\":"
							+ "{\"system\":\"system\"},"
						+ "\"display\":\"displayString\"}"
					+ "}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(identifier));
	}
	
	@Test
	public void signatureUriTest() throws Exception {
		
		DateFormat df = new SimpleDateFormat(FhirConstants.DATE_TIME_FORMAT);
		Date date = df.parse(TEST_DATE_STRING);
		Instant instant = Instant.builder().instant(date).build();
		
		Signature signature = Signature.builder()
			.addType(Coding.builder().build())
			.contentType(new Code("contentTypeCode"))
			.when(instant)
			.whoUri(new Uri("whoUri"))
			.onBehalfOfUri(new Uri("onBehalfUri"))
			.blob("blob".getBytes())
			.build();
		
		printPrettyJson(signature);
		
		String expected = "{\"when\":\"2018-03-23T07:49:40Z\","
				+ "\"whoUri\":\"whoUri\","
				+ "\"onBehalfOfUri\":\"onBehalfUri\","
				+ "\"blob\":[98,108,111,98],\"type\":[{}]}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(signature));
	}
	
	@Test
	public void signatureReferenceTest() throws Exception {
		
		DateFormat df = new SimpleDateFormat(FhirConstants.DATE_TIME_FORMAT);
		Date date = df.parse(TEST_DATE_STRING);
		Instant instant = Instant.builder().instant(date).build();
		
		Signature signature = Signature.builder()
			.addType(Coding.builder().build())
			.contentType(new Code("contentTypeCode"))
			.when(instant)
			.whoReference(new Reference("reference", Identifier.builder().build(), "display"))
			.onBehalfOfUri(new Uri("onBehalfUri"))
			.blob("blob".getBytes())
			.build();
		
		printPrettyJson(signature);
		
		String expected = "{\"when\":\"2018-03-23T07:49:40Z\","
				+ "\"whoReference\":"
					+ "{\"reference\":\"reference\","
					+ "\"identifier\":{},\"display\":\"display\"},"
				+ "\"onBehalfOfUri\":\"onBehalfUri\","
				+ "\"blob\":[98,108,111,98],"
				+ "\"type\":[{}]}";
		
		Assert.assertEquals(expected, objectMapper.writeValueAsString(signature));
	}
	
	@Test
	public void incorrectSignatureReferenceTest() throws Exception {
		
		Issue expectedIssue = builder.addLocation("Signature.valid")
				.codeableConceptWithDisplay(OperationOutcomeCode.MSG_PARAM_INVALID, "Parameter 'valid' content is invalid [false]."
						+ " Violation: Either URI or Reference should be set for the 'who' and 'onBehalfOf' fields.")
				.build();
		
		exception.expect(ValidationException.class);
		exception.expectMessage("1 validation error");
		exception.expect(FhirExceptionIssueMatcher.issue(expectedIssue));
		
		DateFormat df = new SimpleDateFormat(FhirConstants.DATE_TIME_FORMAT);
		Date date = df.parse(TEST_DATE_STRING);
		Instant instant = Instant.builder().instant(date).build();
		
		Signature.builder()
			.addType(Coding.builder().build())
			.contentType(new Code("contentTypeCode"))
			.when(instant)
			.whoUri(new Uri("whoUri"))
			.whoReference(new Reference("reference", Identifier.builder().build(), "display"))
			.onBehalfOfUri(new Uri("onBehalfUri"))
			.blob("blob".getBytes())
			.build();
	}

}
