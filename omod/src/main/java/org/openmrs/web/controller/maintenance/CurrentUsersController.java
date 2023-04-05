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

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.servlet.LoginServlet;
import org.openmrs.web.user.CurrentUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Display the current users logged in the system.
 *
 * @see CurrentUsers
 * @see LoginServlet
 * @see org.openmrs.web.SessionListener
 */
@Controller
public class CurrentUsersController {

    private static final Logger log = LoggerFactory.getLogger(CurrentUsersController.class);

	/**
	 * Lists current users.
	 *
	 * @param request
	 * @param modelMap
	 */
    @GetMapping(value = "admin/maintenance/currentUsers.list")
	public String listCurrentUsers(HttpServletRequest request, ModelMap modelMap) {
		log.debug("List current users");
		if (!Context.hasPrivilege(PrivilegeConstants.GET_USERS)) {
			throw new APIAuthenticationException("Privilege required: " + PrivilegeConstants.GET_USERS);
		}

		modelMap.put("currentUsers", CurrentUsers.getCurrentUsernames(request.getSession()));
		return "/module/legacyui/admin/maintenance/currentUsers";
	}

}
