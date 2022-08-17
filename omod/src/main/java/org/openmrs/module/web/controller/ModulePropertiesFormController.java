/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.ModuleConstants;
import org.openmrs.module.ModuleUtil;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/modules/moduleProperties.form")
public class ModulePropertiesFormController {

    private static final String FORM_VIEW = "/module/legacyui/admin/modules/modulePropertiesForm";
    private static final String SUBMIT_VIEW = "moduleProperties.form";
    
    /**
     * Handles the submission of the Module Properties form.
     *
     * @param request The HTTP request information
     * @return The name of the next view
     */
    @PostMapping
    public ModelAndView processSubmit(HttpServletRequest request) throws Exception {
        
        if (!Context.hasPrivilege(PrivilegeConstants.MANAGE_MODULES)) {
            throw new APIAuthenticationException("Privilege required: " + PrivilegeConstants.MANAGE_MODULES);
        }
        
        HttpSession httpSession = request.getSession();
        String view = SUBMIT_VIEW;
        String success = "";
        String error = ""; 
        
        if (!"".equals(success)) {
            httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success);
        }
        
        if (!"".equals(error)) {
            httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error);
        }
        
        return new ModelAndView(new RedirectView(view));
    }
    
    
    @ModelAttribute("moduleProperties")
    protected Object formBackingObject() {
        return "not used";
    }

    @GetMapping
    public String initForm(ModelMap map) {
        MessageSourceService mss = Context.getMessageSourceService();

        map.put("allowUpload", ModuleUtil.allowAdmin().toString());
        map.put("disallowUploads", mss.getMessage("Module.disallowUploads",
            new String[] { ModuleConstants.RUNTIMEPROPERTY_ALLOW_UPLOAD }, Context.getLocale()));

        return FORM_VIEW;
    }

}
