/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.web.controller;

import java.util.List;

import org.openmrs.module.ModuleFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller which allows users to identify dependencies between modules for shutdown/restart purposes.
 */
@Controller
@RequestMapping(value = "/admin/modules/manage/")
public class ModuleManagementController {
	
	/**
	 * Logger for this class and subclasses
	 */
	
	@GetMapping(value = "/checkdependencies")
	@ResponseBody
	public List<String> manage(@RequestParam(value = "moduleId") String moduleId) {
		return ModuleFactory.getDependencies(moduleId);
		
	}
	
}
