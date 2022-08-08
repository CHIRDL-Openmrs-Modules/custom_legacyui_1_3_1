/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.taglib;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;

/**
 * Tests the {@link PortletTag}
 */
public class PortletTagTest {
	
	/**
	 * @see org.openmrs.web.taglib.PortletTag#generatePortletUrl(String, String)
	 */
	@Test
	public void getModulePortletUrl_shouldReturnTheCorrectUrlForACorePortlet() throws Exception {
		String portletUrl = "test.portlet";
		String moduleId = null;
		
		// Instantiate the portlet and generate the url
		PortletTag portlet = new PortletTag();
		String result = portlet.generatePortletUrl(portletUrl, moduleId);
		
		// Verify the portlet url
		assertEquals("/portlets/" + portletUrl, result);
	}
	
	/**
	 * @see org.openmrs.web.taglib.PortletTag#generatePortletUrl(String, String)
	 */
	@Test
	public void getModulePortletUrl_shouldReturnTheCorrectUrlForAModulePortlet() throws Exception {
		String portletUrl = "test.portlet";
		String moduleId = "moduleId";
		
		Module module = new Module(moduleId, moduleId, portletUrl, "author", "description", "1.0");
		ModuleFactory.loadModule(module, Boolean.FALSE);
		assertEquals(module, ModuleFactory.getModuleById(moduleId));
        
        // Instantiate the portlet and get the module url
        PortletTag portlet = new PortletTag();
        String result = portlet.generatePortletUrl(portletUrl, moduleId);
        
        // Verify the portlet url
        assertEquals("/module/" + moduleId + "/portlets/" + portletUrl, result);
	}
	
	/**
	 * @see org.openmrs.web.taglib.PortletTag#generatePortletUrl(String, String)
	 */
	@Test
	public void getModulePortletUrl_shouldReplacePeriodInAModuleIdWithAForwardSlashWhenBuildingAModulePortletUrl() {
		String portletUrl = "test.portlet";
		String moduleId = "module.id.";
		
		Module module = new Module(moduleId, moduleId, portletUrl, "author", "description", "1.0");
		ModuleFactory.loadModule(module, Boolean.FALSE);
		assertEquals(module, ModuleFactory.getModuleById(moduleId));
            
        PortletTag portlet = new PortletTag();
        String result = portlet.generatePortletUrl(portletUrl, moduleId);
        
        assertEquals("/module/" + moduleId.replace('.', '/') + "/portlets/" + portletUrl, result);
	}
	
	/**
	 * @see org.openmrs.web.taglib.PortletTag#generatePortletUrl(String, String)
	 */
	@Test
	public void getModulePortletUrl_shouldNotUpdateTheModuleIdFieldForAModulePortlet() {
		String portletUrl = "test.portlet";
		String moduleId = "module.id";
		
		Module module = new Module(moduleId, moduleId, portletUrl, "author", "description", "1.0");
		ModuleFactory.loadModule(module, Boolean.FALSE);
		assertEquals(module, ModuleFactory.getModuleById(moduleId));
        
        PortletTag portlet = new PortletTag();
        portlet.setModuleId(moduleId);
        String result = portlet.generatePortletUrl(portletUrl, moduleId);
        
        assertEquals("/module/" + moduleId.replace('.', '/') + "/portlets/" + portletUrl, result);
        assertEquals(moduleId, portlet.getModuleId());
	}
	
	/**
	 * @see org.openmrs.web.taglib.PortletTag#generatePortletUrl(String, String)
	 */
	@Test
	public void getModulePortletUrl_shouldReturnACorePortletUrlWhenTheSpecifiedModuleCannotBeFound() {
		String portletUrl = "test.portlet";
		String moduleId = "moduleId.";
		
		// Setup the mocking for ModuleFactory to return null to test when the module is not found
		assertNull(ModuleFactory.getModuleById(moduleId));
            
        PortletTag portlet = new PortletTag();
        String result = portlet.generatePortletUrl(portletUrl, moduleId);
                
        assertEquals("/portlets/" + portletUrl, result);
	}
	
	/**
	 * @see org.openmrs.web.taglib.PortletTag#generatePortletUrl(String, String)
	 */
	@Test
	public void getModulePortletUrl_shouldAppendDotPortletToTheUrlIfNotSpecified() {
		String portletUrl = "test";
		String moduleId = null;
		
		PortletTag portlet = new PortletTag();
		String result = portlet.generatePortletUrl(portletUrl, moduleId);
		
		assertEquals("/portlets/" + portletUrl + ".portlet", result);
	}
	
	/**
	 * @see org.openmrs.web.taglib.PortletTag#generatePortletUrl(String, String)
	 */
	@Test
	public void getModulePortletUrl_shouldTreatBothAnEmptyAndNullModuleIdAsCorePortlets() {
		String portletUrl = "test.portlet";
		String moduleId = null;
		
		PortletTag portlet = new PortletTag();
		
		// Test with a null module id
		String result = portlet.generatePortletUrl(portletUrl, moduleId);
		assertEquals("/portlets/" + portletUrl, result);
		
		// Test with an empty module id
		moduleId = "";
		result = portlet.generatePortletUrl(portletUrl, moduleId);
		assertEquals("/portlets/" + portletUrl, result);
	}
}
