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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Contains tests for the SchedulerFormController
 */
public class SchedulerFormControllerTest extends BaseModuleWebContextSensitiveTest {
    
    private static final String DATE_TIME_FORMAT = "MM/dd/yyyy HH:mm:ss";
    
    private static final String INITIAL_SCHEDULER_TASK_CONFIG_XML = "org/openmrs/web/include/SchedulerFormControllerTest.xml";
    
    private static final long MAX_WAIT_TIME_IN_MILLISECONDS = 2048;
    
    private MockHttpServletRequest mockRequest;
    
    private TaskHelper taskHelper;
    
    @Autowired
    private SchedulerFormController controller;
    
    // should be @Autowired as well but the respective bean is commented out
    // in applicationContext-service.xml at the time of coding (Jan 2013)
    private SchedulerService service;
    
    @BeforeEach
    public void setUpSchedulerService() throws Exception {
        executeDataSet(INITIAL_SCHEDULER_TASK_CONFIG_XML);
        
        this.service = Context.getSchedulerService();
        this.taskHelper = new TaskHelper(this.service);
        
        this.mockRequest = new MockHttpServletRequest();
        this.mockRequest.setMethod("POST");
        this.mockRequest.setParameter("action", "");
        this.mockRequest.setParameter("taskId", "1");
    }
    
    /**
     * @see SchedulerFormController#saveTask(HttpServletRequest,TaskDefinition)
     */
    @Test
    public void onSubmit_shouldRescheduleACurrentlyScheduledTask() throws Exception {
        Date timeOne = this.taskHelper.getTime(Calendar.MINUTE, 5);
        TaskDefinition task = this.taskHelper.getScheduledTaskDefinition(timeOne);
        Task oldTaskInstance = task.getTaskInstance();
        
        Date timeTwo = this.taskHelper.getTime(Calendar.MINUTE, 2);
        this.mockRequest.setParameter("startTime", new SimpleDateFormat(DATE_TIME_FORMAT).format(timeTwo));
        task.setStartTime(timeTwo);
        
        BindingResult errors = new BindException(task, this.mockRequest.getParameter("taskId"));
        ModelAndView mav = this.controller.processSubmit(this.mockRequest, task, errors);

        assertNotNull(mav);
        assertTrue(mav.getModel().isEmpty());
        
        Assertions.assertNotSame(oldTaskInstance, task.getTaskInstance());
    }
    
    /**
     * @see SchedulerFormController#saveTask(HttpServletRequest,TaskDefinition)
     */
    @Test
    public void onSubmit_shouldNotRescheduleATaskThatIsNotCurrentlyScheduled() throws Exception {
        Date timeOne = this.taskHelper.getTime(Calendar.MINUTE, 5);
        TaskDefinition task = this.taskHelper.getUnscheduledTaskDefinition(timeOne);
        Task oldTaskInstance = task.getTaskInstance();
        
        Date timeTwo = this.taskHelper.getTime(Calendar.MINUTE, 2);
        this.mockRequest.setParameter("startTime", new SimpleDateFormat(DATE_TIME_FORMAT).format(timeTwo));
        task.setStartTime(timeTwo);
        
        BindingResult errors = new BindException(task, this.mockRequest.getParameter("taskId"));
        ModelAndView mav = this.controller.processSubmit(this.mockRequest, task, errors);

        assertNotNull(mav);
        assertTrue(mav.getModel().isEmpty());
        
        Assertions.assertSame(oldTaskInstance, task.getTaskInstance());
    }
    
    /**
     * @see SchedulerFormController#saveTask(HttpServletRequest,TaskDefinition)
     */
    @Test
    public void onSubmit_shouldNotRescheduleATaskIfTheStartTimeHasPassed() throws Exception {
        Date timeOne = this.taskHelper.getTime(Calendar.MINUTE, 5);
        TaskDefinition task = this.taskHelper.getScheduledTaskDefinition(timeOne);
        Task oldTaskInstance = task.getTaskInstance();
        
        Date timeTwo = this.taskHelper.getTime(Calendar.SECOND, -1);
        this.mockRequest.setParameter("startTime", new SimpleDateFormat(DATE_TIME_FORMAT).format(timeTwo));
        task.setStartTime(timeTwo);
        
        BindingResult errors = new BindException(task, this.mockRequest.getParameter("taskId"));
        ModelAndView mav = this.controller.processSubmit(this.mockRequest, task, errors);
        
        assertNotNull(mav);
        assertTrue(mav.getModel().isEmpty());
        
        Assertions.assertSame(oldTaskInstance, task.getTaskInstance());
    }
    
    /**
     * @see SchedulerFormController#saveTask(HttpServletRequest,TaskDefinition)
     */
    @Test
    public void onSubmit_shouldNotRescheduleAnExecutingTask() throws Exception {
        Date startTime = this.taskHelper.getTime(Calendar.SECOND, 1);
        TaskDefinition task = this.taskHelper.getScheduledTaskDefinition(startTime);
        
        this.taskHelper.waitUntilTaskIsExecuting(task, MAX_WAIT_TIME_IN_MILLISECONDS);
        Task oldTaskInstance = task.getTaskInstance();
        
        // use the *same* start time as in the task already running
        this.mockRequest.setParameter("startTime", new SimpleDateFormat(DATE_TIME_FORMAT).format(startTime));
        task.setStartTime(startTime);
        
        BindingResult errors = new BindException(task, this.mockRequest.getParameter("taskId"));
        ModelAndView mav = this.controller.processSubmit(this.mockRequest, task, errors);
        
        assertNotNull(mav);
        assertTrue(mav.getModel().isEmpty());
        
        Assertions.assertSame(oldTaskInstance, task.getTaskInstance());
        deleteAllData();
    }
    
    /**
     * @see SchedulerFormController#processSubmit(HttpServletRequest,TaskDefinition,Object,BindingResult)
     * @verifies not throw null pointer exception if repeat interval is null
     */
    @Test
    public void processFormSubmission_shouldNotThrowNullPointerExceptionIfRepeatIntervalIsNull() throws Exception {
        Date startTime = this.taskHelper.getTime(Calendar.SECOND, 2);
        TaskDefinition task = this.taskHelper.getScheduledTaskDefinition(startTime);
        
        this.mockRequest.setParameter("startTime", new SimpleDateFormat(DATE_TIME_FORMAT).format(startTime));
        this.mockRequest.setParameter("repeatInterval", " ");
        this.mockRequest.setParameter("repeatIntervalUnits", "minutes");
        task.setRepeatInterval(null);
        
        BindingResult errors = new BindException(task, this.mockRequest.getParameter("taskId"));
        ModelAndView mav = this.controller.processSubmit(this.mockRequest, task, errors);
        
        assertNotNull(mav);
        Long interval = 0L;
        Assertions.assertEquals(interval, task.getRepeatInterval());
    }
    
}