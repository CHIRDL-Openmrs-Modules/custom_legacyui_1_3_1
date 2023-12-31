/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.encounter;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationTag;
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.LocationTagEditor;
import org.openmrs.util.MetadataComparator;
import org.openmrs.validator.LocationValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.openmrs.web.attribute.WebAttributeUtil;
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
@RequestMapping(value = "admin/locations/location.form")
public class LocationFormController {
	
	private static final String FORM_VIEW = "/module/legacyui/admin/locations/locationForm";
	private static final String SUBMIT_VIEW = "location.list";
	
	/** Logger for this class and subclasses */
    private static final Logger log = LoggerFactory.getLogger(LocationFormController.class);
	
	/**
	 * Allows for Integers to be used as values in input tags. Normally, only strings and lists are
	 * expected
	 *
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
		binder.registerCustomEditor(Location.class, new LocationEditor());
		binder.registerCustomEditor(LocationTag.class, new LocationTagEditor());
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
    @GetMapping
    public String initForm(ModelMap ret) throws Exception {
		List<LocationTag> tags = Context.getLocationService().getAllLocationTags();
		Collections.sort(tags, new MetadataComparator(Context.getLocale()));
		ret.put("locationTags", tags);
		ret.put("attributeTypes", Context.getLocationService().getAllLocationAttributeTypes());
		return FORM_VIEW;
	}
	
	/**
	 * The onSubmit function receives the form/command object that was modified by the input form
	 * and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 * @should retire location
	 * @should not retire location if reason is empty
	 */
    @PostMapping
    protected ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("location") Location location,
            BindingResult errors) throws Exception {
		
		HttpSession httpSession = request.getSession();
		
		String view = FORM_VIEW;
		if (Context.isAuthenticated()) {
			try {
				WebAttributeUtil.handleSubmittedAttributesForType(location, errors, LocationAttribute.class, request,
				    Context.getLocationService().getAllLocationAttributeTypes());
				
				new LocationValidator().validate(location, errors);
				
				if (errors.hasErrors()) {
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}
				
				LocationService locationService = Context.getLocationService();
				
				//if the user was editing the location
				if (request.getParameter("saveLocation") != null) {
					locationService.saveLocation(location);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Location.saved");
				}
				//the 'retire this location' button was clicked
				else if (request.getParameter("retireLocation") != null) {
					locationService.retireLocation(location, location.getRetireReason());
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Location.retired");
				}
				//the 'unretire this location' button was clicked
				else if (request.getParameter("unretireLocation") != null) {
					locationService.unretireLocation(location);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Location.unretired");
				} 
				else if (request.getParameter("purgeLocation") != null) {
					try {
						locationService.purgeLocation(location);
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "legacyui.Location.purgedSuccessfully");
					}
					catch (DataIntegrityViolationException e) {
						log.error("Failed to delete location", e);
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.object.inuse.cannot.purge");
					}
				}
			}
			catch (APIException e) {
				log.error("Error while saving location: {}", location, e);
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, e.getMessage());
				return ShowFormUtil.showForm(errors, FORM_VIEW);
			}
			
			view = SUBMIT_VIEW;
		}
		
		return new ModelAndView(new RedirectView(view));
	}
   
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 * @should return valid location given valid locationId
	 */
    @ModelAttribute("location")
    protected Object formBackingObject(HttpServletRequest request) {		
		Location location = null;
		
		if (Context.isAuthenticated()) {
			LocationService ls = Context.getLocationService();
			String locationId = request.getParameter("locationId");
			if (locationId != null) {
				location = ls.getLocation(Integer.valueOf(locationId));
			}
		}
		
		if (location == null) {
			location = new Location();
		}
		
		return location;
	}
	
}
