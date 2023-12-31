/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.encounter;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.EncounterType;
import org.openmrs.Privilege;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.EncounterTypeLockedException;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.PrivilegeEditor;
import org.openmrs.validator.EncounterTypeValidator;
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
@RequestMapping(value = "admin/encounters/encounterType.form")
public class EncounterTypeFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/encounters/encounterTypeForm";
	private static final String SUBMIT_VIEW = "encounterType.list";

	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(EncounterTypeFormController.class);

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
		binder.registerCustomEditor(Privilege.class, new PrivilegeEditor());
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
			@ModelAttribute("encounterType") EncounterType encounterType, BindingResult errors, ModelMap map) throws Exception {

		new EncounterTypeValidator().validate(encounterType, errors);

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}

			getModelMap(map);
			return new ModelAndView(FORM_VIEW, map);
		}

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			EncounterService es = Context.getEncounterService();

			try {
				if (request.getParameter("save") != null) {
					es.saveEncounterType(encounterType);
					view = SUBMIT_VIEW;
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "EncounterType.saved");
				}

				// if the user is retiring out the EncounterType
				else if (request.getParameter("retire") != null) {
					String retireReason = request.getParameter("retireReason");
					if (encounterType.getEncounterTypeId() != null && !(StringUtils.hasText(retireReason))) {
						errors.reject("retireReason", "general.retiredReason.empty");
						return ShowFormUtil.showForm(errors, FORM_VIEW);
					}
					es.retireEncounterType(encounterType, retireReason);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "EncounterType.retiredSuccessfully");

					view = SUBMIT_VIEW;
				}

				// if the user is unretiring the EncounterType
				else if (request.getParameter("unretire") != null) {
					es.unretireEncounterType(encounterType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "EncounterType.unretiredSuccessfully");

					view = SUBMIT_VIEW;
				}

				// if the user is purging the encounterType
				else if (request.getParameter("purge") != null) {

					try {
						es.purgeEncounterType(encounterType);
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "EncounterType.purgedSuccessfully");
						view = SUBMIT_VIEW;
					} catch (DataIntegrityViolationException e) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
						view = "encounterType.form?encounterTypeId=" + encounterType.getEncounterTypeId();
					} catch (APIException e) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
								"error.general: " + e.getLocalizedMessage());
						view = "encounterType.form?encounterTypeId=" + encounterType.getEncounterTypeId();
					}
				}
			} catch (EncounterTypeLockedException e) {
				log.error("tried to save, retire, unretire or delete encounter type while encounter types were locked",
						e);
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "EncounterType.encounterTypes.locked");
				if (encounterType.getEncounterTypeId() != null) {
					view = "encounterType.form?encounterTypeId=" + encounterType.getEncounterTypeId();
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
	@ModelAttribute("encounterType")
	protected Object formBackingObject(HttpServletRequest request) {

		EncounterType encounterType = null;

		if (Context.isAuthenticated()) {
			EncounterService os = Context.getEncounterService();
			String encounterTypeId = request.getParameter("encounterTypeId");
			if (encounterTypeId != null) {
				encounterType = os.getEncounterType(Integer.valueOf(encounterTypeId));
			}
		}

		if (encounterType == null) {
			encounterType = new EncounterType();
		}

		return encounterType;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.Errors)
	 */
	@GetMapping
	public String initForm(ModelMap map) throws Exception {

		getModelMap(map);

		return FORM_VIEW;
	}

	public void getModelMap(ModelMap map) {
		List<Privilege> privileges = new ArrayList<>();

		if (Context.isAuthenticated()) {
			privileges = Context.getUserService().getAllPrivileges();
		}

		map.put("privileges", privileges);
	}

}
