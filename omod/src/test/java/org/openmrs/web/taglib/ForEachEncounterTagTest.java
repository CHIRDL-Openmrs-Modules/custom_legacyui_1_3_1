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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockPageContext;

/**
 * Contains tests for the {@link ForEachEncounterTag}
 */
public class ForEachEncounterTagTest extends BaseModuleWebContextSensitiveTest {
	
	/**
	 * @see ForEachEncounterTag#doStartTag()
	 * @regression TRUNK-2465
	 */
	@Test
	public void doStartTag_shouldSortEncountersByEncounterDatetimeInDescendingOrder() throws Exception {
		int num = 3;
		executeDataSet("org/openmrs/web/taglib/include/ForEachEncounterTagTest.xml");
		Patient patient = Context.getPatientService().getPatient(7);
		List<Encounter> encounters = Context.getEncounterService().getEncountersByPatient(patient);
		ForEachEncounterTag tag = new ForEachEncounterTag();
		tag.setPageContext(new MockPageContext());
		tag.setDescending(true);
		tag.setEncounters(encounters);
		tag.setVar("enc");
		tag.setNum(num);
		// the tag passes
		Assertions.assertEquals(BodyTag.EVAL_BODY_BUFFERED, tag.doStartTag());
		//the match count should not exceed the limit
		Assertions.assertTrue(num >= tag.matchingEncs.size());
		//check the sorting
		Assertions.assertEquals(11, tag.matchingEncs.get(0).getId().intValue());
		Assertions.assertEquals(16, tag.matchingEncs.get(1).getId().intValue());
		Assertions.assertEquals(7, tag.matchingEncs.get(2).getId().intValue());
	}
	
	/**
	 * @see ForEachEncounterTag#doStartTag()
	 */
	@Test
	public void doStartTag_shouldPassForAPatientWithNoEncounters() throws Exception {
		ForEachEncounterTag tag = new ForEachEncounterTag();
		tag.setPageContext(new MockPageContext());
		tag.setEncounters(new ArrayList<Encounter>());
		// the tag passes
		Assertions.assertEquals(Tag.SKIP_BODY, tag.doStartTag());
	}
}
