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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.PrivilegeEditor;
import org.openmrs.propertyeditor.RoleEditor;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.RoleConstants;
import org.openmrs.validator.RoleValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping(value = "admin/users/role.form")
public class RoleFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/users/roleForm";
	private static final String SUBMIT_VIEW = "role.list";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(RoleFormController.class);

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
		binder.registerCustomEditor(Privilege.class, new PrivilegeEditor());
		binder.registerCustomEditor(Role.class, new RoleEditor());
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("role") Role role, BindingResult errors, ModelMap map)
			throws Exception {

		new RoleValidator().validate(role, errors);
		String[] inheritiedRoles = request.getParameterValues("inheritedRoles");
		if (inheritiedRoles == null) {
			role.setInheritedRoles(Collections.EMPTY_SET);
		}

		String[] privileges = request.getParameterValues("privileges");
		if (privileges == null) {
			role.setPrivileges((Set) (Collections.emptySet()));
		}

		return processSubmission(request, role, errors, map);
	}

	protected ModelAndView processSubmission(HttpServletRequest request, Role role, BindingResult errors, ModelMap map)
			throws Exception {

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
		
			getModelMap(map, role);
			return new ModelAndView(FORM_VIEW);
		}
		log.debug("No errors -> processing submit");
		return processFormSubmission(request, role, errors);

	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	
	protected ModelAndView processFormSubmission(HttpServletRequest request, @ModelAttribute("role") Role role,
			BindingResult errors) throws Exception {

		HttpSession httpSession = request.getSession();

		String view = FORM_VIEW;

		if (Context.isAuthenticated()) {
			try {
				Context.getUserService().saveRole(role);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Role.saved");
			} catch (APIException e) {
				errors.reject(e.getMessage());
				return ShowFormUtil.showForm(errors, FORM_VIEW);
			}
		}

		return new ModelAndView(new RedirectView(view));
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.Errors)
	 */
	@GetMapping
	public String initForm(ModelMap map, Role role) throws Exception {

		getModelMap(map, role);

		return FORM_VIEW;
	}

	public void getModelMap(ModelMap map, Role role) {
		if (Context.isAuthenticated()) {
			List<Role> allRoles = Context.getUserService().getAllRoles();
			Set<Role> inheritingRoles = new HashSet<>();
			Set<Privilege> inheritedPrivileges = new HashSet<>();
			allRoles.remove(role);
			for (Role r : allRoles) {
				if (r.getInheritedRoles().contains(role)) {
					inheritingRoles.add(r);
				}
			}
			addInheritedPrivileges(role, inheritedPrivileges);

			for (String s : OpenmrsConstants.AUTO_ROLES()) {
				Role r = Context.getUserService().getRole(s);
				allRoles.remove(r);
			}

			map.put("allRoles", allRoles);
			map.put("inheritingRoles", inheritingRoles);
			map.put("inheritedPrivileges", inheritedPrivileges);
			map.put("privileges", Context.getUserService().getAllPrivileges());
			map.put("superuser", RoleConstants.SUPERUSER);
		}
	}
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("role")
	protected Object formBackingObject(HttpServletRequest request) {

		Role role = null;

		if (Context.isAuthenticated()) {
			UserService us = Context.getUserService();
			String r = request.getParameter("roleName");
			if (r != null) {
				role = us.getRole(r);
			}
		}

		if (role == null) {
			role = new Role();
		}

		return role;
	}

	/**
	 * Fills the inherited privilege set recursively from the entire hierarchy of
	 * inheriting roles.
	 * 
	 * @param role                The root role
	 * @param inheritedPrivileges The set to fill
	 */
	private void addInheritedPrivileges(Role role, Set<Privilege> inheritedPrivileges) {
		if (role.getInheritedRoles() != null) {
			for (Role r : role.getInheritedRoles()) {
				if (!r.getAllParentRoles().contains(role) && r.getPrivileges() != null) {
					for (Privilege p : r.getPrivileges()) {
						if (!inheritedPrivileges.contains(p)) {
							inheritedPrivileges.add(p);
						}
					}
					addInheritedPrivileges(r, inheritedPrivileges);
				}
			}
		}
	}
}
