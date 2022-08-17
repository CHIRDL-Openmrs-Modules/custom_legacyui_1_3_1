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

import javax.servlet.http.HttpServletRequest;

import org.openmrs.ImplementationId;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.validator.ImplementationIdValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * This controller controls all uploading and syncing the Implementation Id with
 * the implementation id server
 */
@Controller
@RequestMapping(value = "admin/maintenance/implementationid.form")
public class ImplementationIdFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/maintenance/implementationIdForm";
	private static final String SUBMIT_VIEW = "implementationid.form";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(ImplementationIdFormController.class);

	/**
	 * Actions taken when the form is submitted
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	protected ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("implId") ImplementationId implId,
			BindingResult errors) throws Exception {

		new ImplementationIdValidator().validate(implId, errors);

		if (errors.hasErrors()) {
			return ShowFormUtil.showForm(errors, FORM_VIEW);
		}

		try {
			Context.getAdministrationService().setImplementationId(implId);
			request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ImplementationId.validatedId");
		} catch (APIException e) {
			log.warn("Unable to set implementation id", e);
			errors.reject(e.getMessage());
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, e.getMessage());
			return ShowFormUtil.showForm(errors, FORM_VIEW);
		}

		return new ModelAndView(new RedirectView(SUBMIT_VIEW));
	}

	/**
	 * The object that backs the form. The class of this object (String) is set in
	 * the servlet descriptor file
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("implId")
	protected Object formBackingObject() {

		// get the impl id from the database that is the implementation id
		ImplementationId implId = Context.getAdministrationService().getImplementationId();

		if (implId != null) {
			return implId;
		} else {
			return new ImplementationId();
		}
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}

}
