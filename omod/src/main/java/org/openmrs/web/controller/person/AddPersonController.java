/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.person;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Person;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.dwr.PersonListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "admin/person/addPerson.htm")
public class AddPersonController {

	private static final String FORM_VIEW = "/module/legacyui/admin/person/addPerson";
	private static final String SUBMIT_VIEW = "admin/person/newPerson.form";

	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(AddPersonController.class);

	private static final String PATIENT_SHORT_EDIT_URL = "/admin/patients/shortPatientForm.form";

	private static final String PATIENT_EDIT_URL = "/admin/patients/patient.form";

	private static final String PATIENT_VIEW_URL = "/patientDashboard.form";

	private static final String USER_EDIT_URL = "/admin/users/user.form";

	private static final String PERSON_EDIT_URL = "/admin/person/person.form";

	private static final String FORM_ENTRY_ERROR_URL = "/module/legacyui/admin/person/entryError";

	/** Keys for this class */
	private static final String NAME = "name";

	private static final String BIRTH_DATE = "birthdate";

	private static final String AGE = "age";

	private static final String GENDER = "gender";

	private static final String PERSON_TYPE = "personType";

	private static final String PERSON_ID = "personId";

	private static final String VIEW_TYPE = "viewType";

	private static boolean invalidAgeFormat = false;

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request) throws Exception {

		Map<String, String> person = getParametersFromRequest(request);

		String personId = person.get(PERSON_ID);
		String viewType = person.get(VIEW_TYPE);
		String personType = person.get(PERSON_TYPE);

		if (StringUtils.isEmpty(personId)) {
			// if they didn't pick a person, continue on to the edit screen no matter what
			// type of view was requested)
			if ("view".equals(viewType) || "shortEdit".equals(viewType)) {
				viewType = "shortEdit";
			} else {
				viewType = "edit";
			}
			return new ModelAndView(new RedirectView(getPersonURL("", personType, viewType, request)));
		} else {
			// if they picked a person, go to the type of view that was requested

			// if they selected view, do a double check to make sure that type of person
			// already exists
			if ("view".equals(viewType) && "patient".equals(personType)) {
				// TODO Do we even want to ever redirect to a 'view'. I'm torn between jumping
				// the DAs right to the
				// dashboard or jumping them to the short edit screen to make (potential)
				// adjustments
				try {
					if (Context.getPatientService().getPatient(Integer.valueOf(personId)) == null) {
						viewType = "shortEdit";
					}
				} catch (Exception noPatientEx) {
					System.out.println("Inside Catch block: AddPersonController");
					// if there is no patient yet, they must go through those motions
					viewType = "shortEdit";
				}
			}
			// redirect to the appropriate url
			return new ModelAndView(new RedirectView(getPersonURL(personId, personType, viewType, request)));
		}
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 * @should catch an invalid birthdate
	 * @should catch pass for a valid birthdate
	 */
	@ModelAttribute("people")
	protected Object formBackingObject(HttpServletRequest request) {

		log.debug("Entering formBackingObject()");

		List<PersonListItem> personList = new Vector<>();

		if (Context.isAuthenticated()) {
			
			PersonService ps = Context.getPersonService();

			Integer userId = Context.getAuthenticatedUser().getUserId();

			invalidAgeFormat = false;
			Map<String, String> person = getParametersFromRequest(request);

			String gender = person.get(GENDER);
			String name = person.get(NAME);
			String birthdate = person.get(BIRTH_DATE);
			String age = person.get(AGE);

			log.debug("name: {} birthdate: {} age: {} gender: {}", name, birthdate, age, gender);

			if (StringUtils.isNotEmpty(name) || StringUtils.isNotEmpty(birthdate) || StringUtils.isNotEmpty(age)
					|| StringUtils.isNotEmpty(gender)) {

				log.info("{}|{}|{}|{}|{}", userId, name, birthdate, age, gender);

				Integer d = null;
				birthdate = birthdate.trim();
				age = age.trim();
				int birthyear = -1;

				try {
					// Do these if there's a value in the birthdate string
					if (birthdate.length() > 0) {
						Date birthdateFormatted = Context.getDateFormat().parse(birthdate);
						Calendar calender = Calendar.getInstance();
						calender.setTime(birthdateFormatted);
						birthyear = calender.get(Calendar.YEAR);
					}
				} catch (ParseException e) {
					// In theory, this should never happen -- the date selector should never allowed
					// the
					// user set an invalid date, but never know the scripts could be broken
					if (log.isDebugEnabled()) {
						log.debug("Parse exception occurred : ", e);
					}
					invalidAgeFormat = true;
				}

				// -1 means the birth-year has not defined.
				if (birthyear != -1) {
					d = birthyear;
				} else if (age.length() > 0) {
					Calendar c = Calendar.getInstance();
					c.setTime(new Date());
					d = c.get(Calendar.YEAR);
					try {
						d = d - Integer.parseInt(age);
					} catch (NumberFormatException e) {
						// In theory, this should never happen -- Javascript in the UI should prevent
						// this...
						invalidAgeFormat = true;
					}
				}

				if (gender.length() < 1) {
					person.put("gender", null);
				}

				personList = new Vector<PersonListItem>();
				for (Person p : ps.getSimilarPeople(name, d, gender)) {
					personList.add(PersonListItem.createBestMatch(p));
				}
			}

		}

		log.debug("Returning personList of size: {} from formBackingObject", personList.size());

		return personList;
	}

	/**
	 * Prepares the form view
	 */
	public ModelAndView showForm(HttpServletRequest request, BindingResult errors) throws Exception {

		log.debug("In showForm method");

		ModelAndView mav = ShowFormUtil.showForm(errors, FORM_VIEW);
		
		// If a invalid age is submitted, give the user a useful error message.
		if (invalidAgeFormat) {
			mav = new ModelAndView(FORM_ENTRY_ERROR_URL);
			mav.addObject("errorTitle", "Person.age.error");
			mav.addObject("errorMessage", "Person.birthdate.required");
			return mav;
		}

		Object o = mav.getModel().get("command");

		List personList = (List) o;

		log.debug("Found list of size: {}", personList.size());

		if (personList.size() < 1 && Context.isAuthenticated()) {
			Map<String, String> person = getParametersFromRequest(request);

			String name = person.get(NAME);
			String birthdate = person.get(BIRTH_DATE);
			String age = person.get(AGE);
			String gender = person.get(GENDER);
			String viewType = person.get(VIEW_TYPE);
			String personType = person.get(PERSON_TYPE);

			if (viewType == null) {
				viewType = "edit";
			}

			log.debug("name: {} birthdate: {} age: {} gender: {}", name, birthdate, age, gender);

			if (StringUtils.isNotEmpty(name) || StringUtils.isNotEmpty(birthdate) || StringUtils.isNotEmpty(age)
					|| StringUtils.isNotEmpty(gender)) {
				mav.clear();
				mav.setView(new RedirectView(getPersonURL("", personType, viewType, request)));
			}
		}

		return mav;
	}
	
	/**
	 * Returns the url string for the given personType and viewType
	 *
	 * @param personId
	 * @param personType
	 * @param viewType
	 * @param request
	 * @return url string
	 * @throws ServletException
	 * @throws UnsupportedEncodingException
	 */
	private String getPersonURL(String personId, String personType, String viewType, HttpServletRequest request)
			throws ServletException, UnsupportedEncodingException {

		Map<String, String> person = getParametersFromRequest(request);

		if ("patient".equals(personType)) {
			if ("edit".equals(viewType)) {
				return request.getContextPath() + PATIENT_EDIT_URL + getParametersForURL(person);
			}
			if ("shortEdit".equals(viewType)) {
				return request.getContextPath() + PATIENT_SHORT_EDIT_URL + getParametersForURL(person);
			} else if ("view".equals(viewType)) {
				return request.getContextPath() + PATIENT_VIEW_URL + getParametersForURL(person);
			}
		} else if ("user".equals(personType)) {
			return request.getContextPath() + USER_EDIT_URL + getParametersForURL(person);
		} else {
			if ("edit".equals(viewType)) {
				return request.getContextPath() + PERSON_EDIT_URL + getParametersForURL(person);
			}
		}
		throw new ServletException("You entered viewType = \"" + viewType + "\" and personType = \"" + personType
				+ "\" which is an invalid viewType/personType combination.\n"
				+ "Valid viewType/personType combinations are edit/patient, edit/user, shortEdit/patient, view/patient. The viewType edit is valid without any personType. Also, the personType user is valid without any viewType. \n");
	}

	/**
	 * Returns the appropriate ?patientId/?userId/?name&age&birthyear etc
	 *
	 *
	 * @param person@return
	 * @throws UnsupportedEncodingException
	 */
	private String getParametersForURL(Map<String, String> person) throws UnsupportedEncodingException {

		if (StringUtils.isEmpty(person.get(PERSON_ID))) {
			return "?addName=" + URLEncoder.encode(person.get(NAME), "UTF-8") + "&addBirthdate="
					+ person.get(BIRTH_DATE) + "&addAge=" + person.get(AGE) + "&addGender=" + person.get(GENDER);
		} else {
			if ("patient".equals(person.get(PERSON_TYPE))) {
				return "?patientId=" + person.get(PERSON_ID);
			} else if ("user".equals(person.get(PERSON_TYPE))) {
				return "?userId=" + person.get(PERSON_ID);
			} else {
				return "?personId=" + person.get(PERSON_ID);
			}
		}
	}

	/**
	 * @param request
	 */
	private Map<String, String> getParametersFromRequest(HttpServletRequest request) {
		Map<String, String> person = new HashMap<String, String>();
		person.put(NAME, ServletRequestUtils.getStringParameter(request, "addName", ""));
		person.put(BIRTH_DATE, ServletRequestUtils.getStringParameter(request, "addBirthdate", ""));
		person.put(AGE, ServletRequestUtils.getStringParameter(request, "addAge", ""));
		person.put(GENDER, ServletRequestUtils.getStringParameter(request, "addGender", ""));
		person.put(PERSON_TYPE, ServletRequestUtils.getStringParameter(request, "personType", "patient"));
		person.put(PERSON_ID, ServletRequestUtils.getStringParameter(request, "personId", ""));
		person.put(VIEW_TYPE, ServletRequestUtils.getStringParameter(request, "viewType", ""));

		return person;
	}

	@GetMapping
	public String initForm() throws Exception {
		return FORM_VIEW;
	}
}
