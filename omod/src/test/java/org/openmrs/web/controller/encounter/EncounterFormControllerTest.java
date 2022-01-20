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

import java.util.List;

import org.apache.struts.mock.MockHttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;

public class EncounterFormControllerTest extends BaseModuleWebContextSensitiveTest {
	
	protected static final String ENC_INITIAL_DATA_XML = "org/openmrs/api/include/EncounterServiceTest-initialData.xml";
	
	protected static final String TRANSFER_ENC_DATA_XML = "org/openmrs/api/include/EncounterServiceTest-transferEncounter.xml";
	
	/**
	 * @see EncounterFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Test
	public void onSubmit_shouldSaveANewEncounterRoleObject() throws Exception {
		executeDataSet(ENC_INITIAL_DATA_XML);
		executeDataSet(TRANSFER_ENC_DATA_XML);
		
		EncounterFormController controller = new EncounterFormController();
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("patientId", "201");
		
		Encounter encounter = Context.getEncounterService().getEncounter(200);
		
		Patient oldPatient = encounter.getPatient();
		Patient newPatient = Context.getPatientService().getPatient(201);
		Assertions.assertNotEquals(oldPatient, newPatient);
		
		List<Encounter> newEncounter = Context.getEncounterService().getEncountersByPatientId(newPatient.getPatientId());
		Assertions.assertEquals(0, newEncounter.size());
		
		BindException errors = new BindException(encounter, "encounterRole");
		
		controller.onSubmit(request, response, encounter, errors);
		
		Assertions.assertEquals(true, encounter.isVoided());
		newEncounter = Context.getEncounterService().getEncountersByPatientId(newPatient.getPatientId());
		Assertions.assertEquals(1, newEncounter.size());
		Assertions.assertEquals(false, newEncounter.get(0).isVoided());
	}
}
