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

import java.util.HashMap;
import java.util.Map;

import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A controller for the search index
 */
@Controller
public class SearchIndexController {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexController.class);

	/**
	 * @should return the search index view
	 * @return the searchIndex view
	 */
    @GetMapping(value = "admin/maintenance/searchIndex")
	public String showPage() {
		return "/module/legacyui/admin/maintenance/searchIndex";
	}

	/**
	 * @should return true for success if the update does not fail
	 * @should return false for success if a RuntimeException is thrown
	 * @return a marker indicating success
	 */
	@PostMapping(value = "admin/maintenance/rebuildSearchIndex")
	public @ResponseBody Map<String, Object> rebuildSearchIndex() {
		boolean success = true;
		Map<String, Object> results = new HashMap<String, Object>();
		log.debug("rebuilding search index");
		try {
			Context.updateSearchIndex();
		} catch (RuntimeException e) {
			success = false;
		}

		results.put("success", success);
		return results;
	}
}
