/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.patient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.web.controller.patient.PatientDashboardGraphController;
import org.openmrs.web.controller.patient.PatientGraphData;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.ui.ModelMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

/**
 * Test for graphs on the patient dashboard
 */
public class PatientDashboardGraphControllerTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * Test getting a concept by name and by partial name.
	 * 
	 * @see PatientDashboardGraphController#showGraphData(Integer, Integer, ModelMap)
	 */
	@Test
	public void shouldReturnJSONWithPatientObservationDetails() throws Exception {
		executeDataSet("org/openmrs/api/include/ObsServiceTest-initial.xml");
		PatientDashboardGraphController controller = new PatientDashboardGraphController();
		
		long firstObsDate = new GregorianCalendar(2006, Calendar.FEBRUARY, 9).getTimeInMillis();
		long secondObsDate = new GregorianCalendar(2006, Calendar.FEBRUARY, 10).getTimeInMillis();
		
		ModelMap map = new ModelMap();
		controller.showGraphData(2, 1, map);
		PatientGraphData graph = (PatientGraphData) map.get("graph");
		
		String expectedData = String
		        .format(
		            "{\"normal\":{\"high\":null,\"low\":null},\"data\":[[1139547600000,null],[1139547600000,null],[1139461200000,1.0]],\"critical\":{\"high\":null,\"low\":null},\"absolute\":{\"high\":50.0,\"low\":2.0},\"name\":\"Some concept name\",\"units\":\"\"}",
		            secondObsDate, firstObsDate);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode expectedJson = mapper.readTree(expectedData);
		JsonNode actualJson = mapper.readTree(graph.toString());
		
		Assertions.assertEquals(expectedJson.size(), actualJson.size());
		for (Iterator<String> fieldNames = expectedJson.fieldNames(); fieldNames.hasNext();) {
			String field = fieldNames.next();
			Assertions.assertIterableEquals(expectedJson.get(field), actualJson.get(field));
		}
	}
	
	/**
	 * Test the path of the form for rendering the json data
	 * 
	 * @see PatientDashboardGraphController#showGraphData(Integer, Integer, ModelMap)
	 */
	@Test
	public void shouldDisplayPatientDashboardGraphForm() throws Exception {
		executeDataSet("org/openmrs/api/include/ObsServiceTest-initial.xml");
		Assertions.assertEquals("module/legacyui/patientGraphJsonForm", new PatientDashboardGraphController()
		        .showGraphData(2, 1, new ModelMap()));
	}
}
