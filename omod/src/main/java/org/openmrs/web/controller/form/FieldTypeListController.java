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

import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.FieldType;
import org.openmrs.api.APIException;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/forms/fieldType.list")
public class FieldTypeListController {

	private static final String FORM_VIEW = "/module/legacyui/admin/forms/fieldTypeList";
	private static final String SUBMIT_VIEW = "fieldType.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(FieldTypeListController.class);

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 *
	 * @should display a user friendly error message
	 */
	@PostMapping
	protected ModelAndView processSubmit(HttpServletRequest request) throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;
		if (Context.isAuthenticated()) {
			String[] fieldTypeList = request.getParameterValues("fieldTypeId");
			FormService fs = Context.getFormService();
			// FieldTypeService rs = new TestFieldTypeService();

			String success = "";
			String error = "";

			MessageSourceService mss = Context.getMessageSourceService();
			String deleted = mss.getMessage("general.deleted");
			String notDeleted = mss.getMessage("general.cannot.delete");
			String textFieldType = mss.getMessage("FieldType.fieldType");
			String noneDeleted = mss.getMessage("FieldType.nonedeleted");
			if (fieldTypeList != null) {
				for (String fieldTypeId : fieldTypeList) {
					// TODO convenience method deleteFieldType(Integer) ??
					try {
						fs.purgeFieldType(fs.getFieldType(Integer.valueOf(fieldTypeId)));
						if (!"".equals(success)) {
							success += "<br/>";
						}
						success += textFieldType + " " + fieldTypeId + " " + deleted;
					} catch (APIException e) {
						log.warn("Error deleting field type", e);
						if (!"".equals(error)) {
							error += "<br/>";
						}
						error += textFieldType + " " + fieldTypeId + " " + notDeleted;
					} catch (DataIntegrityViolationException e) {
						log.error("Unable to delete a field type because it is in use. fieldTypeId: {}", fieldTypeId,
								e);
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "FieldType.cannot.delete");
						return new ModelAndView(new RedirectView(SUBMIT_VIEW));
					}
				}
			} else {
				success += noneDeleted;
			}
			view = SUBMIT_VIEW;
			if (!"".equals(success)) {
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success);
			}
			if (!"".equals(error)) {
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error);
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
	@ModelAttribute("fieldTypeList")
	protected Object formBackingObject() {

		// default empty Object
		List<FieldType> fieldTypeList = new Vector<>();

		// only fill the Object is the user has authenticated properly
		if (Context.isAuthenticated()) {
			FormService fs = Context.getFormService();
			// FieldTypeService rs = new TestFieldTypeService();
			fieldTypeList = fs.getAllFieldTypes();
		}

		return fieldTypeList;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
