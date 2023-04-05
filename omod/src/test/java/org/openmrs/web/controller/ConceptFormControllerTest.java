/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptAttribute;
import org.openmrs.ConceptAttributeType;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptReferenceTermMap;
import org.openmrs.ConceptSource;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.util.LocaleUtility;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.web.controller.ConceptFormController.ConceptFormBackingObject;
import org.openmrs.web.test.WebTestHelper;
import org.openmrs.web.test.WebTestHelper.Response;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;

/**
 * Unit testing for the ConceptFormController.
 */

public class ConceptFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	WebTestHelper webTestHelper;

	@Autowired
	ConceptService conceptService;

	@Autowired
	ConceptFormController controller;

	private Locale britishEn;
	private WebDataBinder binder;

	private MockMvc mockMvc;

	protected static final String CONCEPT_ATTRIBUTES_XML = "org/openmrs/api/include/ConceptServiceTest-conceptAttributeType.xml";

	@BeforeEach
	public void updateSearchIndex() {
		super.updateSearchIndex();
		if (britishEn == null) {
			britishEn = LocaleUtility.fromSpecification("en_GB");
		}

		this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	/**
	 * Checks that the conceptId query param gets a concept from the database
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldGetConcept() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setParameter("conceptId", "3");

		ConceptFormBackingObject obj = (ConceptFormBackingObject) this.controller.formBackingObject(request);

		Assertions.assertNotNull(obj.getConcept().getConceptId());

	}

	/**
	 * Test to make sure a new patient form can save a person relationship
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldNotDeleteConceptsWhenConceptsAreLocked() throws Exception {

		ConceptService cs = Context.getConceptService();

		// set up the request and do an initial "get" as if the user loaded the
		// page for the first time

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dictionary/concept.form?conceptId=3");
		request.setSession(new MockHttpSession(null));

		this.mockMvc.perform(post("/dictionary/concept.form").param("action", "Delete Concept"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrlPattern("index.*"))
				.andExpect(model().hasNoErrors());

		Assertions.assertNotNull(cs.getConcept(3));

	}

	/**
	 * This test concept form being submitted with only one name supplied
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldAddConceptWithOnlyNameSpecified() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME)
						.param("descriptionsByLocale[en_GB].description", "some description")
						.param("concept.datatype", "1").param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		assertEquals(EXPECTED_PREFERRED_NAME, actualConcept.getFullySpecifiedName(britishEn).getName());
		Collection<ConceptName> actualNames = actualConcept.getNames();
		assertEquals(1, actualNames.size());
		assertNull(actualConcept.getShortNameInLocale(britishEn));
		assertNotNull(actualConcept.getDescription(britishEn));
	}

	/**
	 * This tests a concept form being submitted with also a short name supplied
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldAddConceptWithNameAndShortNameSpecified() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";
		final String EXPECTED_SHORT_NAME = "nonesuch";

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("shortNamesByLocale[en_GB].name", EXPECTED_SHORT_NAME)
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME)
						.param("descriptionsByLocale[en_GB].description", "some description")
						.param("concept.datatype", "1").param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		Collection<ConceptName> actualNames = actualConcept.getNames();
		assertEquals(2, actualNames.size());
		assertEquals(EXPECTED_PREFERRED_NAME, actualConcept.getFullySpecifiedName(britishEn).getName());
		assertEquals(1, actualConcept.getShortNames().size());
		assertNotNull(actualConcept.getShortNameInLocale(britishEn));
		assertEquals(EXPECTED_SHORT_NAME, actualConcept.getShortNameInLocale(britishEn).getName());
	}

	/**
	 * Tests a concept form being submitted with name/shortname/description all
	 * filled in
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldAddConceptWithNameAndShortNameAndDescriptionSpecifiedToCodeConcepts() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";
		final String EXPECTED_SHORT_NAME = "nonesuch";
		final String EXPECTED_DESCRIPTION = "this is not really a concept";

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("shortNamesByLocale[en_GB].name", EXPECTED_SHORT_NAME)
						.param("descriptionsByLocale[en_GB].description", EXPECTED_DESCRIPTION)
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME).param("concept.datatype", "4")
						.param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		Collection<ConceptName> actualNames = actualConcept.getNames();
		assertEquals(2, actualNames.size());
		assertEquals(EXPECTED_PREFERRED_NAME, actualConcept.getFullySpecifiedName(britishEn).getName());
		assertNotNull(actualConcept.getShortNameInLocale(britishEn));
		assertEquals(EXPECTED_SHORT_NAME, actualConcept.getShortNameInLocale(britishEn).getName());

		assertNotNull(actualConcept.getDescription(britishEn));
		assertEquals(EXPECTED_DESCRIPTION, actualConcept.getDescription(britishEn).getDescription());
	}

	/**
	 * Tests a concept form being submitted with a. name and description for numeric
	 * type of concepts
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldAddConceptWithNameAndShortNameAndDescriptionSpecifiedToNumericConcepts() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";
		final String EXPECTED_SHORT_NAME = "nonesuch";
		final String EXPECTED_DESCRIPTION = "this is not really a concept";

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("shortNamesByLocale[en_GB].name", EXPECTED_SHORT_NAME)
						.param("descriptionsByLocale[en_GB].description", EXPECTED_DESCRIPTION)
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME).param("concept.datatype", "1")
						.param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		Collection<ConceptName> actualNames = actualConcept.getNames();
		assertEquals(2, actualNames.size());
		assertEquals(EXPECTED_PREFERRED_NAME, actualConcept.getFullySpecifiedName(britishEn).getName());
		assertNotNull(actualConcept.getShortNameInLocale(britishEn));
		assertEquals(EXPECTED_SHORT_NAME, actualConcept.getShortNameInLocale(britishEn).getName());

		assertNotNull(actualConcept.getDescription(britishEn));
		assertEquals(EXPECTED_DESCRIPTION, actualConcept.getDescription(britishEn).getDescription());
	}

	/**
	 * Test adding a concept with a preferred name, short name, description and
	 * synonyms.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldAddConceptWithAllNamingSpecified() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";
		final String EXPECTED_SHORT_NAME = "nonesuch";
		final String EXPECTED_DESCRIPTION = "this is not really a concept";
		final String EXPECTED_SYNONYM_A = "phantom";
		final String EXPECTED_SYNONYM_B = "imaginary";
		final String EXPECTED_SYNONYM_C = "mock";

		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(OpenmrsConstants.GLOBAL_PROPERTY_LOCALE_ALLOWED_LIST);
		gp.setPropertyValue("en_GB, en_US");
		as.saveGlobalProperty(gp);

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("synonymsByLocale[en_GB][0].name", EXPECTED_SYNONYM_A)
						.param("synonymsByLocale[en_GB][1].name", EXPECTED_SYNONYM_B)
						.param("synonymsByLocale[en_GB][2].name", EXPECTED_SYNONYM_C)
						.param("shortNamesByLocale[en_GB].name", EXPECTED_SHORT_NAME)
						.param("descriptionsByLocale[en_GB].description", EXPECTED_DESCRIPTION)
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME).param("concept.datatype", "1")
						.param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		Collection<ConceptName> actualNames = actualConcept.getNames();
		assertEquals(5, actualNames.size());
		assertEquals(EXPECTED_PREFERRED_NAME, actualConcept.getFullySpecifiedName(britishEn).getName());
		assertNotNull(actualConcept.getShortNameInLocale(britishEn));
		assertEquals(EXPECTED_SHORT_NAME, actualConcept.getShortNameInLocale(britishEn).getName());

		assertNotNull(actualConcept.getDescription(britishEn));
		assertEquals(EXPECTED_DESCRIPTION, actualConcept.getDescription(britishEn).getDescription());

	}

	/**
	 * Test adding a concept with a preferred name, short name, description and
	 * synonyms.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldUpdateConceptWithNameAlreadyInSynonymList() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";
		final String EXPECTED_SHORT_NAME = "nonesuch";
		final String EXPECTED_DESCRIPTION = "this is not really a concept";
		final String EXPECTED_SYNONYM_A = "phantom";
		final String EXPECTED_SYNONYM_B = EXPECTED_PREFERRED_NAME;
		final String EXPECTED_SYNONYM_C = "mock";

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("synonymsByLocale[en_GB][0].name", EXPECTED_SYNONYM_A)
						.param("synonymsByLocale[en_GB][1].name", EXPECTED_SYNONYM_B)
						.param("synonymsByLocale[en_GB][2].name", EXPECTED_SYNONYM_C)
						.param("shortNamesByLocale[en_GB].name", EXPECTED_SHORT_NAME)
						.param("descriptionsByLocale[en_GB].description", EXPECTED_DESCRIPTION)
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME).param("concept.datatype", "1")
						.param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		Collection<ConceptName> actualNames = actualConcept.getNames();
		assertEquals(4, actualNames.size());
		assertEquals(EXPECTED_PREFERRED_NAME, actualConcept.getFullySpecifiedName(britishEn).getName());
		assertNotNull(actualConcept.getShortNameInLocale(britishEn));
		assertEquals(EXPECTED_SHORT_NAME, actualConcept.getShortNameInLocale(britishEn).getName());

	}

	/**
	 * Test adding a concept with a preferred name, short name, description and
	 * synonyms.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldUpdateConceptWithShortNameAlreadyInSynonymList() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";
		final String EXPECTED_SHORT_NAME = "nonesuch";
		final String EXPECTED_DESCRIPTION = "this is not really a concept";
		final String EXPECTED_SYNONYM_A = "phantom";
		final String EXPECTED_SYNONYM_B = EXPECTED_SHORT_NAME;
		final String EXPECTED_SYNONYM_C = "mock";

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("synonymsByLocale[en_GB][0].name", EXPECTED_SYNONYM_A)
						.param("synonymsByLocale[en_GB][1].name", EXPECTED_SYNONYM_B)
						.param("synonymsByLocale[en_GB][2].name", EXPECTED_SYNONYM_C)
						.param("shortNamesByLocale[en_GB].name", EXPECTED_SHORT_NAME)
						.param("descriptionsByLocale[en_GB].description", EXPECTED_DESCRIPTION)
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME).param("concept.datatype", "1")
						.param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		Collection<ConceptName> actualNames = actualConcept.getNames();
		assertEquals(4, actualNames.size());
		assertEquals(EXPECTED_PREFERRED_NAME, actualConcept.getFullySpecifiedName(britishEn).getName());
		assertNotNull(actualConcept.getShortNameInLocale(britishEn));
		assertEquals(EXPECTED_SHORT_NAME, actualConcept.getShortNameInLocale(britishEn).getName());

	}

	/**
	 * Test updating a concept by adding a name
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldUpdateConceptByAddingName() throws Exception {
		ConceptService cs = Context.getConceptService();

		// make sure the concept already exists
		Concept concept = cs.getConcept(3);
		assertNotNull(concept);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("conceptId", concept.getConceptId().toString())
						.param("namesByLocale[en_GB].name", "new name"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		updateSearchIndex();

		Concept actualConcept = cs.getConceptByName("new name");
		assertNotNull(actualConcept);
		assertEquals(concept.getConceptId(), actualConcept.getConceptId());
	}

	/**
	 * Test removing short name by adding a blank short name
	 *
	 * @throws Exception
	 */
	@Test
	public void shouldVoidShortName() throws Exception {
		final String CONCEPT_NAME = "default concept name";

		ConceptService cs = Context.getConceptService();

		final Concept concept = new Concept();
		concept.addName(new ConceptName(CONCEPT_NAME, britishEn));
		concept.setShortName(new ConceptName("shortname", britishEn));
		concept.addDescription(new ConceptDescription("some description", null));
		concept.setDatatype(cs.getConceptDatatype(1));
		concept.setConceptClass(cs.getConceptClass(1));
		cs.saveConcept(concept);

		Concept actualConcept = cs.getConceptByName(CONCEPT_NAME);
		assertThat(actualConcept.getShortNameInLocale(britishEn), is(notNullValue()));
		assertThat(actualConcept.getShortNames().size(), greaterThan(0));
		assertThat(actualConcept.getNames().size(), is(2));

		this.mockMvc.perform(post("/dictionary/concept.form").param("action", "")
				.param("conceptId", concept.getConceptId().toString()).param("shortNamesByLocale[en_GB].name", " ")
				.param("concept.datatype", "1").param("concept.conceptClass", "1")).andExpect(status().isFound())
				.andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}")).andExpect(model().hasNoErrors());

		actualConcept = cs.getConceptByName(CONCEPT_NAME);
		assertThat(actualConcept.getShortNameInLocale(britishEn), is(nullValue()));
		assertThat(actualConcept.getShortNames().size(), is(0));
		assertThat(actualConcept.getNames().size(), is(1));
	}

	/**
	 * Test adding a concept with a preferred name, short name, description and
	 * synonyms.
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldReplacePreviousDescription() throws Exception {
		final String EXPECTED_PREFERRED_NAME = "no such concept";
		final String EXPECTED_SHORT_NAME = "nonesuch";
		final String ORIGINAL_DESCRIPTION = "this is indescribable";
		final String EXPECTED_DESCRIPTION = "this is not really a concept";
		final String EXPECTED_SYNONYM_A = "phantom";
		final String EXPECTED_SYNONYM_B = EXPECTED_SHORT_NAME;
		final String EXPECTED_SYNONYM_C = "mock";

		ConceptService cs = Context.getConceptService();

		// first, add the concept with an original description
		Concept conceptToUpdate = new Concept();
		conceptToUpdate.addName(new ConceptName("demo name", Context.getLocale()));
		ConceptDescription originalConceptDescription = new ConceptDescription();
		originalConceptDescription.setLocale(britishEn);
		originalConceptDescription.setDescription(ORIGINAL_DESCRIPTION);
		conceptToUpdate.addDescription(originalConceptDescription);
		conceptToUpdate.setDatatype(cs.getConceptDatatype(1));
		conceptToUpdate.setConceptClass(cs.getConceptClass(1));
		cs.saveConcept(conceptToUpdate);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("synonymsByLocale[en_GB][0].name", EXPECTED_SYNONYM_A)
						.param("synonymsByLocale[en_GB][1].name", EXPECTED_SYNONYM_B)
						.param("synonymsByLocale[en_GB][2].name", EXPECTED_SYNONYM_C)
						.param("shortNamesByLocale[en_GB].name", EXPECTED_SHORT_NAME)
						.param("descriptionsByLocale[en_GB].description", EXPECTED_DESCRIPTION)
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME).param("concept.datatype", "1")
						.param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);

		assertNotNull(actualConcept.getDescription(britishEn));
		assertEquals(EXPECTED_DESCRIPTION, actualConcept.getDescription(britishEn).getDescription());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldCopyNumericValuesIntoNumericConcepts() throws Exception {
		final Double EXPECTED_LOW_ABSOLUTE = 100.0;
		final Double EXPECTED_LOW_CRITICAL = 103.0;
		final Double EXPECTED_LOW_NORMAL = 105.0;
		final Double EXPECTED_HI_NORMAL = 110.0;
		final Double EXPECTED_HI_CRITICAL = 117.0;
		final Double EXPECTED_HI_ABSOLUTE = 120.0;

		ConceptService cs = Context.getConceptService();

		this.mockMvc.perform(post("/dictionary/concept.form").param("action", "")
				.param("namesByLocale[en_GB].name", "WEIGHT (KG)").param("conceptId", "5089")
				.param("concept.datatype", "1").param("lowAbsolute", EXPECTED_LOW_ABSOLUTE.toString())
				.param("lowCritical", EXPECTED_LOW_CRITICAL.toString())
				.param("lowNormal", EXPECTED_LOW_NORMAL.toString()).param("hiNormal", EXPECTED_HI_NORMAL.toString())
				.param("hiCritical", EXPECTED_HI_CRITICAL.toString())
				.param("hiAbsolute", EXPECTED_HI_ABSOLUTE.toString())).andExpect(status().isFound())
				.andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}")).andExpect(model().hasNoErrors());

		ConceptNumeric concept = (ConceptNumeric) cs.getConcept(5089);
		Assertions.assertEquals(EXPECTED_LOW_NORMAL, concept.getLowNormal());
		Assertions.assertEquals(EXPECTED_HI_NORMAL, concept.getHiNormal());
		Assertions.assertEquals(EXPECTED_LOW_ABSOLUTE, concept.getLowAbsolute());
		Assertions.assertEquals(EXPECTED_HI_ABSOLUTE, concept.getHiAbsolute());
		Assertions.assertEquals(EXPECTED_LOW_CRITICAL, concept.getLowCritical());
		Assertions.assertEquals(EXPECTED_HI_CRITICAL, concept.getHiCritical());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldDisplayNumericValuesFromTable() throws Exception {
		final Double EXPECTED_LOW_ABSOLUTE = 0.0;
		final Double EXPECTED_LOW_CRITICAL = 99.0;
		final Double EXPECTED_LOW_NORMAL = 445.0;
		final Double EXPECTED_HI_NORMAL = 1497.0;
		final Double EXPECTED_HI_CRITICAL = 1800.0;
		final Double EXPECTED_HI_ABSOLUTE = 2500.0;

		MockHttpServletRequest mockRequest = new MockHttpServletRequest();

		mockRequest.setMethod("GET");
		mockRequest.setParameter("conceptId", "5497");

		ConceptFormBackingObject obj = (ConceptFormBackingObject) this.controller.formBackingObject(mockRequest);

		this.mockMvc.perform(get("/dictionary/concept.form").param("conceptId", "5497")).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());

		Assertions.assertEquals(EXPECTED_LOW_NORMAL, obj.getLowNormal());
		Assertions.assertEquals(EXPECTED_HI_NORMAL, obj.getHiNormal());
		Assertions.assertEquals(EXPECTED_LOW_ABSOLUTE, obj.getLowAbsolute());
		Assertions.assertEquals(EXPECTED_HI_ABSOLUTE, obj.getHiAbsolute());
		Assertions.assertEquals(EXPECTED_LOW_CRITICAL, obj.getLowCritical());
		Assertions.assertEquals(EXPECTED_HI_CRITICAL, obj.getHiCritical());
	}

	/**
	 * This tests removing a concept set
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldRemoveConceptSet() throws Exception {
		ConceptService cs = Context.getConceptService();

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", "23")
						.param("namesByLocale[en_GB].name", "FOOD CONSTRUCT").param("concept.datatype", "4")
						.param("concept.class", "10").param("concept.conceptSets", "18 19"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept concept = cs.getConcept(23);
		assertNotNull(concept);
		assertEquals(2, concept.getConceptSets().size());
	}

	/**
	 * This tests removing an answer
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldRemoveConceptAnswer() throws Exception {
		ConceptService cs = Context.getConceptService();

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", "21")
						.param("namesByLocale[en_GB].name", "FOOD ASSISTANCE FOR ENTIRE FAMILY")
						.param("concept.datatype", "2").param("concept.class", "7").param("concept.answers", "7 8"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept concept = cs.getConcept(21);
		assertNotNull(concept);
		assertEquals(2, concept.getAnswers(false).size());
	}

	/**
	 * This test makes sure that all answers are deleted if the user changes this
	 * concept's datatype to something other than "Coded"
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldRemoveConceptAnswersIfDatatypeChangedFromCoded() throws Exception {
		ConceptService cs = Context.getConceptService();

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", "4")
						.param("namesByLocale[en_GB].name", "CIVIL STATUS").param("concept.datatype", "1")
						.param("concept.class", "10").param("concept.answers", "5 6"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept concept = cs.getConcept(4);
		assertNotNull(concept);
		assertEquals(0, concept.getAnswers(false).size());
	}

	/**
	 * This test makes sure that ConceptComplex objects can be edited
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldEditConceptComplex() throws Exception {
		executeDataSet("org/openmrs/api/include/ObsServiceTest-complex.xml");

		ConceptService cs = Context.getConceptService();

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", "8473")
						.param("namesByLocale[en_GB].name", "A complex concept")
						.param("descriptionsByLocale[en_GB].description", "some description")
						.param("concept.datatype", "13").param("concept.class", "5").param("handlerKey", "TextHandler"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept concept = cs.getConcept(8473);
		assertEquals(ConceptComplex.class, concept.getClass());
		ConceptComplex complex = (ConceptComplex) concept;
		assertEquals("TextHandler", complex.getHandler());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldReturnAConceptWithANullIdIfNoMatchIsFound() throws Exception {

		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("GET");
		mockRequest.setParameter("conceptId", "57432223");

		ConceptFormBackingObject obj = (ConceptFormBackingObject) this.controller.formBackingObject(mockRequest);

		this.mockMvc.perform(get("/dictionary/concept.form").param("conceptId", "57432223")).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());

		assertNotNull(obj.getConcept());
		assertNull(obj.getConcept().getConceptId());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldSetTheLocalPreferredName() throws Exception {
		ConceptService cs = Context.getConceptService();
		Concept concept = cs.getConcept(5497);
		// sanity check, the current preferred Name should be different from what will
		// get set in the form
		Assertions.assertNotSame("CD3+CD4+ABS CNT", concept.getPreferredName(britishEn).getName());

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "Set LocalPreferredName")
						.param("conceptId", "5497").param("preferredNamesByLocale[en_GB]", "CD3+CD4+ABS CNT"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Assertions.assertEquals("CD3+CD4+ABS CNT", concept.getPreferredName(britishEn).getName());
		// preferred name should be the new one that has been set from the form
		Assertions.assertEquals(true, concept.getPreferredName(britishEn).isLocalePreferred());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldVoidASynonymMarkedAsPreferredWhenItIsRemoved() throws Exception {
		ConceptService cs = Context.getConceptService();
		Concept concept = cs.getConcept(5497);
		// mark one of the synonyms as preferred
		ConceptName preferredName = new ConceptName("pref name", britishEn);
		preferredName.setLocalePreferred(true);
		concept.addName(preferredName);
		cs.saveConcept(concept);

		this.mockMvc.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", "5497")
				// remove the synonym that is marked as preferred
				.param("synonymsByLocale[en_GB][0].voided", "true")).andExpect(status().isFound())
				.andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}")).andExpect(model().hasNoErrors());

		Assertions.assertEquals(true, preferredName.isVoided());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldAddANewConceptMapToAnExistingConcept() throws Exception {
		ConceptService cs = Context.getConceptService();
		int conceptId = 3;

		// make sure the concept already exists
		Concept concept = cs.getConcept(conceptId);
		assertNotNull(concept);
		int initialConceptMappingCount = concept.getConceptMappings().size();

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("conceptId", concept.getConceptId().toString())
						.param("conceptMappings[0].conceptReferenceTerm", "1")
						.param("conceptMappings[0].conceptMapType", "3"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		assertEquals(initialConceptMappingCount + 1, cs.getConcept(conceptId).getConceptMappings().size());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldAddANewConceptMapWhenCreatingAConcept() throws Exception {
		ConceptService cs = Context.getConceptService();
		final String conceptName = "new concept";
		// make sure the concept doesn't already exist
		Concept newConcept = cs.getConceptByName(conceptName);
		assertNull(newConcept);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("namesByLocale[en_GB].name", conceptName)
						.param("descriptionsByLocale[en_GB].description", "some description")
						.param("concept.datatype", "1").param("conceptMappings[0].conceptReferenceTerm", "1")
						.param("conceptMappings[0].conceptMapType", "3").param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept createdConcept = cs.getConceptByName(conceptName);
		assertNotNull(createdConcept);
		Assertions.assertEquals(1, createdConcept.getConceptMappings().size());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldIgnoreNewConceptMapRowIfTheUserDidNotSelectATerm() throws Exception {
		ConceptService cs = Context.getConceptService();
		int conceptId = 3;

		// make sure the concept already exists
		Concept concept = cs.getConcept(conceptId);
		assertNotNull(concept);
		int initialConceptMappingCount = concept.getConceptMappings().size();

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("conceptId", concept.getConceptId().toString())
						.param("conceptMappings[0].conceptReferenceTerm", "")
						.param("conceptMappings[0].conceptMapType", ""))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		assertEquals(initialConceptMappingCount, cs.getConcept(conceptId).getConceptMappings().size());
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldRemoveAConceptMapFromAnExistingConcept() throws Exception {
		ConceptService cs = Context.getConceptService();
		int conceptId = 5089;

		// make sure the concept already exists and has some concept mappings
		Concept concept = cs.getConcept(conceptId);
		assertNotNull(concept);
		Collection<ConceptMap> maps = concept.getConceptMappings();
		int initialConceptMappingCount = maps.size();
		assertTrue(initialConceptMappingCount > 0);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("conceptId", concept.getConceptId().toString())
						// remove the first row
						.param("conceptMappings[0].conceptReferenceTerm", ""))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		assertEquals(initialConceptMappingCount - 1, cs.getConcept(conceptId).getConceptMappings().size());
	}

	/**
	 * @see ConceptFormController#validateConceptUsesPersistedObjects(Concept,BindException)
	 * @verifies add error if map type is not saved
	 */
	@Test
	public void validateConceptReferenceTermUsesPersistedObjects_shouldAddErrorIfMapTypeIsNotSaved() throws Exception {
		Concept concept = new Concept();
		ConceptReferenceTerm term = new ConceptReferenceTerm();
		term.setName("name");
		term.setCode("code");
		term.setConceptSource(new ConceptSource(1));
		term.addConceptReferenceTermMap(new ConceptReferenceTermMap(new ConceptReferenceTerm(1), new ConceptMapType()));
		concept.addConceptMapping(new ConceptMap(term, new ConceptMapType(1)));
		BindException errors = new BindException(concept, "concept");
		new ConceptFormController().validateConceptUsesPersistedObjects(concept, errors);
		Assertions.assertEquals(1, errors.getErrorCount());
		Assertions.assertEquals(true, errors
				.hasFieldErrors("conceptMappings[0].conceptReferenceTerm.conceptReferenceTermMaps[0].conceptMapType"));
	}

	/**
	 * @see ConceptFormController#validateConceptUsesPersistedObjects(Concept,BindException)
	 * @verifies add error if source is not saved
	 */
	@Test
	public void validateConceptReferenceTermUsesPersistedObjects_shouldAddErrorIfSourceIsNotSaved() throws Exception {
		Concept concept = new Concept();
		ConceptReferenceTerm term = new ConceptReferenceTerm();
		term.setName("name");
		term.setCode("code");
		term.setConceptSource(new ConceptSource());
		term.addConceptReferenceTermMap(
				new ConceptReferenceTermMap(new ConceptReferenceTerm(1), new ConceptMapType(1)));
		concept.addConceptMapping(new ConceptMap(term, new ConceptMapType(1)));
		BindException errors = new BindException(concept, "concept");
		new ConceptFormController().validateConceptUsesPersistedObjects(concept, errors);
		Assertions.assertEquals(1, errors.getErrorCount());
		Assertions.assertEquals(true, errors.hasFieldErrors("conceptMappings[0].conceptReferenceTerm.conceptSource"));
	}

	/**
	 * @see ConceptFormController#validateConceptUsesPersistedObjects(Concept,BindException)
	 * @verifies add error if term b is not saved
	 */
	@Test
	public void validateConceptReferenceTermUsesPersistedObjects_shouldAddErrorIfTermBIsNotSaved() throws Exception {
		Concept concept = new Concept();
		ConceptReferenceTerm term = new ConceptReferenceTerm();
		term.setName("name");
		term.setCode("code");
		term.setConceptSource(new ConceptSource(1));
		term.addConceptReferenceTermMap(new ConceptReferenceTermMap(new ConceptReferenceTerm(), new ConceptMapType(1)));
		concept.addConceptMapping(new ConceptMap(term, new ConceptMapType(1)));
		BindException errors = new BindException(concept, "concept");
		new ConceptFormController().validateConceptUsesPersistedObjects(concept, errors);
		Assertions.assertEquals(1, errors.getErrorCount());
		Assertions.assertEquals(true,
				errors.hasFieldErrors("conceptMappings[0].conceptReferenceTerm.conceptReferenceTermMaps[0].termB"));
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies not save changes if there are validation errors
	 */
	@Test
	public void onSubmit_shouldNotSaveChangesIfThereAreValidationErrors() throws Exception {
		Integer conceptId = 792;

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dictionary/concept.form");
		request.setParameter("conceptId", conceptId.toString());
		request.setParameter("namesByLocale[en_GB].name", "should not change");
		request.setParameter("preferredNamesByLocale[en_GB]", "should not change");
		request.setParameter("synonymsByLocale[en_GB][1].name", ""); // empty name is invalid
		request.setParameter("synonymsByLocale[en_GB][1].voided", "false");

		Response response = webTestHelper.handle(request);
		assertThat(response.getErrors().hasFieldErrors("synonymsByLocale[en_GB][1].name"), is(true));

		Context.clearSession();

		Concept concept = conceptService.getConcept(conceptId);
		assertThat(concept.getPreferredName(britishEn).getName(), is("STAVUDINE LAMIVUDINE AND NEVIRAPINE"));
	}

	@Test
	public void shouldRemoveConceptDescriptionIfRemovedFromUI() throws Exception {
		ConceptService cs = Context.getConceptService();
		final String espaniol = "es";
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(OpenmrsConstants.GLOBAL_PROPERTY_LOCALE_ALLOWED_LIST);
		gp.setPropertyValue("en_GB, " + espaniol);
		as.saveGlobalProperty(gp);
		// make sure the concept already exists
		Concept concept = cs.getConcept(3);
		assertNotNull(concept);
		Locale spanish = LocaleUtility.fromSpecification(espaniol);
		assertNotNull(concept.getDescription(britishEn, true));
		assertNull(concept.getDescription(spanish, true));

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("conceptId", concept.getConceptId().toString())
						.param("descriptionsByLocale[en_GB].description", "")
						.param("descriptionsByLocale[es].description", "new spanish description"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConcept(3);
		assertNotNull(actualConcept);
		assertNull(concept.getDescription(britishEn, true));
		assertNotNull(concept.getDescription(spanish, true));
	}

	/**
	 * @see ConceptFormBackingObject#getConceptFromFormData()
	 */
	@Test
	public void getConceptFromFormData_shouldSetConceptOnConceptAnswers() throws Exception {
		int conceptId = 21;

		Concept concept = conceptService.getConcept(conceptId);
		assertNotNull(concept);

		int initialCount = concept.getAnswers().size();

		MockHttpServletRequest mockRequest = new MockHttpServletRequest();

		mockRequest.setMethod("POST");
		mockRequest.setParameter("action", "Save Concept");
		mockRequest.setParameter("conceptId", "21");
		mockRequest.setParameter("namesByLocale[en].name", concept.getName().getName());
		mockRequest.setParameter("concept.datatype", "2");
		mockRequest.setParameter("concept.answers", "7 8 22 5089");

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "Save Concept").param("conceptId", "21")
						.param("namesByLocale[en].name", concept.getName().getName()).param("concept.datatype", "2")
						.param("concept.answers", "7 8 22 5089"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		ConceptFormBackingObject cb = (ConceptFormBackingObject) this.controller.formBackingObject(mockRequest);

		Concept parsedConcept = cb.getConceptFromFormData();

		assertEquals(initialCount + 1, parsedConcept.getAnswers().size());
		for (ConceptAnswer ca : parsedConcept.getAnswers()) {
			assertNotNull(ca.getConcept());
		}
	}

	/**
	 * @see ConceptFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies edit short name when there are multiple allowed locales
	 */
	@Test
	public void onSubmit_shouldEditShortNameWhenThereAreMultipleAllowedLocales() throws Exception {
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(OpenmrsConstants.GLOBAL_PROPERTY_LOCALE_ALLOWED_LIST);
		gp.setPropertyValue(britishEn + ", en_US");
		as.saveGlobalProperty(gp);

		final Integer conceptId = 5089;
		Concept concept = conceptService.getConcept(conceptId);
		assertEquals("WT", concept.getShortNameInLocale(britishEn).getName());

		final String newShortName = "WGT";

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "Save Concept")
						.param("conceptId", conceptId.toString())
						.param("shortNamesByLocale[" + britishEn + "].name", newShortName)
						.param("shortNamesByLocale[en_US].name", ""))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		concept = conceptService.getConcept(conceptId);
		ConceptName shortConceptName = concept.getShortNameInLocale(britishEn);
		assertNotNull(shortConceptName);
		assertEquals(newShortName, shortConceptName.getName());
	}

	/**
	 * @verifies should add new concept attributes
	 * @throws Exception
	 */
	@Test
	public void shouldSaveConceptAttribute() throws Exception {
		executeDataSet(CONCEPT_ATTRIBUTES_XML);
		ConceptService cs = Context.getConceptService();
		ConceptAttributeType conceptAttributeType = cs.getConceptAttributeType(1);

		final Integer conceptId = 5089;

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", conceptId.toString())
						.param("attribute." + conceptAttributeType.getId() + ".new[1]", "2014-03-12"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConcept(conceptId);
		assertNotNull(actualConcept);
		final Collection<ConceptAttribute> attributes = actualConcept.getAttributes();
		assertEquals(1, attributes.size());
		final ConceptAttribute conceptAttribute = attributes.iterator().next();
		assertEquals("2014-03-12", conceptAttribute.getValueReference());
	}

	/**
	 * @verifies should add new concept attributes on creating concept
	 * @throws Exception
	 */
	@Test
	public void shouldSaveConceptAttributeOnCreatingConcept() throws Exception {
		executeDataSet(CONCEPT_ATTRIBUTES_XML);
		final String EXPECTED_PREFERRED_NAME = "concept with attribute";

		ConceptService cs = Context.getConceptService();

		// make sure the concept doesn't already exist
		Concept conceptToAdd = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNull(conceptToAdd);

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "")
						.param("namesByLocale[en_GB].name", EXPECTED_PREFERRED_NAME)
						.param("descriptionsByLocale[en_GB].description", "some description")
						.param("concept.datatype", "1").param("attribute.1.new[0]", "2011-04-25")
						.param("concept.conceptClass", "1"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Concept actualConcept = cs.getConceptByName(EXPECTED_PREFERRED_NAME);
		assertNotNull(actualConcept);
		final Collection<ConceptAttribute> attributes = actualConcept.getAttributes();
		assertEquals(1, attributes.size());
		final ConceptAttribute conceptAttribute = attributes.iterator().next();
		assertEquals("2011-04-25", conceptAttribute.getValueReference());
	}

	/**
	 * @verifies not void or change attributeList if the attribute values are same
	 */
	@Test
	public void shouldNotVoidOrChangeAttributeListIfTheAttributeValuesAreSame() throws Exception {
		executeDataSet(CONCEPT_ATTRIBUTES_XML);
		Concept concept = Context.getConceptService().getConcept(3);
		final int existingConceptAttributeId = 1;
		ConceptAttributeType conceptAttributeType = Context.getConceptService()
				.getConceptAttributeType(existingConceptAttributeId);
		conceptAttributeType.setName("concept joined date");

		// assert there is one concept attribute
		assertEquals(1, concept.getAttributes().size());
		assertEquals("2011-04-25", concept.getAttributes().iterator().next().getValueReference());

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", "3")
						.param("attribute." + conceptAttributeType.getId() + ".existing[" + existingConceptAttributeId
								+ "]", "2011-04-25"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Assertions.assertEquals(1, concept.getAttributes().size());
		Assertions.assertFalse(((ConceptAttribute) (concept.getAttributes().toArray()[0])).getVoided());
	}

	/**
	 * @verifies set attributes to void if the values is not set
	 */
	@Test
	public void shouldSetAttributesToVoidIfTheValueIsNotSet() throws Exception {
		executeDataSet(CONCEPT_ATTRIBUTES_XML);
		Concept concept = Context.getConceptService().getConcept(3);
		final int existingConceptAttributeId = 1;
		ConceptAttributeType conceptAttributeType = Context.getConceptService()
				.getConceptAttributeType(existingConceptAttributeId);
		conceptAttributeType.setName("concept type");

		this.mockMvc
				.perform(post("/dictionary/concept.form").param("action", "").param("conceptId", "3")
						.param("attribute." + conceptAttributeType.getId() + ".existing[" + existingConceptAttributeId
								+ "]", ""))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept.form?conceptId={\\d*}"))
				.andExpect(model().hasNoErrors());

		Assertions.assertEquals(1, concept.getAttributes().size());
		Assertions.assertTrue(((ConceptAttribute) (concept.getAttributes().toArray()[0])).getVoided());
	}
}
