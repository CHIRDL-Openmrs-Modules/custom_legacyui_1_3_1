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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tests for the {@link LocationFormController} which handles the location.form
 * page.
 */
public class LocationFormControllerTest extends BaseModuleWebContextSensitiveTest {

	@Autowired
	LocationFormController controller;

	private MockMvc mockMvc;

	@BeforeEach
	public void runBeforeEachTest() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}

	/**
	 * @see LocationFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldNotRetireLocationIfReasonIsEmpty() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.setParameter("retireLocation", "true");

		Location location = Context.getLocationService().getLocation(1);
		location.setRetireReason("");
		location.setRetired(Boolean.TRUE);
		BindingResult errors = new BindException(location, "location");
		ModelAndView modelAndView = this.controller.processSubmit(request, location, errors);

		// make sure an error is returned because of the empty retire reason
		BeanPropertyBindingResult bindingResult = (BeanPropertyBindingResult) modelAndView.getModel()
				.get("org.springframework.validation.BindingResult.location");
		Assertions.assertNotNull(bindingResult);
		Assertions.assertTrue(bindingResult.hasFieldErrors("retireReason"));
	}

	/**
	 * @see LocationFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldRetireLocation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
		request.setParameter("locationId", "1");
		request.setParameter("retireReason", "some non-null reason");
		request.setParameter("retireLocation", "true");

		this.mockMvc
				.perform(post("/admin/locations/location.form").param("locationId", "1")
						.param("retireReason", "some non-null reason").param("retireLocation", "true"))
				.andExpect(status().isFound()).andExpect(redirectedUrlPattern("location.*"))
				.andExpect(model().hasNoErrors());

		Location retiredLocation = Context.getLocationService().getLocation(1);
		Assertions.assertTrue(retiredLocation.isRetired());
	}

	/**
	 * @see LocationFormController#formBackingObject(HttpServletRequest)
	 */
	@Test
	public void formBackingObject_shouldReturnValidLocationGivenValidLocationId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		request.setParameter("locationId", "1");

		this.mockMvc.perform(get("/admin/locations/location.form").param("locationId", "1")).andExpect(status().isOk())
				.andExpect(model().hasNoErrors());

		Location command = (Location) this.controller.formBackingObject(request);

		// make sure there is an "locationId" filled in on the concept
		Assertions.assertNotNull(command.getLocationId());
	}
}
