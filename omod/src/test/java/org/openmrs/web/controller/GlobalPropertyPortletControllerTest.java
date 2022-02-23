/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class GlobalPropertyPortletControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see GlobalPropertyPortletController#populateModel(HttpServletRequest,Map)
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void populateModel_shouldExcludeMultiplePrefixes() throws Exception {
		//given
		GlobalPropertyPortletController portletController = new GlobalPropertyPortletController();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		Map<String, Object> model = new HashMap<String, Object>();
		
		//when
		String excludePrefix = "file.started;file.mandatory";
		model.put("excludePrefix", excludePrefix);
		GlobalProperty[] globalProperties = { new GlobalProperty("file.started", ""),
		        new GlobalProperty("file.mandatory", ""), new GlobalProperty("file.other", "") };
		Context.getAdministrationService().saveGlobalProperties(Arrays.asList(globalProperties));
		
		//then
		portletController.populateModel(request, model);
		List<GlobalProperty> properties = (List<GlobalProperty>) model.get("properties");
		Assertions.assertFalse(properties.contains(globalProperties[0]));
		Assertions.assertFalse(properties.contains(globalProperties[1]));
		Assertions.assertTrue(properties.contains(globalProperties[2]));
	}
	
	/**
	 * @see GlobalPropertyPortletController#setupModelForModule(Map)
	 */
	@Test
	public void setupModelForModule_shouldChangeModelIfForModuleIsPresent() throws Exception {
		//given
		GlobalPropertyPortletController portletController = new GlobalPropertyPortletController();
		Map<String, Object> model = new HashMap<String, Object>();
		
		//when
		String forModule = "file";
		model.put("forModule", forModule);
		
		//then
		portletController.setupModelForModule(model);
		Assertions.assertEquals(forModule + ".", model.get("propertyPrefix"));
		Assertions.assertEquals("true", model.get("hidePrefix"));
		Assertions.assertEquals(forModule + ".started;" + forModule + ".mandatory", model.get("excludePrefix"));
	}
	
	/**
	 * @see GlobalPropertyPortletController#setupModelForModule(Map)
	 */
	@Test
	public void setupModelForModule_shouldNotChangeModeIfForModuleIsNotPresent() throws Exception {
		//given
		GlobalPropertyPortletController portletController = new GlobalPropertyPortletController();
		Map<String, Object> model = new HashMap<String, Object>();
		
		//when
		
		//then
		portletController.setupModelForModule(model);
		Assertions.assertNull(model.get("propertyPrefix"));
		Assertions.assertNull(model.get("hidePrefix"));
		Assertions.assertNull(model.get("excludePrefix"));
	}
	
	/**
	 * @see GlobalPropertyPortletController#setupModelForModule(Map)
	 */
	@Test
	public void setupModelForModule_shouldNotOverrideExcludePrefixButConcatenate() throws Exception {
		//given
		GlobalPropertyPortletController portletController = new GlobalPropertyPortletController();
		Map<String, Object> model = new HashMap<String, Object>();
		
		//when
		String forModule = "file";
		String excludePrefix = "file.custom";
		model.put("forModule", forModule);
		model.put("excludePrefix", excludePrefix);
		
		//then
		portletController.setupModelForModule(model);
		Assertions.assertEquals(excludePrefix + ";" + forModule + ".started;" + forModule + ".mandatory", model
		        .get("excludePrefix"));
	}
}
