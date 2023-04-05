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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.PersonAttributeType;
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
 * Tests the {@link PersonAttributeTypeFormController}
 */
public class PersonAttributeTypeFormControllerTest extends BaseModuleWebContextSensitiveTest {

	private MockHttpServletRequest request;

	@Autowired
	private PersonAttributeTypeFormController controller;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		executeDataSet("org/openmrs/web/controller/include/PersonAttributeTypeFormControllerTest.xml");

		this.request = new MockHttpServletRequest("POST",
				"/admin/person/personAttributeType.form?personAttributeTypeId=1");
		this.request.setSession(new MockHttpSession(null));
		this.request.setContentType("application/x-www-form-urlencoded");
		this.request.addParameter("name", "TRUNK");
		this.request.addParameter("format", "java.lang.String");

		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	@Test
	public void shouldNotSavePersonAttributeTypeWhenPersonAttributeTypesAreLocked() throws Exception {
		this.request.addParameter("save", "Save Person Attribute Type");

		this.mockMvc
				.perform(post("/admin/person/personAttributeType.form?personAttributeTypeId=1")
						.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
						.param("name", "TRUNK").param("format", "java.lang.String")
						.param("save", "Save Person Attribute Type"))
				.andExpect(status().isOk());

		PersonAttributeType attrType = Context.getPersonService().getPersonAttributeType(1);
		BindingResult errors = new BindException(attrType, "Test attrType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, attrType, errors, map);

		Assertions.assertEquals("/module/legacyui/admin/person/personAttributeTypeForm", mav.getViewName(),
				"The save attempt should have failed!");
		Assertions.assertNotEquals("PersonAttributeType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}

	@Test
	public void shouldNotDeletePersonAttributeTypeWhenPersonAttributeTypesAreLocked() throws Exception {
		this.request.addParameter("purge", "Delete Person Attribute Type");

		this.mockMvc
				.perform(post("/admin/person/personAttributeType.form?personAttributeTypeId=1")
						.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
						.param("name", "TRUNK").param("format", "java.lang.String")
						.param("purge", "Delete Person Attribute Type"))
				.andExpect(status().isOk());

		PersonAttributeType attrType = Context.getPersonService().getPersonAttributeType(1);
		BindingResult errors = new BindException(attrType, "Test attrType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, attrType, errors, map);

		Assertions.assertEquals("/module/legacyui/admin/person/personAttributeTypeForm", mav.getViewName(),
				"The delete attempt should have failed!");
		Assertions.assertNotEquals("PersonAttributeType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}

	@Test
	public void shouldNotRetirePersonAttributeTypeWhenPersonAttributeTypesAreLocked() throws Exception {
		this.request.addParameter("retire", "Retire Person Attribute Type");
		this.request.addParameter("retireReason", "Same reason");

		this.mockMvc
				.perform(post("/admin/person/personAttributeType.form?personAttributeTypeId=1")
						.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
						.param("name", "TRUNK").param("format", "java.lang.String")
						.param("retire", "Retire Person Attribute Type").param("retireReason", "Same reason"))
				.andExpect(status().isOk());

		PersonAttributeType attrType = Context.getPersonService().getPersonAttributeType(1);
		BindingResult errors = new BindException(attrType, "Test attrType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, attrType, errors, map);

		Assertions.assertEquals("/module/legacyui/admin/person/personAttributeTypeForm", mav.getViewName(),
				"The retire attempt should have failed!");
		Assertions.assertNotEquals("PersonAttributeType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}

	@Test
	public void shouldNotUnretirePersonAttributeTypeWhenPersonAttributeTypesAreLocked() throws Exception {
		this.request.addParameter("unretire", "Unretire Person Attribute Type");

		this.mockMvc
				.perform(post("/admin/person/personAttributeType.form?personAttributeTypeId=1")
						.contentType("application/x-www-form-urlencoded").session(new MockHttpSession(null))
						.param("name", "TRUNK").param("format", "java.lang.String")
						.param("unretire", "Unretire Person Attribute Type"))
				.andExpect(status().isOk());

		PersonAttributeType attrType = Context.getPersonService().getPersonAttributeType(1);
		BindingResult errors = new BindException(attrType, "Test attrType");

		ModelMap map = new ModelMap();
		ModelAndView mav = this.controller.processSubmit(this.request, attrType, errors, map);

		Assertions.assertEquals("/module/legacyui/admin/person/personAttributeTypeForm", mav.getViewName(),
				"The unretire attempt should have failed!");
		Assertions.assertNotEquals("PersonAttributeType.form", mav.getViewName());
		Assertions.assertNotNull(Context.getPersonService().getPersonAttributeType(1));
	}
}
