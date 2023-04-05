/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.user;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Privilege;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequestMapping(value = "admin/users/privilege.list")
public class PrivilegeListController {

	private static final String FORM_VIEW = "/module/legacyui/admin/users/privilegeList";
	private static final String SUBMIT_VIEW = "privilege.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(PrivilegeListController.class);

	/**
	 * Allows for Integers to be used as values in input tags. Normally, only
	 * strings and lists are expected
	 *
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
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
	protected ModelAndView processSubmit(HttpServletRequest request) throws Exception {

		HttpSession httpSession = request.getSession();

		// Locale locale = request.getLocale();
		String view = FORM_VIEW;
		if (Context.isAuthenticated()) {
			StringBuilder success = new StringBuilder();
			String error = "";

			MessageSourceService mss = Context.getMessageSourceService();

			String[] privilegeList = request.getParameterValues("privilegeId");
			if (privilegeList != null) {
				UserService us = Context.getUserService();
				String deleted = mss.getMessage("general.deleted");
				String notDeleted = mss.getMessage("Privilege.cannot.delete");
				for (String p : privilegeList) {
					// TODO convenience method deletePrivilege(String) ??
					try {
						us.purgePrivilege(us.getPrivilege(p));
						if (!"".equals(success.toString())) {
							success.append("<br/>");
						}
						success.append(p).append(" ").append(deleted);
					} catch (DataIntegrityViolationException e) {
						error = handlePrivilegeIntegrityException(e, error, notDeleted);
					} catch (APIException e) {
						error = handlePrivilegeIntegrityException(e, error, notDeleted);
					}
				}
			} else {
				error = mss.getMessage("Privilege.select");
			}

			view = SUBMIT_VIEW;
			if (!"".equals(success.toString())) {
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success.toString());
			}
			if (!"".equals(error)) {
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error);
			}
		}

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * Logs a privielge delete data integrity violation exception and returns a user
	 * friedly message of the problem that occured.
	 *
	 * @param e          the exception.
	 * @param error      the error message.
	 * @param notDeleted the not deleted error message.
	 * @return the formatted error message.
	 */
	private String handlePrivilegeIntegrityException(Exception e, String error, String notDeleted) {
		log.warn("Error deleting privilege", e);
		if (!"".equals(error)) {
			error += "<br/>";
		}
		error += notDeleted;
		return error;
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("privilegeList")
	protected Object formBackingObject() {

		// map containing the privilege and true/false whether the privilege is core or
		// not
		Map<Privilege, Boolean> privilegeList = new LinkedHashMap<>();

		// only fill the Object is the user has authenticated properly
		if (Context.isAuthenticated()) {
			UserService us = Context.getUserService();
			for (Privilege p : us.getAllPrivileges()) {
				if (OpenmrsUtil.getCorePrivileges().keySet().contains(p.getPrivilege())) {
					privilegeList.put(p, true);
				} else {
					privilegeList.put(p, false);
				}
			}
		}

		return privilegeList;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
