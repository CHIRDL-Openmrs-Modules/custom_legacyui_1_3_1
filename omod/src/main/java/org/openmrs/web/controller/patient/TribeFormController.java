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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * This controller is left around only so that a message can be displayed where the old Manage Tribe
 * link was. This controller will be removed in the next version.
 */
@Controller
@RequestMapping(value = "admin/patients/tribe.list")
public class TribeFormController {
	
	private static final String FORM_VIEW = "/module/legacyui/admin/patients/tribeList";
	private static final String SUBMIT_VIEW = "tribe.list";
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("tribeForm")
	protected Object formBackingObject() {
		return new Object();
	}
	
	@GetMapping
	public String initForm() {
		return FORM_VIEW;
	}
}
