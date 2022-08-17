/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.form;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.FieldType;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.validator.FieldTypeValidator;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/forms/fieldType.form")
public class FieldTypeFormController {
	
	private static final String FORM_VIEW = "/module/legacyui/admin/forms/fieldTypeForm";
    private static final String SUBMIT_VIEW = "fieldType.list";
    
    /** Logger for this class and subclasses */
    private static final Logger log = LoggerFactory.getLogger(FieldTypeFormController.class);
    
	/**
	 * The onSubmit function receives the form/command object that was modified by the input form
	 * and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
    @PostMapping
    public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("fieldType") FieldType fieldType, BindingResult errors) throws Exception {
		
    	new FieldTypeValidator().validate(fieldType, errors);
    	
    	if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			
			return new ModelAndView(FORM_VIEW);
		}
    	
		HttpSession httpSession = request.getSession();
		
		String view = FORM_VIEW;
		
		if (Context.isAuthenticated()) {
			Context.getFormService().saveFieldType(fieldType);
			view = SUBMIT_VIEW;
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "FieldType.saved");
		}
		
		return new ModelAndView(new RedirectView(view));
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
    @ModelAttribute("fieldType")
    protected Object formBackingObject(HttpServletRequest request) {
		
		FieldType fieldType = null;
		
		if (Context.isAuthenticated()) {
			FormService rs = Context.getFormService();
			String fieldTypeId = request.getParameter("fieldTypeId");
			if (fieldTypeId != null) {
				fieldType = rs.getFieldType(Integer.valueOf(fieldTypeId));
			}
		}
		
		if (fieldType == null) {
			fieldType = new FieldType();
		}
		
		return fieldType;
	}
	
    @GetMapping
  	public String initForm() throws Exception {
  		return FORM_VIEW;
  	}
}
