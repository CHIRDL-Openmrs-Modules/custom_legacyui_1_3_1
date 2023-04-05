/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.visit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.VisitAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.customdatatype.CustomDatatypeUtil;
import org.openmrs.validator.VisitAttributeTypeValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for editing visit attribute types.
 *
 * @since 1.9
 */
@Controller
@RequestMapping(value = "admin/visits/visitAttributeType.form")
public class VisitAttributeTypeFormController {

	private static final String FORM_VIEW = "/admin/visits/visitAttributeTypeForm";
	private static final String SUBMIT_VIEW = "visitAttributeType.list";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(VisitAttributeTypeFormController.class);
	
	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request,
			@ModelAttribute("visitAttributeType") VisitAttributeType visitAttributeType, BindingResult errors, ModelMap map)
			throws Exception {

		new VisitAttributeTypeValidator().validate(visitAttributeType, errors);
		
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
			VisitService visitService = Context.getVisitService();

			if (request.getParameter("save") != null) {
				visitService.saveVisitAttributeType(visitAttributeType);
				view = SUBMIT_VIEW;
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitAttributeType.saved");
			}

			// if the user is retiring out the VisitAttributeType
			else if (request.getParameter("retire") != null) {
				String retireReason = request.getParameter("retireReason");
				if (visitAttributeType.getVisitAttributeTypeId() != null && !(StringUtils.hasText(retireReason))) {
					errors.reject("retireReason", "general.retiredReason.empty");
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}

				visitService.retireVisitAttributeType(visitAttributeType, retireReason);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitAttributeType.retiredSuccessfully");

				view = SUBMIT_VIEW;
			}

			// if the user is purging the visitAttributeType
			else if (request.getParameter("purge") != null) {

				try {
					visitService.purgeVisitAttributeType(visitAttributeType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitAttributeType.purgedSuccessfully");
					view = SUBMIT_VIEW;
				} catch (DataIntegrityViolationException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
					view = "visitAttributeType.form?visitAttributeTypeId="
							+ visitAttributeType.getVisitAttributeTypeId();
				} catch (APIException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
							"error.general: " + e.getLocalizedMessage());
					view = "visitAttributeType.form?visitAttributeTypeId="
							+ visitAttributeType.getVisitAttributeTypeId();
				}
			} else if (request.getParameter("unretire") != null) {
				try {
					visitService.unretireVisitAttributeType(visitAttributeType);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "VisitAttributeType.unretiredSuccessfully");
					view = SUBMIT_VIEW;
				} catch (APIException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
							"error.general: " + e.getLocalizedMessage());
					view = "visitAttributeType.form?visitAttributeTypeId="
							+ visitAttributeType.getVisitAttributeTypeId();
				}
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
	@ModelAttribute("visitAttributeType")
	protected Object formBackingObject(HttpServletRequest request) {

		VisitAttributeType visitAttributeType = null;

		if (Context.isAuthenticated()) {
			VisitService os = Context.getVisitService();
			String visitAttributeTypeId = request.getParameter("visitAttributeTypeId");
			if (visitAttributeTypeId != null) {
				visitAttributeType = os.getVisitAttributeType(Integer.valueOf(visitAttributeTypeId));
			}
		}

		if (visitAttributeType == null) {
			visitAttributeType = new VisitAttributeType();
		}

		return visitAttributeType;
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
		map.put("datatypes", CustomDatatypeUtil.getDatatypeClassnames());
		map.put("handlers", CustomDatatypeUtil.getHandlerClassnames());
	}
}
