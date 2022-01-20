/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.openmrs.web.user.UserProperties;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * Test the different aspects of
 * {@link org.openmrs.web.controller.user.ChangePasswordFormController}
 */
public class ChangePasswordFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	private final String oldPassword = "test";
	
	/**
	 * @see ChangePasswordFormController#formBackingObject()
	 */
	@Test
	public void formBackingObject_shouldReturnAuthenticatedUser() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		User user = controller.formBackingObject();
		assertNotNull(user);
		assertEquals(Context.getAuthenticatedUser(), user);
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldDisplayErrorMessageWhenPasswordAndConfirmPasswordAreNotSame() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "password", "differentPassword", "", "", "",
		    Context.getAuthenticatedUser(), errors);
		
		assertTrue(errors.hasErrors());
		assertEquals("error.password.match", errors.getGlobalError().getCode());
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldRedirectToIndexPageWhenPasswordAndConfirmPasswordAreTheSame() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Passw0rd", "", "", "", Context
		        .getAuthenticatedUser(), errors);
		
		assertTrue(!errors.hasErrors());
		assertEquals("redirect:/index.htm", result);
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 *           test =
	 */
	@Test
	public void handleSubmission_shouldDisplayErrorMessageWhenPasswordIsEmpty() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "", "", "", "", "", Context
		        .getAuthenticatedUser(), errors);
		
		assertTrue(errors.hasErrors());
		assertEquals("error.password.weak", errors.getGlobalError().getCode());
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldDiplayErrorMessageOnWeakPasswords() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "password", "password", "", "", "", Context
		        .getAuthenticatedUser(), errors);
		
		assertTrue(errors.hasErrors());
		assertEquals("error.password.requireMixedCase", errors.getGlobalError().getCode());
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldDiplayErrorMessageIfQuestionIsEmptyAndAnswerIsNotEmpty() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Passw0rd", "", "answer", "answer",
		    Context.getAuthenticatedUser(), errors);
		
		assertTrue(errors.hasErrors());
		assertEquals("auth.question.empty", errors.getGlobalError().getCode());
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldDiplayErrorMessageIfAnswerAndConfirmAnswerAreNotTheSame() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Passw0rd", "question", "answer",
		    "confirmanswer", Context.getAuthenticatedUser(), errors);
		
		assertTrue(errors.hasErrors());
		assertEquals("error.options.secretAnswer.match", errors.getGlobalError().getCode());
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldDisplayErrorMessageIfQuestionIsNotEmptyAndAnswerIsEmpty() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Passw0rd", "question", "", "",
		    Context.getAuthenticatedUser(), errors);
		
		assertTrue(errors.hasErrors());
		assertEquals("auth.question.fill", errors.getGlobalError().getCode());
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldProceedToHomePageIfOperationIsSuccesful() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Passw0rd", "question", "answer",
		    "answer", Context.getAuthenticatedUser(), errors);
		
		assertTrue(!errors.hasErrors());
		assertEquals("redirect:/index.htm", result);
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldChangeTheUserPropertyForcePasswordChangeToFalse() throws Exception {
		User user = Context.getAuthenticatedUser();
		new UserProperties(user.getUserProperties()).setSupposedToChangePassword(true);
		
		UserService us = Context.getUserService();
		us.saveUser(user);
		
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Passw0rd", "", "", "", Context
		        .getAuthenticatedUser(), errors);
		
		User modifiedUser = us.getUser(user.getId());
		assertTrue(!new UserProperties(modifiedUser.getUserProperties()).isSupposedToChangePassword());
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldNotChangeTheUserPropertyForcePasswordChangeToFalse() throws Exception {
		User user = Context.getAuthenticatedUser();
		new UserProperties(user.getUserProperties()).setSupposedToChangePassword(true);
		
		UserService us = Context.getUserService();
		us.saveUser(user);
		
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Pasw0rd", "", "", "",
		    Context.getAuthenticatedUser(), errors);
		
		User modifiedUser = us.getUser(user.getId());
		assertTrue(new UserProperties(modifiedUser.getUserProperties()).isSupposedToChangePassword());
	}
	
	/**
	 * @see ChangePasswordFormController#formBackingObject()
	 */
	@Test
	public void formBackingObject_shouldRemainOnChangePasswordFormPageIfThereAreErrors() throws Exception {
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		errors.addError(new ObjectError("Test", "Test Error"));
		String result = controller.handleSubmission(new MockHttpSession(), oldPassword, "password", "differentPassword", "", "", "",
		    Context.getAuthenticatedUser(), errors);
		assertEquals("/module/legacyui/admin/users/changePasswordForm", result);
	}
	
	/**
	 * @see ChangePasswordFormController#handleSubmission(HttpSession, String, String, String, String, String, User, BindingResult)
	 */
	@Test
	public void handleSubmission_shouldSetTheUserSecretQuestionAndAnswer() throws Exception {
		User user = Context.getAuthenticatedUser();
		new UserProperties(user.getUserProperties()).setSupposedToChangePassword(true);
		
		UserService us = Context.getUserService();
		us.saveUser(user);
		
		ChangePasswordFormController controller = new ChangePasswordFormController();
		BindException errors = new BindException(controller.formBackingObject(), "user");
		
		controller.handleSubmission(new MockHttpSession(), oldPassword, "Passw0rd", "Passw0rd", "test_question", "test_answer",
		    "test_answer", Context.getAuthenticatedUser(), errors);
		
		User modifiedUser = us.getUser(user.getId());
		
		assertTrue(us.isSecretAnswer(modifiedUser, "test_answer"));
	}
	
}
