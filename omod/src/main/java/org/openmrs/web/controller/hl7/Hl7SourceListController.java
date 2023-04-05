/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.hl7;

import java.util.List;
import java.util.Vector;

import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Service;
import org.openmrs.hl7.HL7Source;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * This is the controlling class for hl7SourceList.jsp page. It initBinder and
 * formBackingObject are called before page load
 *
 */
@Controller
@RequestMapping(value = "admin/hl7/hl7Source.list")
public class Hl7SourceListController {

	private static final String FORM_VIEW = "/module/legacyui/admin/hl7/hl7SourceList";
	private static final String SUBMIT_VIEW = "hl7Source.list";

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
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("hl7SourceList")
	protected Object formBackingObject() {

		// default empty Object
		List<HL7Source> hl7SourceList = new Vector<>();

		// only fill the Object is the user has authenticated properly
		if (Context.isAuthenticated()) {
			HL7Service hl7s = Context.getHL7Service();
			hl7SourceList = hl7s.getAllHL7Sources();
		}

		return hl7SourceList;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
