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

import org.openmrs.Role;
import org.openmrs.api.APIException;
import org.openmrs.api.CannotDeleteRoleWithChildrenException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.RoleConstants;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/users/role.list")
public class RoleListController {

	private static final String FORM_VIEW = "/module/legacyui/admin/users/roleList";
	private static final String SUBMIT_VIEW = "role.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(RoleListController.class);

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

		String view = FORM_VIEW;
		if (Context.isAuthenticated()) {
			StringBuilder success = new StringBuilder();
			StringBuilder error = new StringBuilder();

			MessageSourceService mss = Context.getMessageSourceService();

			String[] roleList = ServletRequestUtils.getStringParameters(request, "roleId");
			if (roleList.length > 0) {
				UserService us = Context.getUserService();

				String deleted = mss.getMessage("general.deleted");
				String notDeleted = mss.getMessage("Role.cannot.delete");
				String notDeletedWithChild = mss.getMessage("Role.cannot.delete.with.child");
				for (String p : roleList) {
					// TODO convenience method deleteRole(String) ??
					try {
						us.purgeRole(us.getRole(p));
						if (!success.toString().isEmpty()) {
							success.append("<br/>");
						}
						success.append(p).append(" ").append(deleted);
					} catch (DataIntegrityViolationException e) {
						handleRoleIntegrityException(e, error, notDeleted, p);
					} catch (CannotDeleteRoleWithChildrenException e) {
						handleRoleIntegrityException(e, error, notDeletedWithChild, p);
					} catch (APIException e) {
						handleRoleIntegrityException(e, error, notDeleted, p);
					}
				}
			} else {
				error.append(mss.getMessage("Role.select"));
			}

			view = SUBMIT_VIEW;
			if (!success.toString().isEmpty()) {
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success.toString());
			}
			if (!error.toString().isEmpty()) {
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error.toString());
			}
		}

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * Logs a role delete data integrity violation exception and returns a user
	 * friendly message of the problem that occurred.
	 *
	 * @param e          the exception.
	 * @param error      the error message.
	 * @param notDeleted the role not deleted error message.
	 * @return the formatted error message.
	 */
	private void handleRoleIntegrityException(Exception e, StringBuilder error, String notDeleted, String role) {
		log.warn("Error deleting role", e);
		if (!error.toString().isEmpty()) {
			error.append("<br/>");
		}
		error.append(role).append(": ").append(notDeleted);
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("roleList")
	protected Object formBackingObject() {

		// default empty Object
		// Object = the role
		// Boolean= whether or not the role is a core role (not able to be deleted)
		Map<Role, Boolean> roleList = new LinkedHashMap<>();

		// only fill the Object if the user has authenticated properly
		if (Context.isAuthenticated()) {
			UserService us = Context.getUserService();
			for (Role r : us.getAllRoles()) {
				if (OpenmrsUtil.getCoreRoles().keySet().contains(r.getRole())) {
					roleList.put(r, true);
				} else {
					roleList.put(r, false);
				}
			}
		}

		return roleList;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@GetMapping
	public String initForm(ModelMap map) throws Exception {

		if (Context.isAuthenticated()) {
			map.put("superuser", RoleConstants.SUPERUSER);
		}

		return FORM_VIEW;

	}
}
