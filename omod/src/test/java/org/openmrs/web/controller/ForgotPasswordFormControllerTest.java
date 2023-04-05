/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.web.WebConstants;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Test the different aspects of
 * {@link org.openmrs.web.controller.ForgotPasswordFormController}
 */

public class ForgotPasswordFormControllerTest extends BaseModuleWebContextSensitiveTest {

	protected static final String TEST_DATA = "org/openmrs/web/controller/include/ForgotPasswordFormControllerTest.xml";

	@BeforeEach
	public void runBeforeEachTest() throws Exception {
		executeDataSet(TEST_DATA);
		Context.logout();
	}

	/**
	 * Check to see if the admin's secret question comes back
	 *
	 * @throws Exception
	 */

	@Test
	public void shouldSetARandomSecretQuestionWhenTheUsernameIsInvalid() throws Exception {

		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		Object obj = new Object();
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setParameter("uname", "invaliduser");
		request.setMethod("POST");

		BindingResult errors = new BindException(obj, "TestObj");

		controller.processSubmit(request, obj, errors);

		Assertions.assertEquals("invaliduser", request.getAttribute("uname"));

		List<String> questions = new ArrayList<>();

		questions.add(Context.getMessageSourceService().getMessage("What is your best friend's name?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your grandfather's home town?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your mother's maiden name?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your favorite band?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your first pet's name?"));
		questions.add(Context.getMessageSourceService().getMessage("What is your brother's middle name?"));
		questions.add(Context.getMessageSourceService().getMessage("Which city were you born in?"));

		// Check that one of the fake questions is assigned to the invalid username
		Assertions.assertTrue(questions.contains(request.getAttribute("secretQuestion")));
	}

	@Test
	public void shouldAcceptAsUserWithValidSecretQuestion() throws Exception {

		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		Object obj = new Object();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");

		request.addParameter("uname", "validuser");
		request.addParameter("secretAnswer", "valid secret Answer");

		BindingResult errors = new BindException(obj, "TestObj");

		ModelAndView mav = controller.processSubmit(request, obj, errors);
		Assertions.assertEquals("/options.form#Change Login Info", ((RedirectView) mav.getView()).getUrl());
		Assertions.assertEquals(2, Context.getAuthenticatedUser().getId().intValue());
	}

	/**
	 * If a user enters the wrong secret answer, they should be kicked back to the
	 * form and not be accepted even though the username is correct
	 *
	 * @throws Exception
	 */

	@Test
	public void shouldFailForAValidUsernameAndInvalidSecretQuestion() throws Exception {

		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		Object obj = new Object();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.addParameter("uname", "validuser");
		request.addParameter("secretAnswer", "invalid secret answer");

		BindingResult errors = new BindException(obj, "TestObj");
		controller.processSubmit(request, obj, errors);

		Assertions.assertEquals("valid secret question", request.getAttribute("secretQuestion"));
		Assertions.assertEquals("auth.answer.invalid",
				request.getSession().getAttribute(WebConstants.OPENMRS_ERROR_ATTR));
		Assertions.assertEquals("auth.question.fill", request.getSession().getAttribute(WebConstants.OPENMRS_MSG_ATTR));
		Assertions.assertFalse(Context.isAuthenticated());
	}

	/**
	 * If a user enters 5 requests, the 6th should fail even if that one has a valid
	 * username in it
	 *
	 * @throws Exception
	 */

	@Test
	public void shouldLockOutAfterFiveFailedInvalidUsernames() throws Exception {

		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		Object obj = new Object();
		for (int x = 1; x <= 5; x++) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setMethod("POST");

			request.addParameter("uname", "invaliduser");
			BindingResult errors = new BindException(obj, "TestObj");
			controller.processSubmit(request, obj, errors);
		}

		// those were the first five, now the sixth request (with a valid user)
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.addParameter("uname", "validuser");

		BindingResult errors = new BindException(obj, "TestObj");
		controller.processSubmit(request, obj, errors);

		Assertions.assertNull(request.getAttribute("secretQuestion"));
	}

	/**
	 * If a user enters 5 requests, the 6th should fail even if that one has a valid
	 * username and a secret answer associated with it
	 *
	 * @throws Exception
	 */

	@Test
	public void shouldNotAcceptAfterFiveFailedInvalidUsernames() throws Exception {

		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		Object obj = new Object();
		for (int x = 1; x <= 5; x++) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setMethod("POST");

			request.addParameter("uname", "invaliduser");
			BindingResult errors = new BindException(obj, "TestObj");
			controller.processSubmit(request, obj, errors);
		}
		// those were the first five, now the sixth request (with a valid user)
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("POST");

		mockRequest.addParameter("uname", "validuser");
		mockRequest.addParameter("secretAnswer", "valid secret answer");
		BindingResult bindErrors = new BindException(obj, "TestObject");
	    controller.processSubmit(mockRequest, obj, bindErrors);
		Assertions.assertFalse(Context.isAuthenticated());
	}

	/**
	 * If a user enters 5 requests with username+secret answer, the 6th should fail
	 * even if that one has a valid answer in it
	 *
	 * @throws Exception
	 */

	@Test
	public void shouldLockOutAfterFiveFailedInvalidSecretAnswers() throws Exception {

		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		Object obj = new Object();
		for (int x = 1; x <= 5; x++) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setMethod("POST");

			request.addParameter("uname", "validuser");
			request.addParameter("secretAnswer", "invalid secret answer");

			BindingResult errors = new BindException(obj, "TestObj");
			controller.processSubmit(request, obj, errors);
		}

		// those were the first five, now the sixth request (with a valid user)
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");

		request.addParameter("uname", "validuser");
		request.addParameter("secretAnswer", "valid secret answer");

		BindingResult errors = new BindException(obj, "TestObj");
		controller.processSubmit(request, obj, errors);

		Assertions.assertFalse(Context.isAuthenticated());
	}

	/**
	 * If a user enters 4 username requests, the 5th one should reset the lockout
	 * and they should be allowed 5 attempts at the secret answer
	 *
	 * @throws Exception
	 */

	@Test
	public void shouldGiveUserFiveSecretAnswerAttemptsAfterLessThanFiveFailedUsernameAttempts() throws Exception {

		Object obj = new Object();
		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		
		for (int x = 1; x <= 4; x++) {
			BindingResult errors = new BindException(obj, "TestObj");
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setMethod("POST");
			request.addParameter("uname", "invaliduser");
			controller.processSubmit(request, obj, errors);
		}

		BindingResult errors = new BindException(obj, "TestObj");
		MockHttpServletRequest request = new MockHttpServletRequest();
		// those were the first four, now the fifth is a valid username
		//MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.addParameter("uname", "validuser");

		controller.processSubmit(request, obj, errors);

		Assertions.assertNotNull(request.getAttribute("secretQuestion"));

		// now the user has 5 chances at the secret answer
		// fifth request
		MockHttpServletRequest request5 = new MockHttpServletRequest();
		request5.setMethod("POST");

		request5.addParameter("uname", "validuser");
		request5.addParameter("secretAnswer", "invalid answer");
		
		controller.processSubmit(request5, obj, errors);
		Assertions.assertNotNull(request5.getAttribute("secretQuestion"));

		// sixth request (should not lock out because is after valid username)
		MockHttpServletRequest request6 = new MockHttpServletRequest();
		request6.setMethod("POST");

		request6.addParameter("uname", "validuser");
		request6.addParameter("secretAnswer", "invalid answer");
		request.setMethod("POST");
		controller.processSubmit(request6, obj, errors);
		Assertions.assertNotNull(request6.getAttribute("secretQuestion"));

		// seventh request (should Accept with valid answer)
		MockHttpServletRequest request7 = new MockHttpServletRequest();
		request7.setMethod("POST");

		request7.addParameter("uname", "validuser");
		request7.addParameter("secretAnswer", "valid secret answer");
		controller.processSubmit(request7, obj, errors);

		Assertions.assertTrue(Context.isAuthenticated());
	}

	@Test
	public void shouldNotAcceptWithInvalidSecretQuestionIfUserIsNull() throws Exception {
		
		ForgotPasswordFormController controller = new ForgotPasswordFormController();
		Object obj = new Object();
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setMethod("POST");
		request.addParameter("uname", "");
		
		BindingResult errors = new BindException(obj, "TestObj");
		controller.processSubmit(request, obj, errors);
		Assertions.assertFalse(Context.isAuthenticated());
	}
}
