/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.patient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Encounter;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/patients/mergePatients.form")
public class MergePatientsFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/patients/mergePatientsForm";
	private static final String SUBMIT_VIEW = "mergePatients.form";
	
	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(MergePatientsFormController.class);

	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("patient1") Patient p1,
			BindingResult errors) throws Exception {

		log.debug("Number of errors: {}", errors.getErrorCount());

		for (Object o : errors.getAllErrors()) {
			ObjectError e = (ObjectError) o;
			log.debug("Error name: {}", e.getObjectName());
			log.debug("Error code: {}", e.getCode());
			log.debug("Error message: {}", e.getDefaultMessage());
			log.debug("Error args: {}", Arrays.toString(e.getArguments()));
			log.debug("Error codes: {}", (Object[]) e.getCodes());
		}

		// call onSubmit manually so that we don't have to call
		// super.processFormSubmission()
		return processFormSubmission(request, p1, errors);
	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */

	public ModelAndView processFormSubmission(HttpServletRequest request, @ModelAttribute("patient1") Patient p1,
			BindingResult errors) throws Exception {

		HttpSession httpSession = request.getSession();

		if (Context.isAuthenticated()) {
			StringBuilder view = new StringBuilder(SUBMIT_VIEW);
			PatientService ps = Context.getPatientService();

			String pref = request.getParameter("preferred");
			String[] nonPreferred = request.getParameter("nonPreferred").split(",");
			String redirectURL = request.getParameter("redirectURL");
			String modalMode = request.getParameter("modalMode");

			Patient preferred = ps.getPatient(Integer.valueOf(pref));
			preferred.setPatientId(Integer.valueOf(pref)); 
			
			List<Patient> notPreferred = new ArrayList<Patient>();

			view.append("?patientId=").append(preferred.getPatientId());
			for (int i = 0; i < nonPreferred.length; i++) {
				notPreferred.add(ps.getPatient(Integer.valueOf(nonPreferred[i])));
				view.append("&patientId=").append(nonPreferred[i]);
			}

			try {
				ps.mergePatients(preferred, notPreferred);
			} catch (APIException e) {
				log.error("Unable to merge patients", e);
				String message = e.getMessage();
				if (message == null || "".equals(message)) {
					message = "Patient.merge.fail";
				}
				httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, message);
				return ShowFormUtil.showForm(errors, FORM_VIEW);
			}

			httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Patient.merged");

			if ("true".equals(modalMode)) {
				return ShowFormUtil.showForm(errors, FORM_VIEW);
			}

			int index = redirectURL.indexOf(request.getContextPath(), 2);
			if (index != -1) {
				redirectURL = redirectURL.substring(index);
				if (redirectURL.contains(SUBMIT_VIEW)) {
					redirectURL = "findDuplicatePatients.htm";
				}
			} else {
				redirectURL = view.toString();
			}

			return new ModelAndView(new RedirectView(redirectURL));
		}

		return new ModelAndView(new RedirectView(""));
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("patient1")
	protected Object formBackingObject(HttpServletRequest request) {

		Patient p1 = new Patient();

		if (Context.isAuthenticated()) {
			String[] patientIds = request.getParameterValues("patientId");
			if (patientIds != null && patientIds.length > 0) {
				String patientId = patientIds[0];
				Integer pId = Integer.valueOf(patientId);
				p1 = Context.getPatientService().getPatient(pId);
			}
		}

		return p1;
	}

	/**
	 * Called prior to form display. Allows for data to be put in the request to be
	 * used in the view
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@GetMapping
	public String initForm(ModelMap map, Patient p1, HttpServletRequest request) {

		Patient p2 = new Patient();
		Collection<Encounter> patient1Encounters = new Vector<>();
		Collection<Encounter> patient2Encounters = new Vector<>();
		List<Patient> patientList = new ArrayList<>();
		List<Collection<Encounter>> encounterList = new ArrayList<Collection<Encounter>>();
		if (Context.isAuthenticated()) {
			EncounterService es = Context.getEncounterService();
			patient1Encounters = es.getEncountersByPatient(p1);

			String[] patientIds = request.getParameterValues("patientId");
			if (patientIds != null) {
				for (String patient : patientIds) {
					patientList.add(Context.getPatientService().getPatient(Integer.valueOf(patient)));
					encounterList.add(es
							.getEncountersByPatient(Context.getPatientService().getPatient(Integer.valueOf(patient))));
				}
			}
			if (patientIds != null && patientIds.length > 1 && !patientIds[0].equals(patientIds[1])) {
				String patientId = patientIds[1];
				Integer pId = Integer.valueOf(patientId);
				p2 = Context.getPatientService().getPatient(pId);
				patient2Encounters = es.getEncountersByPatient(p2);
			}

		}

		map.put("patient1Encounters", patient1Encounters);
		map.put("patient2Encounters", patient2Encounters);
		map.put("patientEncounters", encounterList);
		map.put("patient2", p2);
		map.put("patientList", patientList);
		map.put("modalMode", request.getParameter("modalMode"));
		map.put("activeOrderErrorMessage", buildErrorMessage(getOrderTypePatientsMap(patientList)));
		return FORM_VIEW;
	}

	private Map<OrderType, Set<Patient>> getOrderTypePatientsMap(List<Patient> patientList) {
		Map<OrderType, Set<Patient>> activeOrderAndPatientsMap = new HashMap<>();
		OrderService os = Context.getOrderService();
		patientList.forEach(patient -> {
			os.getAllOrdersByPatient(patient).forEach(order -> {
				if (!order.isActive())
					return;
				Set<Patient> patients = activeOrderAndPatientsMap.getOrDefault(order.getOrderType(), new HashSet<>());
				patients.add(patient);
				activeOrderAndPatientsMap.putIfAbsent(order.getOrderType(), patients);
			});
		});
		return activeOrderAndPatientsMap;
	}

	private String buildErrorMessage(Map<OrderType, Set<Patient>> activeOrderAndPatientsMap) {
		String ACTIVE_DRUG_ORDER_ERR = "Active [ORDER_TYPE] orders exist for patientsPATIENT_IDS.<br />";
		String ACTIVE_DRUG_ORDER_WARN = "More than one patient having active order of same type is Not allowed";
		String[] errorMessages = new String[] { "" };
		activeOrderAndPatientsMap.forEach((OrderType orderType, Set<Patient> patients) -> {
			if (patients.size() < 2)
				return;
			String patientIds = patients.stream()
					.map((Patient patient) -> patient.getPatientIdentifier().getIdentifier())
					.reduce("", (id1, id2) -> id1 + ", " + id2);

			errorMessages[0] += ACTIVE_DRUG_ORDER_ERR.replace("ORDER_TYPE", orderType.toString()).replace("PATIENT_IDS",
					patientIds);
		});

		if (!"".equals(errorMessages[0])) {
			return errorMessages[0] + ACTIVE_DRUG_ORDER_WARN;
		}
		return "";
	}
}
