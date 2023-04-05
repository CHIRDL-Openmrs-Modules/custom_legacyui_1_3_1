/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.maintenance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;

public class GlobalPropertyControllerTest extends BaseModuleWebContextSensitiveTest {
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private GlobalPropertyController controller;
	
	private AdministrationService administrationService;
	private MockMvc mockMvc;
	
	@BeforeEach
	public void before() {
		this.administrationService = Context.getAdministrationService();
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}
	
	/**
	 * @see GlobalPropertyController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies purge not included properties
	 */
	@Test
	public void onSubmit_shouldPurgeNotIncludedProperties() throws Exception {
		GlobalProperty gp = new GlobalProperty("test1", "test1_value");
		this.administrationService.saveGlobalProperty(gp);
		
		String[] keys = new String[] { "test2", "test3" };
		String[] values = new String[] { "test2_value", "test3_value" };
		String[] descriptions = new String[] { "", "" };
		
		this.mockMvc
		.perform(post("/admin/maintenance/globalProps.form").param("action", this.messageSource.getMessage("general.save", new Object[0], Locale.getDefault()))
				.param(GlobalPropertyController.PROP_NAME, keys)
				.param(GlobalPropertyController.PROP_VAL_NAME, values)
				.param(GlobalPropertyController.PROP_DESC_NAME, descriptions))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("globalProps.*"))
		.andExpect(model().hasNoErrors());
		
		Assertions.assertEquals(2, this.administrationService.getAllGlobalProperties().size());
		for (GlobalProperty globalProperty : this.administrationService.getAllGlobalProperties()) {
			if (globalProperty.getProperty().equals("test2")) {
			    Assertions.assertEquals("test2_value", globalProperty.getPropertyValue());
			} else if (globalProperty.getProperty().equals("test3")) {
			    Assertions.assertEquals("test3_value", globalProperty.getPropertyValue());
			} else {
			    Assertions.fail("Should be either test2 or test3");
			}
		}
	}
	
	/**
	 * @see GlobalPropertyController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies save or update included properties
	 */
	@Test
	public void onSubmit_shouldSaveOrUpdateIncludedProperties() throws Exception {
		GlobalProperty gp = new GlobalProperty("test1", "test1_value");
		this.administrationService.saveGlobalProperty(gp);
		
		String[] keys = new String[] { "test1", "test2" };
		String[] values = new String[] { "test1_new_value", "test2_value" };
		String[] descriptions = new String[] { "", "" };
		
		this.mockMvc
		.perform(post("/admin/maintenance/globalProps.form").param("action", this.messageSource.getMessage("general.save", new Object[0], Locale.getDefault()))
				.param(GlobalPropertyController.PROP_NAME, keys)
				.param(GlobalPropertyController.PROP_VAL_NAME, values)
				.param(GlobalPropertyController.PROP_DESC_NAME, descriptions))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("globalProps.*"))
		.andExpect(model().hasNoErrors());
	
		Assertions.assertEquals(2, this.administrationService.getAllGlobalProperties().size());
		for (GlobalProperty globalProperty : this.administrationService.getAllGlobalProperties()) {
			if (globalProperty.getProperty().equals("test1")) {
			    Assertions.assertEquals(globalProperty.getPropertyValue(), "test1_new_value");
			} else if (globalProperty.getProperty().equals("test2")) {
			    Assertions.assertEquals("test2_value", globalProperty.getPropertyValue());
			} else {
			    Assertions.fail("Should be either test1 or test2");
			}
		}
	}
}
