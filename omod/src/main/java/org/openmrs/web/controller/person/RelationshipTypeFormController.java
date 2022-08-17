/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.person;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.RelationshipType;
import org.openmrs.api.APIException;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.validator.RelationshipTypeValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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
@RequestMapping(value = "admin/person/relationshipType.form")
public class RelationshipTypeFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/person/relationshipTypeForm";
	private static final String SUBMIT_VIEW = "relationshipType.list";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(RelationshipTypeFormController.class);

	/**
	 * Allows for Integers to be used as values in input tags. Normally, only
	 * strings and lists are expected
	 *
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		// NumberFormat nf = NumberFormat.getInstance(new Locale("en_US"));
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("relationshipType") RelationshipType type,
			BindingResult errors) throws Exception {

		new RelationshipTypeValidator().validate(type, errors);

		return processSubmission(request, type, errors);
	}

	protected ModelAndView processSubmission(HttpServletRequest request, RelationshipType relationshipType,
			BindingResult errors) throws Exception {

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			return new ModelAndView(FORM_VIEW);
		}
		log.debug("No errors -> processing submit");
		return processFormSubmission(request, relationshipType, errors);

	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	protected ModelAndView processFormSubmission(HttpServletRequest request,
			@ModelAttribute("relationshipType") RelationshipType relationshipType, BindingResult errors)
			throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			PersonService ps = Context.getPersonService();

			// to save the relationship type
			if (request.getParameter("save") != null) {
				ps.saveRelationshipType(relationshipType);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "RelationshipType.saved");
			}

			// if the user is retiring out the relationshipType
			else if (request.getParameter("retire") != null) {
				String retireReason = request.getParameter("retireReason");
				if (relationshipType.getRelationshipTypeId() != null && !(StringUtils.hasText(retireReason))) {
					errors.reject("retireReason", "general.retiredReason.empty");
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}

				ps.retireRelationshipType(relationshipType, retireReason);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "RelationshipType.retiredSuccessfully");

				view = SUBMIT_VIEW;
			}

			// if the user is purging the relationshipType
			else if (request.getParameter("purge") != null) {
				try {
					ps.purgeRelationshipType(relationshipType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "RelationshipType.purgedSuccessfully");
					view = SUBMIT_VIEW;
				} catch (DataIntegrityViolationException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				} catch (APIException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
							"error.general: " + e.getLocalizedMessage());
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}
			}
			// if the user unretiring relationship type
			else if (request.getParameter("unretire") != null) {
				ps.unretireRelationshipType(relationshipType);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "RelationshipType.unretiredSuccessfully");
				view = SUBMIT_VIEW;
			}

		}

		return new ModelAndView(new RedirectView(view));
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("relationshipType")
	protected Object formBackingObject(HttpServletRequest request) {

		RelationshipType identifierType = null;

		if (Context.isAuthenticated()) {
			PersonService ps = Context.getPersonService();
			String relationshipTypeId = request.getParameter("relationshipTypeId");
			if (relationshipTypeId != null) {
				identifierType = ps.getRelationshipType(Integer.valueOf(relationshipTypeId));
			}
		}

		if (identifierType == null) {
			identifierType = new RelationshipType();
		}

		return identifierType;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}

}
