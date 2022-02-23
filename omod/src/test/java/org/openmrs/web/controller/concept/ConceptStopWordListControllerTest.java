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

import java.util.List;

import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.ConceptStopWord;
import org.openmrs.web.WebConstants;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * Tests the {@link ConceptStopWordListController}
 */
public class ConceptStopWordListControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see ConceptStopWordListController#showForm(javax.servlet.http.HttpSession)
	 */
	@Test
	public void showForm_shouldReturnConceptStopWordListView() throws Exception {
		ConceptStopWordListController controller = (ConceptStopWordListController) applicationContext
		        .getBean("conceptStopWordListController");
		
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("POST");
		
		String showFormResult = controller.showForm(mockRequest.getSession());
		
		Assertions.assertNotNull(showFormResult);
		Assertions.assertEquals("admin/concepts/conceptStopWordList", showFormResult);
	}
	
	/**
	 * @see ConceptStopWordListController#showForm(javax.servlet.http.HttpSession)
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void showForm_shouldAddAllConceptStopWordsInSessionAttribute() throws Exception {
		ConceptStopWordListController controller = (ConceptStopWordListController) applicationContext
		        .getBean("conceptStopWordListController");
		
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setMethod("POST");
		
		controller.showForm(mockRequest.getSession());
		
		List<ConceptStopWord> conceptStopWordList = (List<ConceptStopWord>) mockRequest.getSession().getAttribute(
		    "conceptStopWordList");
		Assertions.assertNotNull(conceptStopWordList);
		Assertions.assertEquals(4, conceptStopWordList.size());
	}
	
	/**
	 * @see {@link ConceptStopWordListController#handleSubmission(javax.servlet.http.HttpSession, String[])
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void handleSubmission_shouldDeleteGivenConceptStopWordFromDB() throws Exception {
		ConceptStopWordListController controller = (ConceptStopWordListController) applicationContext
		        .getBean("conceptStopWordListController");
		
		HttpSession mockSession = new MockHttpSession();
		
		controller.handleSubmission(mockSession, new String[] { "1" });
		
		List<ConceptStopWord> conceptStopWordList = (List<ConceptStopWord>) mockSession.getAttribute("conceptStopWordList");
		Assertions.assertNotNull(conceptStopWordList);
		Assertions.assertEquals(3, conceptStopWordList.size());
	}
	
	/**
	 * @see {@link ConceptStopWordListController#handleSubmission(javax.servlet.http.HttpSession, String[])
	 */
	@Test
	public void handleSubmission_shouldAddTheDeleteSuccessMessageInSession() throws Exception {
		ConceptStopWordListController controller = (ConceptStopWordListController) applicationContext
		        .getBean("conceptStopWordListController");
		
		HttpSession mockSession = new MockHttpSession();
		
		controller.handleSubmission(mockSession, new String[] { "2" });
		
		String successMessage = (String) mockSession.getAttribute(WebConstants.OPENMRS_MSG_ATTR);
		String errorMessage = (String) mockSession.getAttribute(WebConstants.OPENMRS_ERROR_ATTR);
		
		Assertions.assertNotNull(successMessage);
		Assertions.assertNull(errorMessage);
		Assertions.assertEquals("general.deleted", successMessage);
	}
	
	/**
	 * @see {@link ConceptStopWordListController#handleSubmission(javax.servlet.http.HttpSession, String[])
	 */
	@Test
	public void handleSubmission_shouldAddTheDeleteErrorMessageInSession() throws Exception {
		ConceptStopWordListController controller = (ConceptStopWordListController) applicationContext
		        .getBean("conceptStopWordListController");
		
		HttpSession mockSession = new MockHttpSession();
		
		controller.handleSubmission(mockSession, new String[] { "1", "1" });
		
		String successMessage = (String) mockSession.getAttribute(WebConstants.OPENMRS_MSG_ATTR);
		String errorMessage = (String) mockSession.getAttribute(WebConstants.OPENMRS_ERROR_ATTR);
		
		Assertions.assertNotNull(successMessage);
		Assertions.assertNotNull(errorMessage);
		Assertions.assertEquals("ConceptStopWord.error.notfound", errorMessage);
	}
}
