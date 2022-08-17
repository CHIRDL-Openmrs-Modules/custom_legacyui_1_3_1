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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.LoginCredential;
import org.openmrs.api.db.UserDAO;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.Security;
import org.openmrs.web.OptionsForm;
import org.openmrs.web.test.WebTestHelper;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

public class OptionsFormControllerTest extends BaseModuleWebContextSensitiveTest {

	private User user;

	private UserDAO userDao;

	@Autowired
	private OptionsFormController controller;

	@Autowired
	private WebTestHelper testHelper;

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() {
		Context.authenticate("admin", "test");
		this.user = Context.getAuthenticatedUser();
		this.userDao = (UserDAO) this.applicationContext.getBean("userDAO");
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	@Test
	public void shouldChangeSecretQuestionAndAnswer() throws Exception {

		String answer = "test_answer";

		this.mockMvc
				.perform(post("/options.form").param("secretQuestionPassword", "test")
						.param("secretQuestionNew", "test_question").param("secretAnswerNew", answer)
						.param("secretAnswerConfirm", answer))
				.andExpect(status().isFound()).andExpect(model().hasNoErrors());

		LoginCredential loginCredential = userDao.getLoginCredential(user);
		assertEquals(Security.encodeString(answer + loginCredential.getSalt()), loginCredential.getSecretAnswer());
	}

	@Test
	public void shouldRejectEmptySecretAnswer() throws Exception {

		String emptyAnswer = "";

		this.mockMvc.perform(post("/options.form").param("secretQuestionPassword", "test")
				.param("secretQuestionNew", "test_question").param("secretAnswerNew", emptyAnswer)
				.param("secretAnswerConfirm", emptyAnswer)).andExpect(status().isOk());

		LoginCredential loginCredential = userDao.getLoginCredential(user);
		assertNull(loginCredential.getSecretAnswer());
	}

	@Test
	public void shouldRejectEmptySecretAnswerWhenSecretQuestionPasswordIsNotSet() throws Exception {

		String emptyAnswer = "";

		this.mockMvc
				.perform(post("/options.form").param("secretQuestionPassword", "")
						.param("secretQuestionNew", "test_question").param("secretAnswerNew", emptyAnswer)
						.param("secretAnswerConfirm", emptyAnswer))
				.andExpect(status().isOk());

		LoginCredential loginCredential = userDao.getLoginCredential(user);
		assertNull(loginCredential.getSecretAnswer());
	}

	@Test
	public void shouldRejectEmptySecretQuestion() throws Exception {
		LoginCredential loginCredential = userDao.getLoginCredential(user);
		String originalQuestion = loginCredential.getSecretQuestion();

		String emptyAnswer = "test_answer";

		this.mockMvc
				.perform(post("/options.form").param("secretQuestionPassword", "test").param("secretQuestionNew", "")
						.param("secretAnswerNew", emptyAnswer).param("secretAnswerConfirm", emptyAnswer))
				.andExpect(status().isOk());

		loginCredential = userDao.getLoginCredential(user);
		assertEquals(originalQuestion, loginCredential.getSecretQuestion());
	}

	/**
	 * @see OptionsFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies accept email address as username if enabled
	 */

	@Test
	public void onSubmit_shouldAcceptEmailAddressAsUsernameIfEnabled() throws Exception {
		// given
		Context.getAdministrationService().saveGlobalProperty(
				new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_USER_REQUIRE_EMAIL_AS_USERNAME, "true"));

		// when
		this.mockMvc.perform(post("/options.form").param("username", "ab@gmail.com")).andExpect(status().isOk());

		// then
		assertThat("ab@gmail.com", is(Context.getAuthenticatedUser().getUsername()));
	}

	/**
	 * @see OptionsFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies reject invalid email address as username if enabled
	 */

	@Test
	public void onSubmit_shouldRejectInvalidEmailAddressAsUsernameIfEnabled() throws Exception {
		// given
		Context.getAdministrationService().saveGlobalProperty(
				new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_USER_REQUIRE_EMAIL_AS_USERNAME, "true"));

		MockHttpServletRequest post = testHelper.newPOST("/options.form");
		post.addParameter("username", "ab@");

		// when
		this.testHelper.handle(post);

		// then
		assertThat("ab@", is(not(Context.getAuthenticatedUser().getUsername())));
	}

	/**
	 * @see OptionsFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies accept 2 characters as username
	 */

	@Test
	public void onSubmit_shouldAccept2CharactersAsUsername() throws Exception {
		// given
		MockHttpServletRequest post = testHelper.newPOST("/options.form");
		post.addParameter("username", "ab");

		// when
		this.testHelper.handle(post);

		// then
		assertThat("ab", is(Context.getAuthenticatedUser().getUsername()));
	}

	/**
	 * @see OptionsFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 * @verifies reject 1 character as username
	 */
	@Test
	public void onSubmit_shouldReject1CharacterAsUsername() throws Exception {
		// given
		MockHttpServletRequest post = testHelper.newPOST("/options.form");
		post.addParameter("username", "a");

		// when
		this.testHelper.handle(post);

		// then
		assertThat("a", is(not(Context.getAuthenticatedUser().getUsername())));
	}

	@Test
	public void shouldRejectInvalidNotificationAddress() throws Exception {
		final String incorrectAddress = "gayan@gmail";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");

		OptionsForm opts = new OptionsForm();
		opts.setNotification("email");
		opts.setNotificationAddress(incorrectAddress);
		BindingResult errors = new BindException(opts, "opts");
		ModelAndView modelAndView = this.controller.processSubmit(request, opts, errors);

		OptionsForm optionsForm = (OptionsForm) this.controller.formBackingObject();
		assertEquals(incorrectAddress, optionsForm.getNotificationAddress());

		BeanPropertyBindingResult bindingResult = (BeanPropertyBindingResult) modelAndView.getModel()
				.get("org.springframework.validation.BindingResult.opts");
		Assertions.assertTrue(bindingResult.hasErrors());
	}

	@Test
	public void shouldAcceptValidNotificationAddress() throws Exception {
		String notificationTypes[] = { "internal", "internalProtected", "email" };
		String correctAddress = "gayan@gmail.com";

		for (String notifyType : notificationTypes) {

			this.mockMvc.perform(post("/options.form").param("notification", notifyType).param("notificationAddress",
					correctAddress)).andExpect(status().isFound()).andExpect(model().hasNoErrors());

			OptionsForm optionsForm = (OptionsForm) this.controller.formBackingObject();
			assertEquals(correctAddress, optionsForm.getNotificationAddress());
		}
	}

	@Test
	public void shouldRejectEmptyNotificationAddress() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.setParameter("notification", "email");
		request.setParameter("notificationAddress", "");

		this.mockMvc.perform(post("/options.form").param("notification", "email").param("notificationAddress", ""))
				.andExpect(status().isOk());

		OptionsForm opts = new OptionsForm();
		BindingResult errors = new BindException(opts, "opts");
		ModelAndView modelAndView = this.controller.processSubmit(request, opts, errors);

		BeanPropertyBindingResult bindingResult = (BeanPropertyBindingResult) modelAndView.getModel()
				.get("org.springframework.validation.BindingResult.opts");
		Assertions.assertTrue(bindingResult.hasErrors());
	}

	@Test
	public void shouldNotOverwriteUserSecretQuestionOrAnswerWhenChangingPassword() throws Exception {
		LoginCredential loginCredential = this.userDao.getLoginCredential(user);

		this.mockMvc
				.perform(post("/options.form").param("secretQuestionPassword", "test")
						.param("secretQuestionNew", "easy question").param("secretAnswerNew", "easy answer")
						.param("secretAnswerConfirm", "easy answer"))
				.andExpect(status().isFound()).andExpect(model().hasNoErrors());

		Assertions.assertEquals("easy question", loginCredential.getSecretQuestion());
		String hashedAnswer = Security.encodeString("easy answer" + loginCredential.getSalt());
		Assertions.assertEquals(hashedAnswer, loginCredential.getSecretAnswer());
		String oldPassword = loginCredential.getHashedPassword();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.addParameter("secretQuestionNew", "easy question");
		request.setParameter("oldPassword", "test");
		request.setParameter("newPassword", "OpenMRS1");
		request.setParameter("confirmPassword", "OpenMRS1");

		this.mockMvc
				.perform(post("/options.form").param("secretQuestionNew", "easy question").param("oldPassword", "test")
						.param("newPassword", "OpenMRS1").param("confirmPassword", "OpenMRS1"))
				.andExpect(status().isOk());

		if (oldPassword == loginCredential.getHashedPassword()) {
			request.setParameter("secretQuestionNew", "");
			OptionsForm opts = new OptionsForm();
			BindingResult errors = new BindException(opts, "Test options form");
			this.controller.processSubmit(request, opts, errors);
		}
		Assertions.assertEquals(hashedAnswer, loginCredential.getSecretAnswer());
		Assertions.assertEquals("easy question", loginCredential.getSecretQuestion());
	}

}
