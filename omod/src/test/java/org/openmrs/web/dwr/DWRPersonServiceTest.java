/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.dwr;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

/**
 * Test the different aspects of {@link DWRPersonService}
 */
public class DWRPersonServiceTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see DWRPersonService#findPeopleByRoles(String,null,String)
	 */
	@Test
	public void findPeopleByRoles_shouldMatchOnPatientIdentifiers() throws Exception {
		DWRPersonService dwrPersonService = new DWRPersonService();
		
		List<Object> persons = dwrPersonService.findPeopleByRoles("12345K", false, null);
		
		Assertions.assertEquals(1, persons.size());
		Assertions.assertEquals(new PersonListItem(6), persons.get(0));
	}
	
	/**
	 * @see DWRPersonService#findPeopleByRoles(String,null,String)
	 */
	@Test
	public void findPeopleByRoles_shouldAllowNullRolesParameter() throws Exception {
		new DWRPersonService().findPeopleByRoles("some string", false, null);
	}
	
}
