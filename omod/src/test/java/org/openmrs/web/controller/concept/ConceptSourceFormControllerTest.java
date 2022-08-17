/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.concept;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.BindException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests the {@link ConceptSourceFormController}
 */
public class ConceptSourceFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	private MockMvc mockMvc;
	
	@Autowired
	ConceptSourceFormController controller;
	
	/**
	 * @see ConceptSourceListController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldRetireConceptSource() throws Exception {
		
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
		ConceptService cs = Context.getConceptService();
				
		this.mockMvc
		.perform(post("/admin/concepts/conceptSource.form")
				.param("conceptSourceId", "3")
				.param("retireReason", "dummy reason for retirement")
				.param("retire", "dummy reason for retirement"))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept*"))
		.andExpect(model().hasNoErrors());
		
		ConceptSource conceptSource = cs.getConceptSource(3);
		Assertions.assertTrue(conceptSource.isRetired());
		Assertions.assertEquals("dummy reason for retirement", conceptSource.getRetireReason());
	}
	
	/**
	 * @see ConceptSourceListController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldDeleteConceptSource() throws Exception {
		
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
		ConceptService cs = Context.getConceptService();
			
		this.mockMvc
		.perform(post("/admin/concepts/conceptSource.form")
				.param("conceptSourceId", "3")
				.param("purge", "dummy reason for deletion"))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept*"))
		.andExpect(model().hasNoErrors());
		
		ConceptSource nullConceptSource = cs.getConceptSource(3);
		Assertions.assertNull(nullConceptSource);
	}
	
	/**
	 * @see ConceptSourceListController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldRestoreRetiredConceptSource() throws Exception {
		
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
		ConceptService cs = Context.getConceptService();
		
		this.mockMvc
		.perform(post("/admin/concepts/conceptSource.form")
				.param("conceptSourceId", "3")
				.param("retireReason", "dummy reason for retirement")
				.param("retire", "dummy reason for retirement"))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept*"))
		.andExpect(model().hasNoErrors());
		
		ConceptSource conceptSource = cs.getConceptSource(3);
		Assertions.assertTrue(conceptSource.isRetired());
		Assertions.assertEquals("dummy reason for retirement", conceptSource.getRetireReason());
		
		this.mockMvc
		.perform(post("/admin/concepts/conceptSource.form")
				.param("conceptSourceId", "3")
				.param("restore", "dummy reason for restoration"))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("concept*"))
		.andExpect(model().hasNoErrors());
		
		ConceptSource newConceptSource = cs.getConceptSource(3);
		Assertions.assertNotNull(newConceptSource, "Error, Object is null");
		Assertions.assertTrue(!newConceptSource.isRetired());
	}
}
