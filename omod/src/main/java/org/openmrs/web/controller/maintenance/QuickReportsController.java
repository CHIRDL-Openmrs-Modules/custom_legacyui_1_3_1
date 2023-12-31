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

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Display the quick reports in the system.
 *
 * @see org.openmrs.web.SessionListener
 */
@Controller
public class QuickReportsController {

    private static final Logger log = LoggerFactory.getLogger(QuickReportsController.class);

	public static final String QUICK_REPORTS_PATH = "admin/maintenance/quickReport";
	public static final String QUICK_REPORTS_VIEW_PATH = "/module/legacyui/admin/maintenance/quickReport";

	/**
	 * Lists quick reports.
	 *
	 * @param request
	 * @param modelMap
	 */
	@GetMapping(value = QUICK_REPORTS_PATH)
	public String showQuickReports(HttpServletRequest request, ModelMap modelMap) {
		log.debug("Lists quick reports");
		return QUICK_REPORTS_VIEW_PATH;
	}

}
