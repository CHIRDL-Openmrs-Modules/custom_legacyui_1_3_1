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

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.openmrs.util.MemoryAppender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Get the log lines from the MEMORY_APPENDER appender of log4j as a String list
 * and give it to the view.
 * 
 * @see org.openmrs.util.MemoryAppender
 */
@Controller
@RequestMapping(value = "admin/maintenance/serverLog.form")
public class ServerLogController {

	private static final String FORM_VIEW = "/module/legacyui/admin/maintenance/serverLog";
	private static final String SUBMIT_VIEW = "serverLog.form";

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	protected ModelAndView processSubmit() throws Exception {
		return new ModelAndView(new RedirectView(SUBMIT_VIEW));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("logLines")
	protected Object formBackingObject() {
		Appender appender = Logger.getRootLogger().getAppender("MEMORY_APPENDER");
		if (appender instanceof MemoryAppender) {
			MemoryAppender memoryAppender = (MemoryAppender) appender;
			return memoryAppender.getLogLines();

		}
		return new ArrayList<String>();
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}

}
