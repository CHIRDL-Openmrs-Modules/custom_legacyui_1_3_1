/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.patient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tests the {@link PatientIdentifierTypeFormController}
 */
public class PatientIdentifierTypeFormControllerTest extends BaseModuleWebContextSensitiveTest {

	private MockHttpServletRequest request;

	@Autowired
	private PatientIdentifierTypeFormController controller;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		executeDataSet("org/openmrs/web/patient/include/PatientIdentifierTypeFormControllerTest.xml");

		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();

		this.request = new MockHttpServletRequest("POST",
				"/admin/patients/patientIdentifierType.form?patientIdentifierTypeId=1");
		this.request.setSession(new MockHttpSession(null));
		this.request.setContentType("application/x-www-form-urlencoded");
		this.request.addParameter("name", "TRUNK");

	}

	@Test
	public void shouldNotSavePatientIdentifierTypeWhenPatientIdentifierTypesAreLocked() throws Exception {

		this.request.addParameter("save", "Save Identifier Type");

		this.mockMvc.perform(post("/admin/patients/patientIdentifierType.form?patientIdentifierTypeId=1")
				.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
				.param("name", "TRUNK").param("save", "Save Identifier Type")).andExpect(status().isOk());

		PatientIdentifierType identifierType = new PatientIdentifierType();
		BindingResult errors = new BindException(identifierType, "Test idType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, identifierType, errors, map);
		Assertions.assertEquals("/module/legacyui/admin/patients/patientIdentifierTypeForm", mav.getViewName(),
				"The save attempt should have failed!");
		Assertions.assertNotEquals("patientIdentifierType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}

	@Test
	public void shouldNotRetirePatientIdentifierTypeWhenPatientIdentifierTypesAreLocked() throws Exception {
		this.request.addParameter("retire", "Retire Identifier Type");
		this.request.addParameter("retireReason", "Same reason");

		this.mockMvc.perform(post("/admin/patients/patientIdentifierType.form?patientIdentifierTypeId=1")
				.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
				.param("name", "TRUNK").param("retire", "Retire Identifier Type").param("retireReason", "Same reason"))
				.andExpect(status().isOk());

		PatientIdentifierType identifierType = new PatientIdentifierType();
		BindingResult errors = new BindException(identifierType, "Test idType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, identifierType, errors, map);

		Assertions.assertEquals("/module/legacyui/admin/patients/patientIdentifierTypeForm", mav.getViewName(),
				"The retire attempt should have failed!");
		Assertions.assertNotEquals("patientIdentifierType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}

	@Test
	public void shouldNotUnretirePatientIdentifierTypeWhenPatientIdentifierTypesAreLocked() throws Exception {
		this.request.addParameter("unretire", "Unretire Identifier Type");
		this.mockMvc
				.perform(post("/admin/patients/patientIdentifierType.form?patientIdentifierTypeId=1")
						.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
						.param("name", "TRUNK").param("unretire", "Unretire Identifier Type"))
				.andExpect(status().isOk());

		PatientIdentifierType identifierType = new PatientIdentifierType();
		BindingResult errors = new BindException(identifierType, "Test idType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, identifierType, errors, map);
		Assertions.assertEquals("/module/legacyui/admin/patients/patientIdentifierTypeForm", mav.getViewName(),
				"The unretire attempt should have failed!");
		Assertions.assertNotEquals("patientIdentifierType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}

	@Test
	public void shouldNotDeletePatientIdentifierTypeWhenPatientIdentifierTypesAreLocked() throws Exception {
		this.request.addParameter("purge", "Delete Identifier Type");

		this.mockMvc.perform(post("/admin/patients/patientIdentifierType.form?patientIdentifierTypeId=1")
				.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
				.param("name", "TRUNK").param("purge", "Delete Identifier Type")).andExpect(status().isOk());

		PatientIdentifierType identifierType = new PatientIdentifierType();
		BindingResult errors = new BindException(identifierType, "Test idType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, identifierType, errors, map);

		Assertions.assertEquals("/module/legacyui/admin/patients/patientIdentifierTypeForm", mav.getViewName(),
				"The delete attempt should have failed!");
		Assertions.assertNotEquals("patientIdentifierType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}

}
