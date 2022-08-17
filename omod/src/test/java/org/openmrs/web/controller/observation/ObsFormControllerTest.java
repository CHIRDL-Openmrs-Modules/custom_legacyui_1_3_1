/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test the methods on the
 * {@link org.openmrs.web.controller.observation.ObsFormController}
 */
public class ObsFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	ObsFormController controller;

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	/**
	 * Tests that an "encounterId" parameter sets the obs.encounter attribute on an
	 * empty obs
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldGetObsFormWithEncounterFilledIn() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setParameter("encounterId", "3");

		Obs commandObs = (Obs) this.controller.formBackingObject(request);
		Assertions.assertNotNull(commandObs.getEncounter());
		
	}

	/**
	 * Test to make sure a new patient form can save a person relationship
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldSaveObsFormNormally() throws Exception {
		ObsService os = Context.getObsService();

		// set up the request and do an initial "get" as if the user loaded the
		// page for the first time
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/observations/obs.form");
		request.setSession(new MockHttpSession(null));

		this.mockMvc.perform(get("/admin/observations/obs.form").session(new MockHttpSession(null)))
				.andExpect(status().isOk());
		// set this to be a page submission
		
		this.mockMvc
				.perform(post("/admin/observations/obs.form").param("person", "2").param("encounter", "3")
						.param("location", "1").param("obsDatetime", "05/05/2005").param("concept", "4")
						.param("valueCoded", "5").param("saveObs", "Save Obs"))
				.andExpect(status().isFound()).andExpect(model().hasNoErrors());

		// make sure an obs was created
		List<Obs> obsForPatient = os.getObservationsByPerson(new Person(2));
		assertEquals(1, obsForPatient.size());
		assertEquals(3, obsForPatient.get(0).getEncounter().getId().intValue());
		assertEquals(1, obsForPatient.get(0).getLocation().getId().intValue());
	}

}
