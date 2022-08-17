/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.program;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

/**
 * Tests the {@link ProgramFormController} class.
 */
public class ProgramFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	private ProgramFormController controller;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	/**
	 * @see ProgramFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	@Transactional(readOnly = true)
	public void onSubmit_shouldSaveWorkflowsWithProgram() throws Exception {

		// sanity check to make sure that program #3 doesn't have any workflows already:
		Assertions.assertEquals(0, Context.getProgramWorkflowService().getProgram(3).getAllWorkflows().size());

		this.mockMvc.perform(post("/admin/programs/program.form").param("programId", "3").param("allWorkflows", ":3"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("program.*"))
				.andExpect(model().hasNoErrors());

		Assertions.assertNotSame(0, Context.getProgramWorkflowService().getProgram(3).getAllWorkflows().size());
		Assertions.assertEquals(1, Context.getProgramWorkflowService().getProgram(3).getAllWorkflows().size());
	}

	/**
	 * @see ProgramFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies edit existing workflows within programs
	 */
	@Test
	@Transactional(readOnly = true)
	public void onSubmit_shouldEditExistingWorkflowsWithinPrograms() throws Exception {

		this.mockMvc.perform(post("/admin/programs/program.form").param("programId", "3").param("allWorkflows", ":3 4"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("program.*"))
				.andExpect(model().hasNoErrors());

		Assertions.assertEquals(2, Context.getProgramWorkflowService().getProgram(3).getWorkflows().size());

		this.mockMvc.perform(post("/admin/programs/program.form").param("programId", "3").param("allWorkflows", ":5"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("program.*"))
				.andExpect(model().hasNoErrors());

		Assertions.assertEquals(1, Context.getProgramWorkflowService().getProgram(3).getWorkflows().size());
		Assertions.assertEquals(5, Context.getProgramWorkflowService().getProgram(3).getWorkflows().iterator().next()
				.getConcept().getConceptId().intValue());
	}
}
