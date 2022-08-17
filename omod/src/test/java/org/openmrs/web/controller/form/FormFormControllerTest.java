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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Form;
import org.openmrs.api.FormService;
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
 * Tests the {@link FormFormController} class.
 */
public class FormFormControllerTest extends BaseModuleWebContextSensitiveTest {

	private FormService formService;
	private MockMvc mockMvc;

	@Autowired
	private FormFormController controller;

	@BeforeEach
	public void setup() throws Exception {
		if (this.formService == null) {
			this.formService = Context.getFormService();
		}
		// dataset to locks forms
		executeDataSet("org/openmrs/web/controller/include/FormFormControllerTest.xml");

		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	@Test
	public void shouldNotSaveAFormWhenFormsAreLocked() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/forms/formEdit.form?formId=1");
		request.setSession(new MockHttpSession(null));

		request.addParameter("name", "TRUNK");
		request.addParameter("version", "1");
		request.addParameter("action", "Form.save");
		request.setContentType("application/x-www-form-urlencoded");

		this.mockMvc
				.perform(post("/admin/forms/formEdit.form?formId=1").param("name", "TRUNK").param("version", "1")
						.param("action", "Form.save").contentType("application/x-www-form-urlencoded"))
				.andExpect(status().isOk());

		Form form = new Form();
		BindingResult errors = new BindException(form, "TestForm");
		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(request, form, errors, map);
		Assertions.assertEquals("/module/legacyui/admin/forms/formEditForm", mav.getViewName(), "The save attempt should have failed!");
		Assertions.assertNotEquals("formEdit.form", mav.getViewName());
		Assertions.assertNotNull(this.formService.getForm(1));
	}

	@Test
	public void shouldNotDuplicateAFormWhenFormsAreLocked() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST",
				"/admin/forms/formEdit.form?duplicate=true&formId=1");
		request.setSession(new MockHttpSession(null));
		Form form = new Form();
		BindingResult errors = new BindException(form, "TestForm");
		ModelMap map = new ModelMap();
		this.controller.processSubmit(request, form, errors, map);

		request.addParameter("name", "TRUNK");
		request.addParameter("version", "1");
		request.addParameter("action", "Form.Duplicate");
		request.setContentType("application/x-www-form-urlencoded");

		this.mockMvc
				.perform(post("/admin/forms/formEdit.form?formId=1").param("name", "TRUNK").param("version", "1")
						.param("action", "Form.Duplicate").contentType("application/x-www-form-urlencoded"))
				.andExpect(status().isOk());

		ModelAndView mav = this.controller.processSubmit(request, form, errors, map);
		Assertions.assertEquals("/module/legacyui/admin/forms/formEditForm", mav.getViewName(), "The duplicate attempt should have failed!");
		Assertions.assertNotEquals("formEdit.form", mav.getViewName());
		Assertions.assertNotNull(this.formService.getForm(1));
	}
}
