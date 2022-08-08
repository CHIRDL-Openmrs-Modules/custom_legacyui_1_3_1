/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.remotecommunication;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7InQueue;
import org.openmrs.hl7.HL7Service;
import org.openmrs.hl7.HL7Source;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class PostHl7Controller implements Controller {
	
    private static final Logger log = LoggerFactory.getLogger(PostHl7Controller.class);
	
	private String formView;
	
	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		Boolean success = false;
		if (!Context.isAuthenticated()) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				Context.authenticate(username, password, request.getRemoteAddr(), request.getHeader("User-Agent"));
			} else {
				model.put("error", "PostHl7.missingAuthentication");
			}
		}
		if (Context.isAuthenticated()) {
			String message = request.getParameter("hl7Message");
			String hl7Source = request.getParameter("source");
			if (StringUtils.hasText(message) && StringUtils.hasText(hl7Source)) {
				HL7Service service = Context.getHL7Service();
				HL7Source source = service.getHL7SourceByName(hl7Source);
				
				HL7InQueue hl7InQueue = new HL7InQueue();
				hl7InQueue.setHL7Data(message);
				hl7InQueue.setHL7Source(source);
				log.debug("source: {} , message: {}", hl7Source, message);
				Context.getHL7Service().saveHL7InQueue(hl7InQueue);
				success = true;
			} else {
				model.put("error", "PostHl7.sourceAndhl7MessageParametersRequired");
			}
		}
		model.put("success", success);
		return new ModelAndView(formView, "model", model);
	}
	
	public String getFormView() {
		return formView;
	}
	
	public void setFormView(String formView) {
		this.formView = formView;
	}
	
}
