/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.concept;

import java.net.BindException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the {@link ConceptSourceFormController}
 */
public class ConceptSourceFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see ConceptSourceListController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldRetireConceptSource() throws Exception {
		ConceptService cs = Context.getConceptService();
		ConceptSourceFormController controller = (ConceptSourceFormController) applicationContext
		        .getBean("conceptSourceForm");
		
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("POST");
		mockRequest.setParameter("conceptSourceId", "3");
		mockRequest.setParameter("retireReason", "dummy reason for retirement");
		mockRequest.setParameter("retire", "dummy reason for retirement");
		
		controller.handleRequest(mockRequest, new MockHttpServletResponse());
		
		ConceptSource conceptSource = cs.getConceptSource(3);
		Assertions.assertTrue(conceptSource.isRetired());
		Assertions.assertEquals("dummy reason for retirement", conceptSource.getRetireReason());
	}
	
	/**
	 * @see ConceptSourceListController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldDeleteConceptSource() throws Exception {
		ConceptService cs = Context.getConceptService();
		ConceptSourceFormController controller = (ConceptSourceFormController) applicationContext
		        .getBean("conceptSourceForm");
		
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("POST");
		mockRequest.setParameter("conceptSourceId", "3");
		mockRequest.setParameter("purge", "dummy reason for deletion");
		
		controller.handleRequest(mockRequest, new MockHttpServletResponse());
		
		ConceptSource nullConceptSource = cs.getConceptSource(3);
		Assertions.assertNull(nullConceptSource);
	}
	
	/**
	 * @see ConceptSourceListController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldRestoreRetiredConceptSource() throws Exception {
		ConceptService cs = Context.getConceptService();
		ConceptSourceFormController controller = (ConceptSourceFormController) applicationContext
		        .getBean("conceptSourceForm");
		
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("POST");
		mockRequest.setParameter("conceptSourceId", "3");
		mockRequest.setParameter("retireReason", "dummy reason for retirement");
		mockRequest.setParameter("retire", "dummy reason for retirement");
		
		controller.handleRequest(mockRequest, new MockHttpServletResponse());
		
		ConceptSource conceptSource = cs.getConceptSource(3);
		Assertions.assertTrue(conceptSource.isRetired());
		Assertions.assertEquals("dummy reason for retirement", conceptSource.getRetireReason());
		
		MockHttpServletRequest restoreMockRequest = new MockHttpServletRequest();
		restoreMockRequest.setMethod("POST");
		restoreMockRequest.setParameter("conceptSourceId", "3");
		restoreMockRequest.setParameter("restore", "dummy reason for restoration");
		
		controller.handleRequest(restoreMockRequest, new MockHttpServletResponse());
		
		ConceptSource newConceptSource = cs.getConceptSource(3);
		Assertions.assertNotNull(newConceptSource, "Error, Object is null");
		Assertions.assertTrue(!newConceptSource.isRetired());
	}
}
