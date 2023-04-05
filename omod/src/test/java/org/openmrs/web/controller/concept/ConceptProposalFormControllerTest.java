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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptProposal;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;

public class ConceptProposalFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	@Autowired
	ConceptProposalFormController controller;

	private MockMvc mockMvc;
	
	@BeforeEach
	public void runBeforeEachTest() throws Exception {
		this.mockMvc = MockMvcBuilders.standaloneSetup(this.controller).build();
	}
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
				
		this.mockMvc
		.perform(post("/admin/concepts/conceptProposal.form").param("action", "")
				.param("conceptProposalId", conceptproposalId.toString())
				.param("finalText", cp.getOriginalText())
				.param("conceptId", conceptToMap.getConceptId().toString())
				.param("conceptNamelocale", locale.toString())
				.param("actionToTake", "saveAsSynonym"))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("conceptProposal.*"))
		.andExpect(model().hasNoErrors());
		
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
		
		this.mockMvc
		.perform(post("/admin/concepts/conceptProposal.form").param("action", "")
				.param("conceptProposalId", conceptproposalId.toString())
				.param("finalText", cp.getOriginalText())
				.param("conceptId", conceptToMap.getConceptId().toString())
				.param("conceptNamelocale", locale.toString())
				.param("actionToTake", "saveAsSynonym"))
		.andExpect(status().isFound()).andExpect(redirectedUrlPattern("conceptProposal.*"))
		.andExpect(model().hasNoErrors());
		
		Assertions.assertEquals(cp.getOriginalText(), cp.getFinalText());
		Assertions.assertTrue(conceptToMap.hasName(cp.getOriginalText(), locale));
	}
}
