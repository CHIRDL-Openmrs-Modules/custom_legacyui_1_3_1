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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptProposal;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class ConceptProposalFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see ConceptProposalFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldCreateASingleUniqueSynonymAndObsForAllSimilarProposals() throws Exception {
		executeDataSet("org/openmrs/api/include/ConceptServiceTest-proposals.xml");
		
		ConceptService cs = Context.getConceptService();
		ObsService os = Context.getObsService();
		final Integer conceptproposalId = 5;
		ConceptProposal cp = cs.getConceptProposal(conceptproposalId);
		Concept obsConcept = cp.getObsConcept();
		Concept conceptToMap = cs.getConcept(5);
		Locale locale = Locale.ENGLISH;
		//sanity checks
		Assertions.assertFalse(conceptToMap.hasName(cp.getOriginalText(), locale));
		Assertions.assertEquals(0, os.getObservationsByPersonAndConcept(cp.getEncounter().getPatient(), obsConcept).size());
		List<ConceptProposal> proposals = cs.getConceptProposals(cp.getOriginalText());
		Assertions.assertEquals(5, proposals.size());
		for (ConceptProposal conceptProposal : proposals) {
		    Assertions.assertNull(conceptProposal.getObs());
		}
		
		// set up the controller
		ConceptProposalFormController controller = (ConceptProposalFormController) applicationContext
		        .getBean("conceptProposalForm");
		controller.setApplicationContext(applicationContext);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(new MockHttpSession(null));
		request.setMethod("POST");
		request.addParameter("conceptProposalId", conceptproposalId.toString());
		request.addParameter("finalText", cp.getOriginalText());
		request.addParameter("conceptId", conceptToMap.getConceptId().toString());
		request.addParameter("conceptNamelocale", locale.toString());
		request.addParameter("action", "");
		request.addParameter("actionToTake", "saveAsSynonym");
		
		HttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertNotNull(mav);
		assertTrue(mav.getModel().isEmpty());
		
		Assertions.assertEquals(cp.getOriginalText(), cp.getFinalText());
		Assertions.assertTrue(conceptToMap.hasName(cp.getOriginalText(), locale));
		Assertions.assertNotNull(cp.getObs());
		//Obs should have been created for the 2 proposals with same text, obsConcept but different encounters
		Assertions.assertEquals(2, os.getObservationsByPersonAndConcept(cp.getEncounter().getPatient(), obsConcept).size());
		
		//The proposal with a different obs concept should have been skipped
		proposals = cs.getConceptProposals(cp.getFinalText());
		Assertions.assertEquals(1, proposals.size());
		Assertions.assertEquals(21, proposals.get(0).getObsConcept().getConceptId().intValue());
	}
	
	/**
	 * @see ConceptProposalFormController#onSubmit(HttpServletRequest,HttpServletResponse,Object,BindException)
	 */
	@Test
	public void onSubmit_shouldWorkProperlyForCountryLocales() throws Exception {
		executeDataSet("org/openmrs/api/include/ConceptServiceTest-proposals.xml");
		
		ConceptService cs = Context.getConceptService();
		
		final Integer conceptproposalId = 5;
		ConceptProposal cp = cs.getConceptProposal(conceptproposalId);
		Concept conceptToMap = cs.getConcept(4);
		Locale locale = new Locale("en", "GB");
		
		Assertions.assertFalse(conceptToMap.hasName(cp.getOriginalText(), locale));
		
		ConceptProposalFormController controller = (ConceptProposalFormController) applicationContext
		        .getBean("conceptProposalForm");
		controller.setApplicationContext(applicationContext);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(new MockHttpSession(null));
		request.setMethod("POST");
		request.addParameter("conceptProposalId", conceptproposalId.toString());
		request.addParameter("finalText", cp.getOriginalText());
		request.addParameter("conceptId", conceptToMap.getConceptId().toString());
		request.addParameter("conceptNamelocale", locale.toString());
		request.addParameter("action", "");
		request.addParameter("actionToTake", "saveAsSynonym");
		
		HttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertNotNull(mav);
		assertTrue(mav.getModel().isEmpty());
		
		Assertions.assertEquals(cp.getOriginalText(), cp.getFinalText());
		Assertions.assertTrue(conceptToMap.hasName(cp.getOriginalText(), locale));
	}
}
