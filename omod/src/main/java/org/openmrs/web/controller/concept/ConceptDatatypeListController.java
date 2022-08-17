/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.concept;

import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.ConceptDatatype;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/concepts/conceptDatatype.list")
public class ConceptDatatypeListController {
	
	private static final String FORM_VIEW = "/admin/concepts/conceptDatatypeList";

	/**
	 * Set the name of the view that should be shown on successful submit.
	 */
	private static final String SUBMIT_VIEW = "conceptDatatype.list";
	
	/** Logger for this class and subclasses */
    private static final Logger log = LoggerFactory.getLogger(ConceptDatatypeListController.class);
	
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
	public ModelAndView processSubmit(HttpServletRequest request) throws Exception {
		
		HttpSession httpSession = request.getSession();
		
		String view = FORM_VIEW;
		if (Context.isAuthenticated()) {
			
			String[] cdList = request.getParameterValues("conceptDatatypeId");
			ConceptService cs = Context.getConceptService();
			
			StringBuilder success = new StringBuilder("");
			StringBuilder error = new StringBuilder();
			
			MessageSourceService mss = Context.getMessageSourceService();
			String deleted = mss.getMessage("general.deleted");
			String notDeleted = mss.getMessage("general.cannot.delete");
			if (cdList.length != 0) {
				log.warn("Deleting concept datatype is not supported");
				if (!"".equals(error.toString())) {
					error.append("<br/>");
				}
				error.append("ConceptDatatype").append(" ").append(notDeleted);
			}
			
			view = SUBMIT_VIEW;
			if (!"".equals(success.toString())) {
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success.toString());
			}
			if (!"".equals(error.toString())) {
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error.toString());
			}
		}
		
		return new ModelAndView(new RedirectView(view));
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
    @ModelAttribute("conceptDatatypeList")
	protected Object formBackingObject() {
		
		//default empty Object
		List<ConceptDatatype> cdList = new Vector<ConceptDatatype>();
		
		//only fill the Object is the user has authenticated properly
		if (Context.isAuthenticated()) {
			ConceptService cs = Context.getConceptService();
			cdList = cs.getAllConceptDatatypes();
		}
		
		return cdList;
	}
    
    @GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
