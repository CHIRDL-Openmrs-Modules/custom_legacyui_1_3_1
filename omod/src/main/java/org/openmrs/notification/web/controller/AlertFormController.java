/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.notification.web.controller;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.notification.Alert;
import org.openmrs.notification.AlertRecipient;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.validator.AlertValidator;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
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
@RequestMapping(value = "admin/users/alert.form")
public class AlertFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/users/alertForm";
	private static final String SUBMIT_VIEW = "alert.list";
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(AlertFormController.class);

	@InitBinder
	protected void initBinder(WebDataBinder binder) {

		Locale locale = Context.getLocale();
		NumberFormat nf = NumberFormat.getInstance(locale);

		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, nf, true));
		binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(Context.getDateFormat(), true, 10));

	}

	/**
	 * Handles the submission of the Alert List form.
	 *
	 * @param request The HTTP request information
	 * @param alert   Alert
	 * @param errors  org.springframework.validation.BindingResult
	 * @return The name of the next view
	 */
	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("alert") Alert alert,
			BindingResult errors) throws Exception {

		new AlertValidator().validate(alert, errors);
		try {
			// check that the user has the right privileges here because
			// we are giving them a proxy privilege in the line following this
			if (!Context.hasPrivilege(PrivilegeConstants.MANAGE_ALERTS)) {
				throw new APIAuthenticationException("Must be logged in as user with alerts privileges");
			}

			Context.addProxyPrivilege(PrivilegeConstants.GET_USERS);

			UserService us = Context.getUserService();

			if (Context.isAuthenticated()) {
				String[] userIdValues = request.getParameter("userIds").split(" ");
				List<Integer> userIds = new ArrayList<>();
				String[] roleValues = request.getParameter("newRoles").split(",");
				List<String> roles = new ArrayList<>();

				// create user list
				if (userIdValues != null) {
					for (String userId : userIdValues) {
						if (!"".equals(userId.trim())) {
							userIds.add(Integer.valueOf(userId.trim()));
						}
					}
				}

				// create role list
				if (roleValues != null) {
					for (String role : roleValues) {
						if (!"".equals(role.trim())) {
							roles.add(role.trim());
						}
					}
				}

				// remove all recipients not in the userIds list
				List<AlertRecipient> recipientsToRemove = new ArrayList<>();
				List<Integer> recipientIds = new ArrayList<>();
				if (alert.getRecipients() != null) {
					for (AlertRecipient recipient : alert.getRecipients()) {
						Integer userId = recipient.getRecipient().getUserId();
						if (!userIds.contains(userId)) {
							recipientsToRemove.add(recipient);
						} else {
							recipientIds.add(userId);
						}
					}
				}
				for (AlertRecipient ar : recipientsToRemove) {
					alert.removeRecipient(ar);
				}

				// add all new users

				for (Integer userId : userIds) {
					if (!recipientIds.contains(userId))
						alert.addRecipient(new User(userId));
				}

				// add all new users according to the role(s) selected
				for (String roleStr : roles) {
					List<User> users = us.getUsersByRole(new Role(roleStr));
					for (User user : users) {
						if (!userIds.contains(user.getId()))
							alert.addRecipient(user);
					}
				}

			}

			if (alert.getRecipients() == null || alert.getRecipients().isEmpty()) {
				errors.rejectValue("recipients", "Alert.recipientRequired");
			}

		} catch (Exception e) {
			log.error("Error while processing alert form", e);
			errors.reject(e.getMessage());
		} finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_USERS);
		}

		return processFormSubmission(request, alert, errors);
	}

	protected ModelAndView processFormSubmission(HttpServletRequest request, Alert alert, BindingResult errors)
			throws Exception {

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			return new ModelAndView(FORM_VIEW);
		}
		log.debug("No errors -> processing submit");
		return saveAlert(request, alert);

	}

	/**
	 * Saves alert to the db
	 *
	 * @param request The HTTP request information
	 * @param alert   Alert
	 */
	public ModelAndView saveAlert(HttpServletRequest request, Alert alert) throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			Context.getAlertService().saveAlert(alert);
			view = SUBMIT_VIEW;
			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Alert.saved");
		}

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 */
	@ModelAttribute("alert")
	protected Object formBackingObject(HttpServletRequest request) {

		Alert alert = null;

		if (Context.isAuthenticated()) {
			String a = request.getParameter("alertId");
			if (a != null) {
				alert = Context.getAlertService().getAlert(Integer.valueOf(a));
			}
		}

		if (alert == null) {
			alert = new Alert();
		}

		return alert;
	}

	@GetMapping
	public String initForm(ModelMap map) throws Exception {

		if (Context.isAuthenticated()) {
			map.put("allRoles", Context.getUserService().getAllRoles());
		}

		return FORM_VIEW;
	}

}