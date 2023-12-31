/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.scheduler.web.controller;

import static org.junit.Assert.assertNotNull;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

public class SchedulerFormTest extends BaseModuleWebContextSensitiveTest {
    
    private TaskDefinition def;
    
    @Autowired
    private SchedulerFormController controller;
    
    @BeforeEach
    public void setUpSchedulerService() throws Exception {
        
        this.def = new TaskDefinition();
        this.def.setStartOnStartup(false);
        this.def.setId(1);
        this.def.setName("TestTask");
        this.def.setRepeatInterval(3600000L);
        this.def.setTaskClass("org.openmrs.scheduler.tasks.TestTask");
        this.def.setStartTime(Calendar.getInstance().getTime());
    }
    
    /**
     * See TRUNK-3970: Error when adding a task in version 1.9.3
     * https://tickets.openmrs.org/browse/TRUNK-3970
     */
    @Test
    public void addANewTaskShouldNotError() throws Exception {
        
        HttpServletRequest request = new MockHttpServletRequest("GET", "/openmrs/admin/scheduler/scheduler.form");
        BindingResult errors = new BindException(this.def, "TestTask");
        ModelAndView mav = this.controller.processSubmit(request, this.def, errors);
        
        assertNotNull(mav);
    }
}
