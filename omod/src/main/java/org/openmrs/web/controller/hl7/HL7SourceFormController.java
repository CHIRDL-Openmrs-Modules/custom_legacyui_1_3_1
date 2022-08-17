/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.hl7;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Service;
import org.openmrs.hl7.HL7Source;
import org.openmrs.validator.HL7SourceValidator;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * This is the controlling class for hl7SourceForm.jsp page. It initBinder and
 * formBackingObject are called before page load. After submission,The onSubmit
 * function receives the form/command object that was modified by the input form
 * and saves it to the db
 */
@Controller
@RequestMapping(value = "admin/hl7/hl7Source.form")
public class HL7SourceFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/hl7/hl7SourceForm";
	private static final String SUBMIT_VIEW = "hl7Source.list";

	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(HL7SourceFormController.class);

	/**
	 * Allows for Integers to be used as values in input tags. Normally, only
	 * strings and lists are expected
	 * 
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	protected ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("hl7Source") HL7Source hl7Source,
			BindingResult errors) throws Exception {

		new HL7SourceValidator().validate(hl7Source, errors);

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}

			return new ModelAndView(FORM_VIEW);
		}

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			HL7Service hs = Context.getHL7Service();

			if (request.getParameter("save") != null) {
				hs.saveHL7Source(hl7Source);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "HL7Source.saved");
			}

			// if the user is retiring out the HL7Source
			// not implemented yet

			// if the user is purging the HL7Source
			else if (request.getParameter("purge") != null) {

				try {
					hs.purgeHL7Source(hl7Source);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "HL7Source.purgedSuccessfully");
					view = SUBMIT_VIEW;
				} catch (DataIntegrityViolationException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
					view = "hl7Source.form?hl7SourceId=" + hl7Source.getHL7SourceId();
				} catch (APIException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
							"error.general: " + e.getLocalizedMessage());
					view = "hl7Source.form?hl7SourceId=" + hl7Source.getHL7SourceId();
				}
			}

		}

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("hl7Source")
	protected Object formBackingObject(HttpServletRequest request) {
		HL7Source hl7Source = null;

		if (Context.isAuthenticated()) {
			HL7Service hs = Context.getHL7Service();
			String hl7SourceId = request.getParameter("hl7SourceId");
			if (hl7SourceId != null) {
				hl7Source = hs.getHL7Source(Integer.valueOf(hl7SourceId));
			}
		}

		if (hl7Source == null) {
			hl7Source = new HL7Source();
		}

		return hl7Source;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
