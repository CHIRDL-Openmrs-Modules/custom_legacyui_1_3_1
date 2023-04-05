/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.person;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 */
@Controller
@RequestMapping(value = "personDashboard.form")
public class PersonDashboardController {

	private static final String FORM_VIEW = "/personDashboardForm";
	private static final String SUBMIT_VIEW = "personDashboard.form";

	@ModelAttribute("person")
	protected Object formBackingObject(HttpServletRequest request) {
		if (!Context.isAuthenticated()) {
			return new Person();
		}
		return Context.getPersonService().getPerson(Integer.valueOf(request.getParameter("personId")));
	}

	@GetMapping
	public String initForm() {
		return FORM_VIEW;
	}
}
