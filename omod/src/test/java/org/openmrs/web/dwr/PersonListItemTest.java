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

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

/**
 * Tests the {@link PersonListItem} class.
 */
public class PersonListItemTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see PersonListItem#PersonListItem(Person, String)
	 */
	@Test
	public void PersonListItem_shouldIdentifyBestMatchingNameForTheFamilyName() throws Exception {
		
		PersonListItem listItem = new PersonListItem(Context.getPersonService().getPerson(2), "hornblower3");
		Assertions.assertEquals("Hornblower3", listItem.getFamilyName());
		Assertions.assertEquals("John", listItem.getGivenName());
		Assertions.assertEquals("Peeter", listItem.getMiddleName());
	}
	
	/**
	 * @see PersonListItem#PersonListItem(Person, String)
	 */
	@Test
	public void PersonListItem_shouldIdentifyBestMatchingNameForTheGivenPreferredNameEvenIfOtherNamesMatch()
	        throws Exception {
		
		PersonListItem listItem = new PersonListItem(Context.getPersonService().getPerson(2), "Horatio");
		Assertions.assertEquals("Hornblower", listItem.getFamilyName());
		Assertions.assertEquals("Horatio", listItem.getGivenName());
		Assertions.assertEquals("Test", listItem.getMiddleName());
	}
	
	/**
	 * @see PersonListItem#PersonListItem(Person, String)
	 */
	@Test
	public void PersonListItem_shouldIdentifyBestMatchingNameAsOtherNameForTheMiddleName() throws Exception {
		
		PersonListItem listItem = new PersonListItem(Context.getPersonService().getPerson(2), "Peeter");
		Assertions.assertEquals("Hornblower2", listItem.getFamilyName());
		Assertions.assertEquals("Horatio", listItem.getGivenName());
		Assertions.assertEquals("Peeter", listItem.getMiddleName());
	}
	
	/**
	 * @see PersonListItem#PersonListItem(Person, String)
	 */
	@Test
	public void PersonListItem_shouldIdentifyBestMatchingNameAsOtherNameForTheGivenName() throws Exception {
		
		PersonListItem listItem = new PersonListItem(Context.getPersonService().getPerson(2), "joh");
		Assertions.assertEquals("Hornblower3", listItem.getFamilyName());
		Assertions.assertEquals("John", listItem.getGivenName());
		Assertions.assertEquals("Peeter", listItem.getMiddleName());
	}
	
	/**
	 * @see PersonListItem#PersonListItem(Person, String)
	 */
	@Test
	public void PersonListItem_shouldIdentifyBestMatchingNameInMultipleSearchNames() throws Exception {
		
		PersonListItem listItem = new PersonListItem(Context.getPersonService().getPerson(2), "Horn peet john");
		Assertions.assertEquals("Hornblower3", listItem.getFamilyName());
		Assertions.assertEquals("John", listItem.getGivenName());
		Assertions.assertEquals("Peeter", listItem.getMiddleName());
	}
	
	/**
	 * @see PersonListItem#PersonListItem(Person)
	 */
	@Test
	public void PersonListItem_shouldPutAttributeToStringValueIntoAttributesMap() throws Exception {
		PersonListItem listItem = PersonListItem.createBestMatch(Context.getPersonService().getPerson(2));
		
		for (Map.Entry<String, String> entry : listItem.getAttributes().entrySet()) {
			if (entry.getKey().equals("Civil Status")) {
			    Assertions.assertEquals("MARRIED", entry.getValue()); // should be string not conceptId
				return; // quit after we test the first one
			}
		}
		
		// make sure we found at least one attr
		Assertions.fail("No civil status person attribute was defined");
	}
	
	/**
	 * @see PersonListItem#createBestMatch(Person)
	 */
	@Test
	@SuppressWarnings("unused")
	public void createBestMatch_shouldReturnPatientListItemGivenPatientParameter() throws Exception {
		PatientListItem listItem = (PatientListItem) PersonListItem.createBestMatch(Context.getPersonService().getPerson(2));
	}
	
	/**
	 * @see PersonListItem#createBestMatch(Person)
	 */
	@Test
	public void createBestMatch_shouldReturnPersonListItemGivenPersonParameter() throws Exception {
		PersonListItem listItem = PersonListItem.createBestMatch(Context.getPersonService().getPerson(2));
		Assertions.assertTrue(listItem instanceof PersonListItem);
	}
	
}
