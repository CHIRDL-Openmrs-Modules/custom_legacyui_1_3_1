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

import java.util.Map;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.ui.ModelMap;

/**
 * Tests the {@link SystemInformationController} controller
 */
public class SystemInformationControllerTest extends BaseModuleWebContextSensitiveTest {
	
	private ModelMap model = null;
	
	@BeforeEach
	public void before() throws Exception {
		createController();
	}
	
	/**
	 * Creates the controller with necessary parameters
	 */
	private void createController() {
		model = new ModelMap();
		SystemInformationController controller = new SystemInformationController();
		controller.showPage(model);
		//System.out.println("SystemInformationControllerTest.createController() "+model.toString());
	}
	
	/**
	 * @see SystemInformationController#showPage(ModelMap)
	 */
	@Test
	public void showPage_shouldReturnOpenmrsInformation() {
	    Assertions.assertTrue(((Map<String, Map<String, String>>) model.get("systemInfo"))
		        .containsKey("SystemInfo.title.openmrsInformation"));
	}
	
	/**
	 * @see SystemInformationController#showPage(ModelMap)
	 */
	@Test
	public void showPage_shouldReturnUserInformation() {
	    Assertions.assertTrue(((Map<String, Map<String, String>>) model.get("systemInfo"))
		        .containsKey("SystemInfo.title.javaRuntimeEnvironmentInformation"));
	}
	
	/**
	 * @see SystemInformationController#showPage(ModelMap)
	 */
	@Test
	public void showPage_shouldReturnAllJavaRuntimeInformation() {
	    Assertions.assertTrue(((Map<String, Map<String, String>>) model.get("systemInfo"))
		        .containsKey("SystemInfo.title.moduleInformation"));
	}
	
	/**
	 * @see SystemInformationController#showPage(ModelMap)
	 */
	@Test
	public void showPage_shouldReturnAllDatabaseInformation() {
	    Assertions.assertTrue(((Map<String, Map<String, String>>) model.get("systemInfo"))
		        .containsKey("SystemInfo.title.dataBaseInformation"));
	}
	
	/**
	 * @see SystemInformationController#showPage(ModelMap)
	 */
	@Test
	public void getMemoryInformation_shouldReturnMemoryInformation() {
	    Assertions.assertTrue(((Map<String, Map<String, String>>) model.get("systemInfo"))
		        .containsKey("SystemInfo.title.memoryInformation"));
	}
	
}
