/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.encounter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.EncounterType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.web.ShowFormUtil;
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

public class EncounterTypeFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	EncounterTypeFormController controller;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	@Test
	public void shouldNotDeleteEncounterTypeWhenEncounterTypesAreLocked() throws Exception {
		// dataset to lock encounter types
		executeDataSet("org/openmrs/web/encounter/include/EncounterTypeFormControllerTest.xml");

		EncounterService es = Context.getEncounterService();

		// setting up the request and doing an initial "get" equivalent to the user
		// loading the page for the first time

		MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/admin/encounters/encounterType.form?encounterTypeId=1");
		request.setSession(new MockHttpSession(null));

		EncounterType encounterType = new EncounterType();
		BindingResult errors = new BindException(encounterType, "Test Encounter");

		this.mockMvc.perform(get("/admin/encounters/encounterType.form?encounterTypeId=1")).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());

		ModelMap map = new ModelMap();
		this.controller.processSubmit(request, encounterType, errors, map);

		// set this to be a page submission
		this.mockMvc.perform(
				post("/admin/encounters/encounterType.form?encounterTypeId=1").param("action", "Delete EncounterType"))
				.andExpect(status().isFound()).andExpect(model().hasNoErrors());

		request.setMethod("POST");

		request.addParameter("action", "Delete EncounterType"); // so that the form is processed

		// send the parameters to the controller
		ModelAndView mav = ShowFormUtil.showForm(errors, "EncounterType.form");
		
		Assertions.assertEquals("EncounterType.form", mav.getViewName(), "The purge attempt should have failed!");
		Assertions.assertNotNull(es.getEncounterType(1));
	}

	@Test
	public void shouldSaveEncounterTypeWhenEncounterTypesAreNotLocked() throws Exception {
		EncounterService es = Context.getEncounterService();

		MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/admin/encounters/encounterType.form?encounterTypeId=1");
		request.setSession(new MockHttpSession(null));

		EncounterType encounterType = new EncounterType();
		BindingResult errors = new BindException(encounterType, "Test Encounter");

		this.mockMvc.perform(get("/admin/encounters/encounterType.form?encounterTypeId=1")).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());

		ModelMap map = new ModelMap();
		this.controller.processSubmit(request, encounterType, errors, map);

		request.setMethod("POST");

		request.addParameter("action", "Save EncounterType");

		this.mockMvc.perform(
				post("/admin/encounters/encounterType.form?encounterTypeId=1").param("action", "Save EncounterType"))
				.andExpect(status().isFound()).andExpect(model().hasNoErrors());

		ModelAndView mav = ShowFormUtil.showForm(errors, "index.htm");
		
		Assertions.assertNotEquals("The save attempt should have passed!", "index.htm", mav.getViewName());
		Assertions.assertNotNull(es.getEncounterType(1));
	}

}
