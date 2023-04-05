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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/scheduler/scheduler.list")
public class SchedulerListController {

    private static final String FORM_VIEW = "/module/legacyui/admin/scheduler/schedulerList";
    private static final String SUBMIT_VIEW = "scheduler.list";

    /**
     * Logger for this class and subclasses
     */
    private static final Logger log = LoggerFactory.getLogger(SchedulerListController.class);

    @InitBinder
    protected void initBinder(WebDataBinder binder){
        binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
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
    public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("taskList") Collection<TaskDefinition> tasks, BindingResult errors) throws Exception {

        HttpSession httpSession = request.getSession();
        String view = FORM_VIEW;
        StringBuilder success = new StringBuilder();
        StringBuilder error = new StringBuilder();
        String action = request.getParameter("action");
        MessageSourceService mss = Context.getMessageSourceService();

        String[] taskList = request.getParameterValues("taskId");

        SchedulerService schedulerService = Context.getSchedulerService();

        if (taskList != null) {

            for (String taskId : taskList) {

                // Argument to pass to the success/error message
                Object[] args = new Object[] { taskId };

                try {

                    TaskDefinition task = schedulerService.getTask(Integer.valueOf(taskId));

                    // If we can get the name, let's use it
                    if (task != null) {
                        args = new Object[] { task.getName() };
                    }

                    if (action.equals(mss.getMessage("Scheduler.taskList.delete"))) {
                        if (task != null && !task.getStarted()) {
                            schedulerService.deleteTask(Integer.valueOf(taskId));
                            success.append(mss.getMessage("Scheduler.taskList.deleted", args, Context.getLocale()));
                        } else {
                            error.append(mss.getMessage("Scheduler.taskList.deleteNotAllowed", args, Context.getLocale()));
                        }
                    } else if (action.equals(mss.getMessage("Scheduler.taskList.stop"))) {
                        schedulerService.shutdownTask(task);
                        success.append(mss.getMessage("Scheduler.taskList.stopped", args, Context.getLocale()));
                    } else if (action.equals(mss.getMessage("Scheduler.taskList.start"))) {
                        schedulerService.scheduleTask(task);
                        success.append(mss.getMessage("Scheduler.taskList.started", args, Context.getLocale()));
                    }
                }
                catch (APIException e) {
                    log.warn("Error processing schedulerlistcontroller task", e);
                    error.append(mss.getMessage("Scheduler.taskList.error", args, Context.getLocale()));
                }
                catch (SchedulerException ex) {
                    log.error("Error processing schedulerlistcontroller task", ex);
                    error.append(mss.getMessage("Scheduler.taskList.error", args, Context.getLocale()));
                }
            }
        } else {
            error.append(mss.getMessage("Scheduler.taskList.requireTask"));
        }

        view = SUBMIT_VIEW;

        if (!"".equals(success.toString())) {
            httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success.toString());
        }
        if (!"".equals(error.toString())) {
            httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error.toString());
        }

        return new ModelAndView(new RedirectView(view));
    }

    @ModelAttribute("taskList")
    protected Object formBackingObject() {

        // Get all tasks that are available to be executed
        return Context.getSchedulerService().getRegisteredTasks();
    }

    @GetMapping
    public String initForm(ModelMap map, @ModelAttribute("taskList") Collection<TaskDefinition> tasks) {
        
        Map<TaskDefinition, String> intervals = new HashMap<>();
        MessageSourceService mss = Context.getMessageSourceService();

        for (TaskDefinition task : tasks) {

            Long interval = task.getRepeatInterval();

            if (interval < 60) {
                intervals.put(task, interval + " " + mss.getMessage("Scheduler.scheduleForm.repeatInterval.units.seconds"));
            } else if (interval < 3600) {
                intervals.put(task, interval / 60 + " "
                        + mss.getMessage("Scheduler.scheduleForm.repeatInterval.units.minutes"));
            } else if (interval < 86400) {
                intervals.put(task, interval / 3600 + " "
                        + mss.getMessage("Scheduler.scheduleForm.repeatInterval.units.hours"));
            } else {
                intervals.put(task, interval / 86400 + " "
                        + mss.getMessage("Scheduler.scheduleForm.repeatInterval.units.days"));
            }
        }
        map.put("intervals", intervals);

        return FORM_VIEW;
    }

}
