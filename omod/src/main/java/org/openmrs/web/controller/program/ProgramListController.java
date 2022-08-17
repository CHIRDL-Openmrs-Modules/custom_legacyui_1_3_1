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

import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Program;
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
@RequestMapping(value = "admin/programs/program.list")
public class ProgramListController {

	private static final String FORM_VIEW = "/module/legacyui/admin/programs/programList";
	private static final String SUBMIT_VIEW = "program.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(ProgramListController.class);

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("programList")
	protected Object formBackingObject() {

		// default empty Object
		List<Program> programList = new Vector<>();

		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			ProgramWorkflowService ps = Context.getProgramWorkflowService();
			programList = ps.getAllPrograms();
		}

		return programList;
	}

	@PostMapping
	protected ModelAndView processSubmit(HttpServletRequest request) throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;
		if (Context.isAuthenticated()) {
			String[] programList = request.getParameterValues("programId");
			ProgramWorkflowService ps = Context.getProgramWorkflowService();

			StringBuilder success = new StringBuilder("");
			StringBuilder error = new StringBuilder();

			MessageSourceService mss = Context.getMessageSourceService();
			String deleted = mss.getMessage("general.deleted");
			String notDeleted = mss.getMessage("general.cannot.delete");
			String textProgram = mss.getMessage("Program.program");
			String noneDeleted = mss.getMessage("Program.nonedeleted");
			if (programList != null) {
				for (String p : programList) {

					try {
						ps.purgeProgram(ps.getProgram(Integer.valueOf(p)));
						if (!"".equals(success.toString())) {
							success.append("<br/>");
						}
						success.append(textProgram);
						success.append(" ");
						success.append(p);
						success.append(" ");
						success.append(deleted);
					} catch (APIException e) {
						log.warn("Error deleting program", e);
						if (!"".equals(error.toString())) {
							error.append("<br/>");
						}
						error.append(textProgram).append(" ").append(p).append(" ").append(notDeleted);
					}
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

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
