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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.EncounterType;
import org.openmrs.Field;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.validator.FieldValidator;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequestMapping(value = "admin/forms/field.form")
public class FieldFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/forms/fieldForm";
	private static final String SUBMIT_VIEW = "field.list";

	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(FieldFormController.class);

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
	}

	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("field") Field field,
			BindingResult errors, ModelMap map) throws Exception {

		new FieldValidator().validate(field, errors);

		return processSubmission(request, field, errors, map);
	}

	protected ModelAndView processSubmission(HttpServletRequest request, Field field, BindingResult errors,
			ModelMap map) throws Exception {

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}

			getModelMap(map, field);
			return new ModelAndView(FORM_VIEW, map);
		}
		log.debug("No errors -> processing submit");
		return processFormSubmission(request, field, errors);

	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */

	public ModelAndView processFormSubmission(HttpServletRequest request, @ModelAttribute("field") Field field,
			BindingResult errors) throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;
		String action = request.getParameter("action");

		if (Context.isAuthenticated()) {
			field = setObjects(field, request);

			if (action != null && action.equals(Context.getMessageSourceService().getMessage("general.delete"))) {
				try {
					Context.getFormService().purgeField(field);
				} catch (DataIntegrityViolationException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
					return new ModelAndView(new RedirectView("field.form?fieldId=" + field.getFieldId()));
				}
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Field.Deleted");
			} else {
				Context.getFormService().saveField(field);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Field.saved");
			}
		}

		view = SUBMIT_VIEW;
		view = view + "?phrase=" + request.getParameter("phrase");

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("field")
	protected Object formBackingObject(HttpServletRequest request) {

		Field field = null;

		if (Context.isAuthenticated()) {
			FormService fs = Context.getFormService();
			String fieldId = request.getParameter("fieldId");
			if (fieldId != null) {
				field = fs.getField(Integer.valueOf(fieldId));
			}
		}

		if (field == null) {
			field = new Field();
		}

		return field;
	}

	@GetMapping
	public String initForm(ModelMap map, @ModelAttribute("field") Field field) {

		getModelMap(map, field);
		return FORM_VIEW;
	}

	public void getModelMap(ModelMap map, Field field) {
		Locale locale = Context.getLocale();
		FormService fs = Context.getFormService();

		String defaultVerbose = "false";

		if (Context.isAuthenticated()) {
			map.put("fieldTypes", fs.getAllFieldTypes());
			if (field.getConcept() != null) {
				map.put("conceptName", field.getConcept().getName(locale));
			} else {
				map.put("conceptName", "");
			}
			defaultVerbose = Context.getAuthenticatedUser()
					.getUserProperty(OpenmrsConstants.USER_PROPERTY_SHOW_VERBOSE);
		}
		map.put("defaultVerbose", defaultVerbose.equals("true") ? true : false);

		Collection<EncounterType> encounterTypes = new ArrayList<>();
		Collection<FormField> containingAnyFormField = new ArrayList<>();
		Collection<FormField> containingAllFormFields = new ArrayList<>();
		Collection<Field> fields = new ArrayList<>();
		fields.add(field); // add the field to the fields collection
		List<Form> formsReturned = null;
		try {
			formsReturned = fs.getForms(null, null, encounterTypes, null, containingAnyFormField,
					containingAllFormFields, fields); // Retrieving forms which contain this particular field
		} catch (Exception e) {
			// When Object parameter doesn't contain a valid Form object, getFroms() throws
			// an Exception
		}

		map.put("formList", formsReturned); // add the returned forms to the map
	}

	private Field setObjects(Field field, HttpServletRequest request) {

		if (Context.isAuthenticated()) {
			String conceptId = request.getParameter("conceptId");
			if (conceptId != null && conceptId.length() > 0) {
				field.setConcept(Context.getConceptService().getConcept(Integer.valueOf(conceptId)));
			} else {
				field.setConcept(null);
			}

			field.setFieldType(
					Context.getFormService().getFieldType(Integer.valueOf(request.getParameter("fieldTypeId"))));
		}

		return field;

	}

}
