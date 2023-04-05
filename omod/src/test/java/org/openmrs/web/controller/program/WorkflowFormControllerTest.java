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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.ProgramWorkflow;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests the {@link WorkflowFormController} class.
 */
public class WorkflowFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	private WorkflowFormController controller;

	/**
	 * @see WorkflowFormController#formBackingObject(HttpServletRequest)
	 */
	@Test
	public void formBackingObject_shouldReturnValidProgramWorkflowGivenValidProgramIdAndWorkflowId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setParameter("programId", "1");
		request.setParameter("programWorkflowId", "1");

		ProgramWorkflow command = (ProgramWorkflow) this.controller.formBackingObject(request);
		Assertions.assertNotNull(command.getProgramWorkflowId());
	}

}
