/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.encounter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;

/**
 * Tests against the {@link EncounterTypeListController}
 */
public class EncounterTypeListControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	EncounterTypeListController controller;

	private MockMvc mockMvc;

	/**
	 * @see EncounterTypeListController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldNotFailIfNoEncounterTypesAreSelected() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();

		this.mockMvc.perform(get("/admin/encounters/encounterType.list")).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());
	}
}
