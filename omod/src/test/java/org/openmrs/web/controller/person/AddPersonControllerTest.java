/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.web.dwr.PersonListItem;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tests for the {@link AddPersonController} which handles the Add Person.form
 * page.
 */
public class AddPersonControllerTest extends BaseModuleWebContextSensitiveTest {

	private MockMvc mockMvc;

	@Autowired
	private AddPersonController controller;

	@BeforeEach
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	/**
	 * @see AddPersonController#formBackingObject(HttpServletRequest)
	 * @verifies catch an invalid birthdate
	 */
	@Test
	public void formBackingObject_shouldCatchAnInvalidBirthdate() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");

		request.setParameter("addName", "Gayan Perera");
		request.setParameter("addBirthdate", "03/07/199s");
		request.setParameter("addGender", "M");
		request.setParameter("personType", "patient");
		request.setParameter("viewType", "edit");

		List<PersonListItem> personListItem = (List<PersonListItem>) this.controller.formBackingObject(request);

		BindingResult errors = new BindException(personListItem, "Person");
		ModelAndView mav = this.controller.showForm(request, errors);

		assertNotNull(mav);
		assertEquals("Person.birthdate.required", mav.getModel().get("errorMessage"));
	}

	/**
	 * @see AddPersonController#formBackingObject(HttpServletRequest)
	 * @verifies catch pass for a valid birthdate
	 */
	@Test
	public void formBackingObject_shouldCatchPassForAValidBirthdate() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");

		request.setParameter("addName", "Gayan Perera");
		request.setParameter("addBirthdate", "03/07/1990");
		request.setParameter("addGender", "M");
		request.setParameter("personType", "patient");
		request.setParameter("viewType", "edit");

		List<PersonListItem> personListItem = (List<PersonListItem>) this.controller.formBackingObject(request);

		ModelAndView mav = this.controller.processSubmit(request);

		assertNotNull(mav);
		assertTrue(mav.getModel().isEmpty());
	}
}
