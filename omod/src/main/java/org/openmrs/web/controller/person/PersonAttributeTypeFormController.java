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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.PersonAttributeType;
import org.openmrs.Privilege;
import org.openmrs.api.APIException;
import org.openmrs.api.PersonAttributeTypeLockedException;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.PrivilegeEditor;
import org.openmrs.validator.PersonAttributeTypeValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.openmrs.web.taglib.fieldgen.FieldGenHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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

/**
 * Controller for adding/editing a single PersonAttributeType
 */

@Controller
@RequestMapping(value = "admin/person/personAttributeType.form")
public class PersonAttributeTypeFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/person/personAttributeTypeForm";
	private static final String SUBMIT_VIEW = "personAttributeType.list";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(PersonAttributeTypeFormController.class);

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
		binder.registerCustomEditor(Privilege.class, new PrivilegeEditor());
	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	protected ModelAndView processSubmit(HttpServletRequest request,
			@ModelAttribute("personAttributeType") PersonAttributeType attrType, BindingResult errors, ModelMap map)
			throws Exception {

		new PersonAttributeTypeValidator().validate(attrType, errors);
		
		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			
			getModelMap(map);
			return new ModelAndView(FORM_VIEW, map);
		}
		
		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			PersonService ps = Context.getPersonService();
			try {
				if (request.getParameter("save") != null) {
					ps.savePersonAttributeType(attrType);
					view = SUBMIT_VIEW;
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "PersonAttributeType.saved");
				}
				// if the user is retiring out the personAttributeType
				else if (request.getParameter("retire") != null) {
					String retireReason = request.getParameter("retireReason");
					if (attrType.getPersonAttributeTypeId() != null && !(StringUtils.hasText(retireReason))) {
						errors.reject("retireReason", "general.retiredReason.empty");
						return ShowFormUtil.showForm(errors, FORM_VIEW);
					}
					ps.retirePersonAttributeType(attrType, retireReason);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "PersonAttributeType.retiredSuccessfully");
					view = SUBMIT_VIEW;
				}
				// if the user is purging the personAttributeType
				else if (request.getParameter("purge") != null) {
					try {
						ps.purgePersonAttributeType(attrType);
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR,
								"PersonAttributeType.purgedSuccessfully");
						view = SUBMIT_VIEW;
					} catch (DataIntegrityViolationException e) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
						view = "personAttributeType.form?personAttributeTypeId=" + attrType.getPersonAttributeTypeId();
					}
				} else if (request.getParameter("unretire") != null) {
					ps.unretirePersonAttributeType(attrType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR,
							"PersonAttributeType.unretiredSuccessfully");
					view = SUBMIT_VIEW;
				}
			} catch (PersonAttributeTypeLockedException e) {
				log.error("PersonAttributeType.locked", e);
				errors.reject(e.getMessage());
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "PersonAttributeType.locked");
				return ShowFormUtil.showForm(errors, FORM_VIEW);
			} catch (APIException e) {
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.general: " + e.getLocalizedMessage());
				view = "personAttributeType.form?personAttributeTypeId=" + attrType.getPersonAttributeTypeId();
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
	@ModelAttribute("personAttributeType")
	protected Object formBackingObject(HttpServletRequest request) {

		PersonAttributeType attrType = null;

		if (Context.isAuthenticated()) {
			PersonService ps = Context.getPersonService();
			String attrTypeId = request.getParameter("personAttributeTypeId");
			if (attrTypeId != null) {
				attrType = ps.getPersonAttributeType(Integer.valueOf(attrTypeId));
			}
		}

		if (attrType == null) {
			attrType = new PersonAttributeType();
		}

		return attrType;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.Errors)
	 */
	@GetMapping
	public String initForm(ModelMap map) throws Exception {

        getModelMap(map);

		return FORM_VIEW;
	}
	
	public void getModelMap(ModelMap map) {
		List<Privilege> privileges = new ArrayList<>();

		if (Context.isAuthenticated()) {
			privileges = Context.getUserService().getAllPrivileges();
		}

		Set<String> formats = new TreeSet<>(FieldGenHandlerFactory.getSingletonInstance().getHandlers().keySet());

		// these formats are handled directly by the FieldGenTag.java class and so
		// aren't in the
		// "handlers" list in openmrs-servlet.xml
		formats.add("java.lang.Character");
		formats.add("java.lang.Integer");
		formats.add("java.lang.Float");
		formats.add("java.lang.Boolean");

		// java.util.Date doesn't work as a PersonAttributeType since it gets saved in a
		// user-date-format-specific way
		formats.remove("java.util.Date");

		// Removing these two as per ticket: TRUNK-2460
		formats.remove("org.openmrs.Patient.exitReason");
		formats.remove("org.openmrs.DrugOrder.discontinuedReason");

		map.put("privileges", privileges);
		map.put("formats", formats);
	}
}
