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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.context.Context;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.validator.SchedulerFormValidator;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/scheduler/scheduler.form")
public class SchedulerFormController implements ServletContextAware {

    private static final String FORM_VIEW = "/module/legacyui/admin/scheduler/schedulerForm";
    private static final String SUBMIT_VIEW = "scheduler.list";

    ServletContext servletContext;
    
    /**
     * Logger for this class and subclasses
     */
    private static final Logger log = LoggerFactory.getLogger(SchedulerFormController.class);
    
    // Move this to message.properties or OpenmrsConstants
    public static final String DEFAULT_DATE_PATTERN = "MM/dd/yyyy HH:mm:ss";
    
    @InitBinder
    protected void initBinder(WebDataBinder binder){
        binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
        binder.registerCustomEditor(java.lang.Long.class, new CustomNumberEditor(java.lang.Long.class, true));
        binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(new SimpleDateFormat(DEFAULT_DATE_PATTERN),
            true));
    }
    
    /**
     * Handles the submission of the Scheduler form.
     *
     * @param request The HTTP request information
     * @param task Task Definition
     * @param errors org.springframework.validation.BindingResult
     * @return The name of the next view
     */
    @PostMapping
    public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("task") TaskDefinition task, BindingResult errors) throws Exception {
        
        new SchedulerFormValidator().validate(task, errors);
        
        // assign the properties to the task
        String[] names = request.getParameterValues("propertyName");
        String[] values = request.getParameterValues("propertyValue");
        
        Map<String, String> properties = new HashMap<>();
        
        if (names != null) {
            for (int x = 0; x < names.length; x++) {
                if (names[x].length() > 0) {
                    properties.put(names[x], values[x]);
                }
            }
        }
        
        task.setProperties(properties);
        
        // if the user selected a different repeat interval unit, fix repeatInterval
        String units = request.getParameter("repeatIntervalUnits");
        Long interval = task.getRepeatInterval();
        if (interval != null) {
            if ("minutes".equals(units)) {
                interval = interval * 60;
            } else if ("hours".equals(units)) {
                interval = interval * 60 * 60;
            } else if ("days".equals(units)) {
                interval = interval * 60 * 60 * 24;
            }
            
            task.setRepeatInterval(interval);
        } else {
            task.setRepeatInterval(0L);
        }
        
        return processFormSubmission(request, task, errors);
    }
    
    
    protected ModelAndView processFormSubmission(HttpServletRequest request, TaskDefinition task,
            BindingResult errors) throws Exception {
        
        if (errors.hasErrors()) {
            if (log.isDebugEnabled()) {
                log.debug("Data binding errors: {}", errors.getErrorCount());
            }
            return new ModelAndView(FORM_VIEW);
        }
        log.debug("No errors -> processing submit");
        return saveTask(request, task);
        
    }
    
    /**
     * Saves task definition to the db
     *
     * @param request The HTTP request information
     * @param task Task Definition
     */

    public ModelAndView saveTask(HttpServletRequest request, TaskDefinition task) throws Exception {
        HttpSession httpSession = request.getSession();
        
        String view = FORM_VIEW;
        
        task.setStartTimePattern(DEFAULT_DATE_PATTERN);
        log.info("task started? {}", task.getStarted());
        
        //TODO Add unit test method to check that an executing task doesn't get rescheduled, it would require adding a test task
        //that runs for a period that spans beyond time it takes to execute all the necessary assertions in the test method
        
        //only reschedule a task if it is started, is not running and the time is not in the past
        if (task.getStarted() && OpenmrsUtil.compareWithNullAsEarliest(task.getStartTime(), new Date()) > 0
                && (task.getTaskInstance() == null || !task.getTaskInstance().isExecuting())) {
            Context.getSchedulerService().rescheduleTask(task);
        }
        Context.getSchedulerService().saveTaskDefinition(task);
        
        view = SUBMIT_VIEW;
        
        Object[] args = new Object[] { task.getName() };
        String success = Context.getMessageSourceService().getMessage("Scheduler.taskForm.saved", args, Context.getLocale());
        httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success);
        
        return new ModelAndView(new RedirectView(view));
    }
    
    
    @ModelAttribute("task")
    protected Object formBackingObject(HttpServletRequest request) {
        
        TaskDefinition task = new TaskDefinition();
        
        String taskId = request.getParameter("taskId");
        if (taskId != null) {
            task = Context.getSchedulerService().getTask(Integer.valueOf(taskId));
        }
        
        // Date format pattern for new and existing (currently disabled, but visible)
        if (task.getStartTimePattern() == null) {
            task.setStartTimePattern(DEFAULT_DATE_PATTERN);
        }
        
        return task;
    }

    @GetMapping
    public String initForm(ModelMap map, @ModelAttribute("task") TaskDefinition task) {
        
        Long interval = task.getRepeatInterval();
        if (interval == null) {
            interval = (long) 60;
        }
        Long repeatInterval;
        if (interval < 60) {
            map.put("units", "seconds");
            repeatInterval = interval;
        } else if (interval < 3600) {
            map.put("units", "minutes");
            repeatInterval = interval / 60;
        } else if (interval < 86400) {
            map.put("units", "hours");
            repeatInterval = interval / 3600;
        } else {
            map.put("units", "days");
            repeatInterval = interval / 86400;
        }
        
        map.put("repeatInterval", repeatInterval.toString());
        return FORM_VIEW;
    }
    
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

}