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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.ConceptStateConversion;
import org.openmrs.api.APIException;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/programs/conversion.list")
public class StateConversionListController {

	private static final String FORM_VIEW = "/module/legacyui/admin/programs/conversionList";
	private static final String SUBMIT_VIEW = "conversion.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(StateConversionListController.class);

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
			String[] conversionIdList = request.getParameterValues("conceptStateConversionId");
			ProgramWorkflowService pws = Context.getProgramWorkflowService();

			StringBuilder success = new StringBuilder("");
			StringBuilder error = new StringBuilder("");
			int numDeleted = 0;

			MessageSourceService mss = Context.getMessageSourceService();
			String deleted = mss.getMessage("general.deleted");
			String notDeleted = mss.getMessage("general.cannot.delete");
			String textConversion = mss.getMessage("Program.conversion");
			String noneDeleted = mss.getMessage("Program.conversion.nonedeleted");
			if (conversionIdList != null) {
				for (String id : conversionIdList) {
					try {
						pws.purgeConceptStateConversion(pws.getConceptStateConversion(Integer.valueOf(id)));
						if (!"".equals(success.toString())) {
							success.append("<br/>");
						}
						success.append(textConversion).append(" ").append(id).append(" ").append(deleted);
						numDeleted++;
					} catch (APIException e) {
						log.warn("Error deleting concept state conversion", e);
						if (!"".equals(error.toString())) {
							error.append("<br/>");
						}
						error.append(textConversion).append(" ").append(id).append(" ").append(notDeleted);
					}
				}

				if (numDeleted > 3) {
					success = new StringBuilder(numDeleted).append(" ").append(deleted);
				}
			} else {
				success.append(noneDeleted);
			}
			view = SUBMIT_VIEW;
			if (!"".equals(success.toString())) {
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success.toString());
			}
			if (!"".equals(error.toString())) {
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error.toString());
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
	@ModelAttribute("stateConversionList")
	protected Object formBackingObject() {

		// default empty Object
		List<ConceptStateConversion> conversionList = new ArrayList<>();

		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			ProgramWorkflowService ps = Context.getProgramWorkflowService();
			conversionList = ps.getAllConceptStateConversions();
		}

		return conversionList;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
