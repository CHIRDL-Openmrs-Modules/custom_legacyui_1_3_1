/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.form;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller used to merge duplicate fields
 * <p>
 * This class calls the FormService's mergeDuplicateFields
 */

@Controller
@RequestMapping(value = "admin/forms/auditField.form")
public class AuditFieldController {

	private static final String FORM_VIEW = "/module/legacyui/admin/forms/auditFieldForm";
	private static final String SUBMIT_VIEW = "auditField.form";
    // command name is auditField
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(AuditFieldController.class);

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request) throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			view = SUBMIT_VIEW;

			try {
				int i = Context.getFormService().mergeDuplicateFields();
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ARGS, i);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Form.auditSuccess");
			} catch (APIException e) {
				log.warn("Error in mergeDuplicateFields", e);

				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Form.auditError");
			}
		}

		return new ModelAndView(new RedirectView(view));
	}

	@GetMapping
	public String initForm() {
		return FORM_VIEW;
	}

}
