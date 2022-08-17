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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Concept;
import org.openmrs.Program;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.WorkflowCollectionEditor;
import org.openmrs.validator.ProgramValidator;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Controller
@RequestMapping(value = "admin/programs/program.form")
public class ProgramFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/programs/programForm";
	private static final String SUBMIT_VIEW = "program.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(ProgramFormController.class);

	@InitBinder
	protected void initBinder(WebDataBinder binder) {

		// this depends on this form being a "session-form" (defined in
		// openrms-servlet.xml)
		Program program = (Program) binder.getTarget();

		binder.registerCustomEditor(Concept.class, new ConceptEditor());
		binder.registerCustomEditor(java.util.Collection.class, "allWorkflows", new WorkflowCollectionEditor(program));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("program")
	protected Object formBackingObject(HttpServletRequest request) {
		log.debug("called formBackingObject");

		Program program = null;

		if (Context.isAuthenticated()) {
			ProgramWorkflowService ps = Context.getProgramWorkflowService();
			String programId = request.getParameter("programId");
			if (programId != null) {
				program = ps.getProgram(Integer.valueOf(programId));
			}
		}

		if (program == null) {
			program = new Program();
		}

		return program;
	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 * @should save workflows with program
	 * @should edit existing workflows within programs
	 */
	@PostMapping
	protected ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("program") Program p, BindingResult errors)
			throws Exception {
		log.debug("about to save {}", p);
		
		new ProgramValidator().validate(p, errors);

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {

			try {
				Context.getProgramWorkflowService().saveProgram(p);
			} catch (Exception e) {
				log.warn("Error saving Program", e);
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, e.getMessage());
			}
			view = SUBMIT_VIEW;
		}

		return new ModelAndView(new RedirectView(view));
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
