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
import org.openmrs.ConceptStateConversion;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.APIException;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.ProgramWorkflowEditor;
import org.openmrs.propertyeditor.ProgramWorkflowStateEditor;
import org.openmrs.validator.StateConversionValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/programs/conversion.form")
public class StateConversionFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/programs/conversionForm";
	private static final String SUBMIT_VIEW = "conversion.list";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(StateConversionFormController.class);

	/*
	 * public void setStateConversionValidator(StateConversionValidator
	 * stateConversionValidator) { super.setValidator(stateConversionValidator); }
	 */

	@InitBinder
	protected void initBinder(WebDataBinder binder) {

		binder.registerCustomEditor(Concept.class, new ConceptEditor());
		binder.registerCustomEditor(ProgramWorkflow.class, new ProgramWorkflowEditor());
		binder.registerCustomEditor(ProgramWorkflowState.class, new ProgramWorkflowStateEditor());
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 * 
	 * @throws ServletRequestBindingException
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("conversion")
	protected Object formBackingObject(HttpServletRequest request) throws ServletRequestBindingException {
		log.debug("called formBackingObject");

		ConceptStateConversion conversion = null;

		if (Context.isAuthenticated()) {
			ProgramWorkflowService ps = Context.getProgramWorkflowService();
			String conversionId = ServletRequestUtils.getStringParameter(request, "conceptStateConversionId");
			if (conversionId != null) {
				log.debug("conversion ID is {}", conversionId);
				try {
					conversion = ps.getConceptStateConversion(Integer.valueOf(conversionId));
					log.debug("Csc is now {}", conversion);
				} catch (NumberFormatException nfe) {
					log.error("conversionId passed is not a valid number");
				}
			} else {
				log.debug("conversionID is null");
			}
		}

		if (conversion == null) {
			log.debug("Conversion is null");
			conversion = new ConceptStateConversion();
		} else {
			log.debug("Conversion is not null");
		}

		return conversion;

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
	public ModelAndView processSubmit(HttpServletRequest request,
			@ModelAttribute("conversion") ConceptStateConversion c, BindingResult errors) throws Exception {

		new StateConversionValidator().validate(c, errors);
		
		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			
			return new ModelAndView(FORM_VIEW);
		}
		
		log.debug("about to save {}", c);

		HttpSession httpSession = request.getSession();

		if (Context.isAuthenticated()) {

			boolean isError = false;
			try {

				Context.getProgramWorkflowService().saveConceptStateConversion(c);

			} catch (APIException ae) {

				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
						"ConceptStateConversion.error.incompleteform");
				isError = true;
				if (c.getConcept() == null) {
					errors.rejectValue("conversion.concept", "error.concept");
				}
				if (c.getProgramWorkflow() == null) {
					errors.rejectValue("conversion.programWorkflow", "error.programWorkflow");
				}
				if (c.getProgramWorkflowState() == null) {
					errors.rejectValue("conversion.programWorkflowState", "error.programWorkflowState");
				}

			}

			if (!isError) {

				String view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Program.conversion.saved");
				return new ModelAndView(new RedirectView(view));
			} 
			return ShowFormUtil.showForm(errors, FORM_VIEW);

		}

		return new ModelAndView(new RedirectView(FORM_VIEW));
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
