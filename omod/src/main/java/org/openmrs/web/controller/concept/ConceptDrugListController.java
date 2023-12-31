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

import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "admin/concepts/conceptDrug.list")
public class ConceptDrugListController {
	
	private static final String FORM_VIEW = "/admin/concepts/conceptDrugList";
	/**
	 * Set the name of the view that should be shown on successful submit.
	 */
	private static final String SUBMIT_VIEW = "conceptDrug.list";
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	
	@ModelAttribute("conceptDrugList") 
	protected Object formBackingObject(HttpServletRequest request) { 
		//HttpSession httpSession = request.getSession();
		
		// default empty Object
		List<Drug> conceptDrugList = new Vector<Drug>();
		
		//only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			ConceptService cs = Context.getConceptService();
			conceptDrugList = cs.getAllDrugs();
		}
		
		return conceptDrugList;
	}
	
	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
