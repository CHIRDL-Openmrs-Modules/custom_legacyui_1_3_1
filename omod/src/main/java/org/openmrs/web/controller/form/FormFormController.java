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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.EncounterType;
import org.openmrs.FieldType;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.api.FormService;
import org.openmrs.api.FormsLockedException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.propertyeditor.EncounterTypeEditor;
import org.openmrs.util.FormUtil;
import org.openmrs.util.MetadataComparator;
import org.openmrs.validator.FormValidator;
import org.openmrs.web.ShowFormUtil;
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
@RequestMapping(value = {"admin/forms/formEdit.form","admin/forms/formSchemaDesign.form"})
public class FormFormController {
	
	private static final String FORM_VIEW = "/module/legacyui/admin/forms/formEditForm";
    private static final String SUBMIT_VIEW = "form.list";
    
	/** Logger for this class and subclasses */
    private static final Logger log = LoggerFactory.getLogger(FormFormController.class);
	
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
		// NumberFormat nf = NumberFormat.getInstance(new Locale("en_US"));
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
		binder.registerCustomEditor(EncounterType.class, new EncounterTypeEditor());
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
    protected ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("form") Form form,
            BindingResult errors, ModelMap map) throws Exception {
    	
    	new FormValidator().validate(form, errors);
    	
    	if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}

			getModelMap(map, request);
			return new ModelAndView(FORM_VIEW, map);
		}
		
		HttpSession httpSession = request.getSession();
		String view = FORM_VIEW;
		
		if (Context.isAuthenticated()) {
			
			MessageSourceService mss = Context.getMessageSourceService();
			String action = request.getParameter("action");
			try {
				if (action == null) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Form.not.saved");
				} else {
					if (action.equals(mss.getMessage("Form.save"))) {
						try {
							// save form
							form = Context.getFormService().saveForm(form);
							httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Form.saved");
						}
						catch (Exception e) {
							log.error("Error while saving form {}", form.getFormId(), e);
							errors.reject(e.getMessage());
							httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Form.not.saved");
							return ShowFormUtil.showForm(errors, FORM_VIEW);
						}
					} else if (action.equals(mss.getMessage("Form.delete"))) {
						try {
							Context.getFormService().purgeForm(form);
							httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Form.deleted");
						}
						catch (DataIntegrityViolationException e) {
							httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Form.cannot.delete");
							return new ModelAndView(new RedirectView("formEdit.form?formId=" + form.getFormId()));
						}
						catch (Exception e) {
							log.error("Error while deleting form {}", form.getFormId(), e);
							errors.reject(e.getMessage());
							httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Form.cannot.delete");
							return ShowFormUtil.showForm(errors, FORM_VIEW);
							//return new ModelAndView(new RedirectView(getSuccessView()));
						}
					} else if (action.equals(mss.getMessage("Form.updateSortOrder"))) {
						
						FormService fs = Context.getFormService();
						
						Map<Integer, TreeSet<FormField>> treeMap = FormUtil.getFormStructure(form);
						for (Map.Entry<Integer, TreeSet<FormField>> entry : treeMap.entrySet()) {
							float sortWeight = 0;
							for (FormField formField : entry.getValue()) {
								formField.setSortWeight(sortWeight);
								fs.saveFormField(formField);
								sortWeight += 50;
							}
						}
						
					} else {
						try {
							Context.getFormService().duplicateForm(form);
							httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Form.duplicated");
						}
						catch (Exception e) {
							log.error("Error while duplicating form {}", form.getFormId(), e);
							errors.reject(e.getMessage());
							httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Form.cannot.duplicate");
							return ShowFormUtil.showForm(errors, FORM_VIEW);
						}
					}
					
					view = SUBMIT_VIEW;
				}
			}
			catch (FormsLockedException e) {
				log.error("forms.locked", e);
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "forms.locked");
				if (form.getFormId() != null) {
					view = "formEdit.form?formId=" + form.getFormId();
				}
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
    @ModelAttribute("form")
    protected Object formBackingObject(HttpServletRequest request) {
		return getForm(request);
	}
	
    @GetMapping
    public String initForm(ModelMap map, HttpServletRequest request) throws Exception {
		
		getModelMap(map, request);
		
		return FORM_VIEW;
	}
    
    public void getModelMap(ModelMap map, HttpServletRequest request) {
    	List<FieldType> fieldTypes = new Vector<>();
		List<EncounterType> encTypes = new Vector<>();
		
		if (Context.isAuthenticated()) {
			fieldTypes = Context.getFormService().getAllFieldTypes();
			encTypes = Context.getEncounterService().getAllEncounterTypes();
			// Non-retired types first
			Collections.sort(encTypes, new MetadataComparator(Context.getLocale()));
		}
		
		map.put("fieldTypes", fieldTypes);
		map.put("encounterTypes", encTypes);
		map.put("isBasicForm", isBasicForm(getForm(request)));
    }
	
	/**
	 * Gets the form for a given http request.
	 *
	 * @param request the http request.
	 * @return the form.
	 */
	private Form getForm(HttpServletRequest request) {
		Form form = null;
		
		if (Context.isAuthenticated()) {
			FormService fs = Context.getFormService();
			String formId = request.getParameter("formId");
			if (formId != null) {
				try {
					form = fs.getForm(Integer.valueOf(formId));
				}
				catch (NumberFormatException e) {

				} //If formId has no readable value defaults to the case where form==null
			}
		}
		
		if (form == null) {
			form = new Form();
		}
		
		return form;
	}
	
	/**
	 * Checks if a form is a read only basic form installed with demo data.
	 *
	 * @param form the form.
	 * @return true if this is the demo data basic form, else false.
	 */
	private boolean isBasicForm(Form form) {
		if (form.getFormId() == null || form.getCreator() == null || form.getCreator().getUserId() == null
		        || form.getChangedBy() == null || form.getDateChanged() == null || form.getBuild() == null) {
			return false;
		}
		
		Calendar calender = Calendar.getInstance();
		calender.setTime(form.getDateCreated());
		
		return form.getFormId().intValue() == 1 && form.getCreator().getUserId().intValue() == 1
		        && calender.get(Calendar.YEAR) == 2006 && calender.get(Calendar.MONTH) == 6
		        && calender.get(Calendar.DAY_OF_MONTH) == 18 && form.getBuild().intValue() == 1;
	}
	
}
