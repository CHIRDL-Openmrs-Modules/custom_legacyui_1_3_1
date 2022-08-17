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

import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Concept;
import org.openmrs.ConceptProposal;
import org.openmrs.Encounter;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/concepts/proposeConcept.form")
public class ProposeConceptFormController {

	private static final String FORM_VIEW = "/admin/concepts/proposeConceptForm";
	/**
	 * Set the name of the view that should be shown on successful submit.
	 */
	private static final String SUBMIT_VIEW = "conceptProposal.list";

	/**
	 * Logger for this class and subclasses
	 */
	private static final Logger log = LoggerFactory.getLogger(ProposeConceptFormController.class);

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */

	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("conceptProposal") ConceptProposal cp,
			BindingResult errors) throws Exception {

		Concept c = cp.getObsConcept();
		String id = ServletRequestUtils.getStringParameter(request, "conceptId", null);
		if (c == null && id != null) {
			c = Context.getConceptService().getConcept(Integer.valueOf(id));
			cp.setObsConcept(c);
		}

		Encounter e = cp.getEncounter();
		id = ServletRequestUtils.getStringParameter(request, "encounterId", null);
		if (e == null && id != null) {
			e = Context.getEncounterService().getEncounter(Integer.valueOf(id));
			cp.setEncounter(e);
		}

		if ("".equals(cp.getOriginalText())) {
			errors.rejectValue("originalText", "error.null");
		}

		return processSubmission(request, cp, errors);
	}

	protected ModelAndView processSubmission(HttpServletRequest request, ConceptProposal cp, BindingResult errors)
			throws Exception {

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			return new ModelAndView(FORM_VIEW);
		}
		log.debug("No errors -> processing submit");
		return processFormSubmission(request, cp);

	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	
	protected ModelAndView processFormSubmission(HttpServletRequest request, ConceptProposal cp) throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {

			// this proposal's final text
			ConceptService cs = Context.getConceptService();

			cp.setCreator(Context.getAuthenticatedUser());
			cp.setDateCreated(new Date());

			cs.saveConceptProposal(cp);

			view = SUBMIT_VIEW;
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptProposal.proposed");
		}

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 * 
	 * @throws ServletRequestBindingException
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("conceptProposal")
	protected Object formBackingObject(HttpServletRequest request) throws ServletRequestBindingException {

		ConceptProposal cp = new ConceptProposal();

		if (Context.isAuthenticated()) {
			ConceptService cs = Context.getConceptService();
			EncounterService es = Context.getEncounterService();
			String id = ServletRequestUtils.getStringParameter(request, "encounterId");
			if (id != null) {
				cp.setEncounter(es.getEncounter(Integer.valueOf(id)));
			}

			id = ServletRequestUtils.getStringParameter(request, "obsConceptId");
			if (id != null) {
				cp.setObsConcept(cs.getConcept(Integer.valueOf(id)));
			}

		}

		return cp;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.Errors)
	 */
	@GetMapping
	public String initForm(ModelMap map, @ModelAttribute("conceptProposal") ConceptProposal cp) {

		Locale locale = Context.getLocale();

		String defaultVerbose = "false";
		if (Context.isAuthenticated()) {
			// optional user property for default verbose display in concept search
			defaultVerbose = Context.getAuthenticatedUser()
					.getUserProperty(OpenmrsConstants.USER_PROPERTY_SHOW_VERBOSE);

			// preemptively get the obs concept name
			if (cp.getObsConcept() != null) {
				map.put("conceptName", cp.getObsConcept().getName(locale));
			}
		}
		map.put("defaultVerbose", defaultVerbose.equals("true") ? true : false);

		return FORM_VIEW;
	}

}
