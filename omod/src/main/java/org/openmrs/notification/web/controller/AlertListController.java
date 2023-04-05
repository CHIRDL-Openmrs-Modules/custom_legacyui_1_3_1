/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.notification.web.controller;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.notification.Alert;
import org.openmrs.notification.AlertService;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
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
@RequestMapping(value = "admin/users/alert.list")
public class AlertListController {

    private static final String FORM_VIEW = "/module/legacyui/admin/users/alertList";
    private static final String SUBMIT_VIEW = "alert.list";
    
    /**
     * Allows for Integers to be used as values in input tags. Normally, only strings and lists are
     * expected
     *
     * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
     *      org.springframework.web.bind.ServletRequestDataBinder)
     */
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
    }

    /**
     * The onSubmit function receives the form/command object that was modified by the input form
     * and saves it to the db
     *
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object,
     *      org.springframework.validation.BindException)
     */
    @PostMapping
    public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("alert") Alert alert, BindingResult errors) throws Exception {
        
        HttpSession httpSession = request.getSession();

        Locale locale = Context.getLocale();

        if (Context.isAuthenticated()) {
            AlertService as = Context.getAlertService();

            MessageSourceService mss = Context.getMessageSourceService();
            String msg = "";

            // expire the selected alerts
            String[] alertIds = request.getParameterValues("alertId");
            if (alertIds != null) {
                for (String alertIdString : alertIds) {
                    Integer alertId = Integer.parseInt(alertIdString);
                    Alert a = as.getAlert(alertId);
                    a.setDateToExpire(new Date());
                    as.saveAlert(a);
                }

                msg = mss.getMessage("Alert.expired", new Object[] { alertIds.length }, locale);
            } else {
                msg = mss.getMessage("Alert.select");
            }

            httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, msg);
            return new ModelAndView(new RedirectView(SUBMIT_VIEW));
        }

        // The user isn't authenticated or their session has expired
        return ShowFormUtil.showForm(errors, FORM_VIEW);
    }
    
    /**
     * This is called prior to displaying a form for the first time. It tells Spring the
     * form/command object to load into the request
     *
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @ModelAttribute("alertList")
    protected Object formBackingObject(HttpServletRequest request) {

        //map containing the privilege and true/false whether the privilege is core or not
        List<Alert> alertList = new Vector<>();

        //only fill the list if the user has authenticated properly
        if (Context.isAuthenticated()) {
            AlertService as = Context.getAlertService();
            boolean b = new Boolean(request.getParameter("includeExpired"));
            alertList = as.getAllAlerts(b);
        }

        return alertList;
    }

    /**
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
     *      java.lang.Object, org.springframework.validation.Errors)
     */
    @GetMapping
    public String initForm(ModelMap map) throws Exception {

        map.put("today", new Date());

        return FORM_VIEW;
    }

}
