/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.maintenance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 *
 */
@Controller
@RequestMapping(value = "admin/maintenance/globalProps.form")
public class GlobalPropertyController {

	private static final String FORM_VIEW = "/module/legacyui/admin/maintenance/globalPropsForm";
	private static final String SUBMIT_VIEW = "globalProps.form";
	
	public static final String PROP_NAME = "property";

	public static final String PROP_VAL_NAME = "value";

	public static final String PROP_DESC_NAME = "description";

	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(GlobalPropertyController.class);

	/**
	 * The onSubmit function receives the form/command object that was modified
	 * by the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 * @should save or update included properties
	 * @should purge not included properties
	 */
	@PostMapping
	protected ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("globalProps") List<GlobalProperty> formBackingObject, BindingResult errors) throws Exception {

		String action = request.getParameter("action");
		if (action == null) {
			action = "cancel";
		}

		if (action.equals(Context.getMessageSourceService().getMessage("general.save"))) {
			HttpSession httpSession = request.getSession();

			if (Context.isAuthenticated()) {
				AdministrationService as = Context.getAdministrationService();

				// fetch the backing object
				// and save it to a hashmap for easy retrieval of
				// already-used-GPs
				Map<String, GlobalProperty> formBackingObjectMap = new HashMap<String, GlobalProperty>();
				for (GlobalProperty prop : formBackingObject) {
					formBackingObjectMap.put(prop.getProperty(), prop);
				}

				// the list we'll save to the database
				List<GlobalProperty> globalPropList = new ArrayList<GlobalProperty>();

				String[] keys = request.getParameterValues(PROP_NAME);
				String[] values = request.getParameterValues(PROP_VAL_NAME);
				String[] descriptions = request.getParameterValues(PROP_DESC_NAME);

				for (int x = 0; x < keys.length; x++) {
					String key = keys[x];
					String val = values[x];
					String desc = descriptions[x];

					// try to get an already-used global property for this key
					GlobalProperty tmpGlobalProperty = formBackingObjectMap.get(key);

					// if it exists, use that object...just update it
					if (tmpGlobalProperty != null) {
						tmpGlobalProperty.setPropertyValue(val);
						tmpGlobalProperty.setDescription(desc);
						globalPropList.add(tmpGlobalProperty);
					} else {
						// if it doesn't exist, create a new global property
						globalPropList.add(new GlobalProperty(key, val, desc));
					}
				}

				try {
					// delete all properties not in this new list
					List<GlobalProperty> purgeGlobalPropList = new ArrayList<GlobalProperty>(
							as.getAllGlobalProperties());
					purgeGlobalPropList.removeAll(globalPropList);
					as.purgeGlobalProperties(purgeGlobalPropList);

					as.saveGlobalProperties(globalPropList);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "GlobalProperty.saved");

					// refresh log level from global property(ies)
					OpenmrsUtil.applyLogLevels();

					OpenmrsUtil.setupLogAppenders();
				} catch (Exception e) {
					log.error("Error saving properties", e);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "GlobalProperty.not.saved");
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, e.getMessage());
				}

				return new ModelAndView(new RedirectView(SUBMIT_VIEW));

			}
		}

		return ShowFormUtil.showForm(errors, FORM_VIEW);

	}

	/**
	 * This is called prior to displaying a form for the first time. It tells
	 * Spring the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("globalProps")
	protected Object formBackingObject() {

		if (Context.isAuthenticated()) {
			// return a non-empty list if the user has authenticated properly
			AdministrationService as = Context.getAdministrationService();
			return as.getAllGlobalProperties();
		} else {
			return new ArrayList<GlobalProperty>();
		}
	}
	
	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}

}
