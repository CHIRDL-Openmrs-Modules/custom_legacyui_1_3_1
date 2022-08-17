/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.concept;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.ConceptSource;
import org.openmrs.ImplementationId;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.validator.ConceptSourceValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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

@Controller
@RequestMapping(value = "admin/concepts/conceptSource.form")
public class ConceptSourceFormController {

	private static final String FORM_VIEW = "/admin/concepts/conceptSourceForm";
	/**
	 * Set the name of the view that should be shown on successful submit.
	 */
	private static final String SUBMIT_VIEW = "conceptSource.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(ConceptSourceFormController.class);

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
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("conceptSource") ConceptSource conceptSource,
			BindingResult errors) throws Exception {

		new ConceptSourceValidator().validate(conceptSource, errors);
		
		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			return new ModelAndView(FORM_VIEW);
		}
		
		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {

			if (request.getParameter("retire") != null) {
				String retireReason = request.getParameter("retireReason");
				if (!StringUtils.hasText(retireReason)) {
					errors.reject("retireReason", "general.retiredReason.empty");
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}

				conceptSource.setRetireReason(retireReason);
				conceptSource.setRetired(true);

				Context.getConceptService().saveConceptSource(conceptSource);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptSource.retired");
			} else if (request.getParameter("restore") != null) {
				conceptSource.setRetireReason(null);
				conceptSource.setRetired(false);

				Context.getConceptService().saveConceptSource(conceptSource);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptSource.restored");
			} else if (request.getParameter("purge") != null) {
				try {
					Context.getConceptService().purgeConceptSource(conceptSource);
					view = SUBMIT_VIEW;
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptSource.purged");
				} catch (DataIntegrityViolationException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}
			} else {
				Context.getConceptService().saveConceptSource(conceptSource);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptSource.saved");
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
	@ModelAttribute("conceptSource")
	protected Object formBackingObject(HttpServletRequest request) {

		ConceptSource conceptSource = null;

		if (Context.isAuthenticated()) {
			ConceptService cs = Context.getConceptService();
			String conceptSourceId = request.getParameter("conceptSourceId");
			if (conceptSourceId != null) {
				conceptSource = cs.getConceptSource(Integer.valueOf(conceptSourceId));
			}
		}

		if (conceptSource == null) {
			conceptSource = new ConceptSource();
		}

		return conceptSource;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.Errors)
	 */
	@GetMapping
	public String initForm(ConceptSource conceptSource, ModelMap map)
			throws Exception {

		ImplementationId implId = Context.getAdministrationService().getImplementationId();

		if (implId != null && implId.getImplementationId().equals(conceptSource.getHl7Code())) {
			map.put("isImplementationId", true);
		}

		return FORM_VIEW;
	}

}
