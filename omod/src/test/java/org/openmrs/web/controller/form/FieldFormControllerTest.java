/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.form;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Field;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link FieldFormController} class.
 */
public class FieldFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	FieldFormController controller;

	private MockMvc mockMvc;

	@BeforeEach
	public void runBeforeEachTest() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	/**
	 * @see FieldFormController#formBackingObject(HttpServletRequest)
	 */
	// @Transactional annotation needed because the parent class is @Transactional
	// and so screws propagates to this readOnly test
	@Transactional(readOnly = true)
	@Test
	public void formBackingObject_shouldGetField() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setParameter("fieldId", "1");

		this.mockMvc.perform(get("/admin/forms/field.form").param("fieldId", "1")).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());

		Field command = (Field) this.controller.formBackingObject(request);
		Assertions.assertNotNull(command.getFieldId());
	}

	/*
	*//**
		 * @see FieldFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
		 */

	@Test
	public void onSubmit_shouldNotFailOnFieldAnswers() throws Exception {
		final String FIELD_ID = "1";

		this.mockMvc.perform(get("/admin/forms/field.form").param("fieldId", FIELD_ID)).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());

		Context.closeSession();
		Context.openSession();
		authenticate();

		this.mockMvc
				.perform(post("/admin/forms/field.form").param("fieldId", FIELD_ID).param("name", "Some concept")
						.param("description", "This is a test field").param("fieldTypeId", "1")
						.param("name", "Some concept").param("conceptId", "3").param("action", "save"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("field.list?*"))
				.andExpect(model().hasNoErrors());

	}

	@Test
	public void onSubmit_shouldPurgeField() throws Exception {
		final String FIELD_ID = "1";

		this.mockMvc
				.perform(post("/admin/forms/field.form").param("fieldId", FIELD_ID).param("name", "Some concept")
						.param("description", "This is a test field").param("fieldTypeId", "1")
						.param("name", "Some concept").param("conceptId", "3")
						.param("action", Context.getMessageSourceService().getMessage("general.delete")))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("field.list?*"))
				.andExpect(model().hasNoErrors());

		Assertions.assertNull(Context.getFormService().getField(Integer.valueOf(FIELD_ID)));
	}

}
