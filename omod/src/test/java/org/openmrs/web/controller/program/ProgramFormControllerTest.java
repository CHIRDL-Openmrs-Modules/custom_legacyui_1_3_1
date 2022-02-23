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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

/**
 * Tests the {@link ProgramFormController} class.
 */
public class ProgramFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see ProgramFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	@Transactional(readOnly = true)
	public void onSubmit_shouldSaveWorkflowsWithProgram() throws Exception {
		
		// sanity check to make sure that program #3 doesn't have any workflows already:
	    Assertions.assertEquals(0, Context.getProgramWorkflowService().getProgram(3).getAllWorkflows().size());
		
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.setParameter("programId", "3");
		request.setParameter("allWorkflows", ":3"); // set one workflow on this program
		
		ProgramFormController controller = (ProgramFormController) applicationContext.getBean("programForm");
		controller.handleRequest(request, new MockHttpServletResponse());
		
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
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.setParameter("programId", "3");
		request.setParameter("allWorkflows", ":3 4"); // set two workflows on this program
		
		ProgramFormController controller = (ProgramFormController) applicationContext.getBean("programForm");
		controller.handleRequest(request, new MockHttpServletResponse());
		
		Assertions.assertEquals(2, Context.getProgramWorkflowService().getProgram(3).getWorkflows().size());
		
		request = new MockHttpServletRequest("POST", "");
		request.setParameter("programId", "3");
		request.setParameter("allWorkflows", ":5"); // set one workflow on this program
		
		controller.handleRequest(request, new MockHttpServletResponse());
		
		Assertions.assertEquals(1, Context.getProgramWorkflowService().getProgram(3).getWorkflows().size());
		Assertions.assertEquals(5, Context.getProgramWorkflowService().getProgram(3).getWorkflows().iterator().next()
		        .getConcept().getConceptId().intValue());
	}
}
