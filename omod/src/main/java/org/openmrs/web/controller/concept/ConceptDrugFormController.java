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

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.validator.ConceptDrugValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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
@RequestMapping(value = "admin/concepts/conceptDrug.form")
public class ConceptDrugFormController {

	private static final String FORM_VIEW = "/admin/concepts/conceptDrugForm";
	/**
	 * Set the name of the view that should be shown on successful submit.
	 */
	private static final String SUBMIT_VIEW = "conceptDrug.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(ConceptDrugFormController.class);

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
		binder.registerCustomEditor(java.lang.Double.class, new CustomNumberEditor(java.lang.Double.class, true));
		binder.registerCustomEditor(Concept.class, new ConceptEditor());
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
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("drug") Drug drug,
			BindingResult errors) throws Exception {
		
		new ConceptDrugValidator().validate(drug, errors);
		
		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			return new ModelAndView(FORM_VIEW);
		}
		
		HttpSession httpSession = request.getSession();
		String view = FORM_VIEW;
		
		if (Context.isAuthenticated()) {
			ConceptService conceptService = Context.getConceptService();

			if (request.getParameter("retireDrug") != null) {
				String retireReason = request.getParameter("retireReason");
				if (drug.getId() != null && (retireReason == null || retireReason.length() == 0)) {
					errors.reject("retireReason", "ConceptDrug.retire.reason.empty");
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}

				conceptService.retireDrug(drug, retireReason);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptDrug.retiredSuccessfully");
			}

			// if this obs is already voided and needs to be unvoided
			else if (request.getParameter("unretireDrug") != null) {
				conceptService.unretireDrug(drug);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptDrug.unretiredSuccessfully");
			} else if (request.getParameter("purgeDrug") != null) {
				try {
					conceptService.purgeDrug(drug);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptDrug.purgedSuccessfully");
				} catch (DataIntegrityViolationException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
				}
			} else {
				conceptService.saveDrug(drug);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "ConceptDrug.saved");
			}

			view = SUBMIT_VIEW;

		}

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("drug")
	protected Object formBackingObject(HttpServletRequest request) {

		Drug drug = null;

		if (Context.isAuthenticated()) {
			ConceptService cs = Context.getConceptService();
			String id = request.getParameter("drugId");
			if (id != null) {
				drug = cs.getDrug(Integer.valueOf(id));
			}
		}

		if (drug == null) {
			drug = new Drug();
		}

		return drug;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.Errors)
	 */
	@GetMapping
	public String initForm(HttpServletRequest request, ModelMap map, @ModelAttribute("drug") Drug drug, BindingResult errors) throws Exception {

		String defaultVerbose = "false";

		if (Context.isAuthenticated()) {
			if (drug.getConcept() != null) {
				map.put("conceptName", drug.getConcept().getName(request.getLocale()));
			}
			defaultVerbose = Context.getAuthenticatedUser()
					.getUserProperty(OpenmrsConstants.USER_PROPERTY_SHOW_VERBOSE);
		}

		map.put("defaultVerbose", defaultVerbose.equals("true") ? true : false);

		String editReason = request.getParameter("editReason");
		if (editReason == null) {
			editReason = "";
		}

		map.put("editReason", editReason);

		return FORM_VIEW;
	}

}
