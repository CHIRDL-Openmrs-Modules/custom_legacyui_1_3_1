/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.patient;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.PatientIdentifierType;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientIdentifierTypeLockedException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.patient.IdentifierValidator;
import org.openmrs.validator.PatientIdentifierTypeValidator;
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
@RequestMapping(value = "admin/patients/patientIdentifierType.form")
public class PatientIdentifierTypeFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/patients/patientIdentifierTypeForm";
	private static final String SUBMIT_VIEW = "patientIdentifierType.list";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(PatientIdentifierTypeFormController.class);

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
	protected ModelAndView processSubmit(HttpServletRequest request,
			@ModelAttribute("patientIdentifierType") PatientIdentifierType identifierType, BindingResult errors, ModelMap map)
			throws Exception {

		new PatientIdentifierTypeValidator().validate(identifierType, errors);
		
		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}

			getModelMap(map);
			return new ModelAndView(FORM_VIEW, map);
		}
		
		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		ModelAndView toReturn = new ModelAndView(new RedirectView(view));

		if (Context.isAuthenticated()) {

			PatientService ps = Context.getPatientService();

			// to save the patient identifier type
			try {
				if (request.getParameter("save") != null) {
					ps.savePatientIdentifierType(identifierType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "PatientIdentifierType.saved");
					toReturn = new ModelAndView(new RedirectView(SUBMIT_VIEW));
				}
				// if the user is retiring the identifierType
				else if (request.getParameter("retire") != null) {
					String retireReason = request.getParameter("retireReason");
					if (identifierType.getPatientIdentifierTypeId() != null && !(StringUtils.hasText(retireReason))) {
						errors.reject("retireReason", "general.retiredReason.empty");
						return ShowFormUtil.showForm(errors, FORM_VIEW);
					}
					ps.retirePatientIdentifierType(identifierType, retireReason);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR,
							"PatientIdentifierType.retiredSuccessfully");
					toReturn = new ModelAndView(new RedirectView(SUBMIT_VIEW));
				}
				// if the user is purging the identifierType
				else if (request.getParameter("purge") != null) {
					try {
						ps.purgePatientIdentifierType(identifierType);
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR,
								"PatientIdentifierType.purgedSuccessfully");
						toReturn = new ModelAndView(new RedirectView(SUBMIT_VIEW));
					} catch (DataIntegrityViolationException e) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
						return ShowFormUtil.showForm(errors, FORM_VIEW);
					}
				}
				// if the user unretiring patient identifier type
				else if (request.getParameter("unretire") != null) {
					ps.unretirePatientIdentifierType(identifierType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR,
							"PatientIdentifierType.unretiredSuccessfully");
					toReturn = new ModelAndView(new RedirectView(SUBMIT_VIEW));
				}
			} catch (PatientIdentifierTypeLockedException e) {
				log.error("PatientIdentifierType.locked", e);
				errors.reject(e.getMessage());
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "PatientIdentifierType.locked");
				return ShowFormUtil.showForm(errors, FORM_VIEW);
			} catch (APIException e) {
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.general: " + e.getLocalizedMessage());
				return ShowFormUtil.showForm(errors, FORM_VIEW);
			}
		}

		return toReturn;
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("patientIdentifierType")
	protected Object formBackingObject(HttpServletRequest request) {

		PatientIdentifierType identifierType = null;

		if (Context.isAuthenticated()) {
			PatientService ps = Context.getPatientService();
			String identifierTypeId = request.getParameter("patientIdentifierTypeId");
			if (identifierTypeId != null) {
				identifierType = ps.getPatientIdentifierType(Integer.valueOf(identifierTypeId));
			}
		}

		if (identifierType == null) {
			identifierType = new PatientIdentifierType();
		}

		return identifierType;
	}

	/**
	 * Called prior to form display. Allows for data to be put in the request to be
	 * used in the view
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@GetMapping
	public String initForm(ModelMap toReturn) throws Exception {

		getModelMap(toReturn);

		return FORM_VIEW;
	}

	public void getModelMap(ModelMap toReturn) {
		Collection<IdentifierValidator> pivs = Context.getPatientService().getAllIdentifierValidators();

		toReturn.put("patientIdentifierValidators", pivs);

		String defaultValidatorName = Context.getPatientService().getDefaultIdentifierValidator().getName();

		toReturn.put("defaultValidatorName", defaultValidatorName);

		toReturn.put("locationBehaviors", PatientIdentifierType.LocationBehavior.values());

		toReturn.put("uniquenessBehaviors", PatientIdentifierType.UniquenessBehavior.values());
	}
}
