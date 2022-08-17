/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.patient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.web.WebConstants;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Consists of unit tests for the PatientFormController
 *
 * @see PatientFormController
 */
public class PatientFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	PatientFormController controller;

	/**
	 * @see PatientFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, Object,
	 *      org.springframework.validation.BindException)
	 */
	@Test
	public void onSubmit_shouldVoidPatientWhenVoidReasonIsNotEmpty() throws Exception {

		Patient p = Context.getPatientService().getPatient(2);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.setParameter("action", "Patient.void");
		request.setParameter("voidReason", "some reason");

		BindingResult errors = new BindException(p, "patient");
		ModelAndView modelAndview = this.controller.processFormSubmission(request, p, errors);

		Assertions.assertTrue(p.isVoided());
	}

	/**
	 * @see PatientFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, Object,
	 *      org.springframework.validation.BindException)
	 */
	@Test
	public void onSubmit_shouldNotVoidPatientWhenVoidReasonIsEmpty() throws Exception {
		Patient p = Context.getPatientService().getPatient(2);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.setParameter("action", "Patient.void");
		request.setParameter("voidReason", "");

		BindingResult errors = new BindException(p, "patient");
		ModelAndView modelAndview = this.controller.processFormSubmission(request, p, errors);

		Assertions.assertTrue(!p.isVoided());
		String tmp = request.getSession().getAttribute(WebConstants.OPENMRS_ERROR_ATTR).toString();
		Assertions.assertEquals(tmp, "Patient.error.void.reasonEmpty");
	}
}
