/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.visit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.VisitType;
import org.openmrs.api.APIException;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.validator.VisitTypeValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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
 * Controller for editing visit types.
 *
 * @since 1.9
 */
@Controller
@RequestMapping(value = "admin/visits/visitType.form")
public class VisitTypeFormController {

	private static final String FORM_VIEW = "/admin/visits/visitTypeForm";
	private static final String SUBMIT_VIEW = "visitType.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(VisitTypeFormController.class);
	
	/**
	 * Allows for Integers to be used as values in input tags. Normally, only
	 * strings and lists are expected
	 *
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		// NumberFormat nf = NumberFormat.getInstance(new Locale("en_US"));
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
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("visitType") VisitType visitType,
			BindingResult errors) throws Exception {

		new VisitTypeValidator().validate(visitType, errors);
		
		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			return new ModelAndView(FORM_VIEW);
		}
		
		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			VisitService es = Context.getVisitService();

			if (request.getParameter("save") != null) {
				es.saveVisitType(visitType);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitType.saved");
			}

			// if the user is retiring out the VisitType
			else if (request.getParameter("retire") != null) {
				String retireReason = request.getParameter("retireReason");
				if (visitType.getVisitTypeId() != null && !(StringUtils.hasText(retireReason))) {
					errors.reject("retireReason", "general.retiredReason.empty");
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}

				es.retireVisitType(visitType, retireReason);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitType.retiredSuccessfully");

				view = SUBMIT_VIEW;
			}

			// if the user is unretiring the VisitType
			else if (request.getParameter("unretire") != null) {
				es.unretireVisitType(visitType);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitType.unretiredSuccessfully");
				view = SUBMIT_VIEW;
			}

			// if the user is purging the visitType
			else if (request.getParameter("purge") != null) {

				try {
					es.purgeVisitType(visitType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitType.purgedSuccessfully");
					view = SUBMIT_VIEW;
				} catch (DataIntegrityViolationException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
					view = "visitType.form?visitTypeId=" + visitType.getVisitTypeId();
				} catch (APIException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
							"error.general: " + e.getLocalizedMessage());
					view = "visitType.form?visitTypeId=" + visitType.getVisitTypeId();
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
	@ModelAttribute("visitType")
	protected Object formBackingObject(HttpServletRequest request) {

		VisitType visitType = null;

		if (Context.isAuthenticated()) {
			VisitService os = Context.getVisitService();
			String visitTypeId = request.getParameter("visitTypeId");
			if (visitTypeId != null) {
				visitType = os.getVisitType(Integer.valueOf(visitTypeId));
			}
		}

		if (visitType == null) {
			visitType = new VisitType();
		}

		return visitType;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}

}
