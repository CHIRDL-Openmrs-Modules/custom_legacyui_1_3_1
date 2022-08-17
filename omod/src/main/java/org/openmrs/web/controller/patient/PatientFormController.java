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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Attributable;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientIdentifierType.LocationBehavior;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.DuplicateIdentifierException;
import org.openmrs.api.IdentifierNotUniqueException;
import org.openmrs.api.InsufficientIdentifiersException;
import org.openmrs.api.InvalidIdentifierFormatException;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientIdentifierException;
import org.openmrs.api.PatientService;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.PatientIdentifierTypeEditor;
import org.openmrs.util.OpenmrsConstants.PERSON_TYPE;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.validator.PatientIdentifierValidator;
import org.openmrs.validator.PatientValidator;
import org.openmrs.validator.PersonAddressValidator;
import org.openmrs.web.ShowFormUtil;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Patient-specific form controller. Creates the model/view etc for editing
 * patients.
 *
 * @see org.openmrs.web.controller.person.PersonFormController
 */
@Controller
@RequestMapping(value = "admin/patients/patient.form")
public class PatientFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/patients/patientForm";
	private static final String SUBMIT_VIEW = "patient.form";

	/** Logger for this class and subclasses */
	private static final Logger log = LoggerFactory.getLogger(PatientFormController.class);

	@Autowired
	PatientValidator patientValidator;

	/**
	 * Allows for other Objects to be used as values in input tags. Normally, only
	 * strings and lists are expected
	 *
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */

	@InitBinder
	protected void initBinder(WebDataBinder binder) {

		NumberFormat nf = NumberFormat.getInstance(Context.getLocale());
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, nf, true));
		binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(Context.getDateFormat(), true, 10));
		binder.registerCustomEditor(PatientIdentifierType.class, new PatientIdentifierTypeEditor());
		binder.registerCustomEditor(Location.class, new LocationEditor());
		binder.registerCustomEditor(Concept.class, "civilStatus", new ConceptEditor());
		binder.registerCustomEditor(Concept.class, "causeOfDeath", new ConceptEditor());
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@PostMapping
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("patient") Patient patient,
			BindingResult errors, ModelMap map) throws Exception {

		if (Context.isAuthenticated()) {

			PatientService ps = Context.getPatientService();
			LocationService ls = Context.getLocationService();
			Object[] objs = null;

			MessageSourceService mss = Context.getMessageSourceService();
			String action = request.getParameter("action");

			if (action.equals(mss.getMessage("Patient.save"))) {

				patientValidator.validate(patient, errors);

				if (errors.hasErrors()) {
					getModelMap(map, patient);
					return ShowFormUtil.showForm(errors, FORM_VIEW);
				}

				updatePersonNames(request, patient);

				updatePersonAddresses(request, patient, errors);

				updatePersonAttributes(request, errors, patient);

				// Patient Identifiers
				objs = patient.getIdentifiers().toArray();
				for (int i = 0; i < objs.length; i++) {
					if (request.getParameter("identifiers[" + i + "].identifier") == null) {
						patient.removeIdentifier((PatientIdentifier) objs[i]);
					}
				}

				String[] ids = request.getParameterValues("identifier");
				String[] idTypes = request.getParameterValues("identifierType");
				String[] locs = request.getParameterValues("location");
				String[] idPrefStatus = ServletRequestUtils.getStringParameters(request, "preferred");

				if (ids != null) {
					for (int i = 0; i < ids.length; i++) {
						String id = ids[i].trim();
						if (!"".equals(id) && !"".equals(idTypes[i])) { // skips invalid and blank
																		// identifiers/identifierTypes
							PatientIdentifier pi = new PatientIdentifier();
							pi.setIdentifier(id);
							pi.setIdentifierType(ps.getPatientIdentifierType(Integer.valueOf(idTypes[i])));
							if (StringUtils.isNotEmpty(locs[i])) {
								pi.setLocation(ls.getLocation(Integer.valueOf(locs[i])));
							}
							if (idPrefStatus != null && idPrefStatus.length > i) {
								pi.setPreferred(new Boolean(idPrefStatus[i]));
							}
							new PatientIdentifierValidator().validate(pi, errors);
							if (errors.hasErrors()) {
								return ShowFormUtil.showForm(errors, FORM_VIEW);
							}
							patient.addIdentifier(pi);
						}
					}
				}

				Iterator<PatientIdentifier> identifiers = patient.getIdentifiers().iterator();
				PatientIdentifier currentId = null;
				PatientIdentifier preferredId = null;
				while (identifiers.hasNext()) {
					currentId = identifiers.next();
					if (currentId.isPreferred()) {
						if (preferredId != null) { // if there's a preferred identifier already exists, make it
													// preferred=false
							preferredId.setPreferred(false);
						}
						preferredId = currentId;
					}
				}
				if ((preferredId == null) && (currentId != null)) { // No preferred identifiers. Make the last
																	// identifier entry as preferred.
					currentId.setPreferred(true);
				}

				// check patient identifier formats
				for (PatientIdentifier pi : patient.getIdentifiers()) {
					// skip voided identifiers
					if (pi.isVoided()) {
						continue;
					}
					PatientIdentifierType pit = pi.getIdentifierType();
					String identifier = pi.getIdentifier();
					String format = pit.getFormat();
					String formatDescription = pit.getFormatDescription();
					String formatStr = format;
					if (format == null) {
						formatStr = "";
					}
					if (formatDescription != null && formatDescription.length() > 0) {
						formatStr = formatDescription;
					}
					String[] args = { identifier, formatStr };
					try {
						if (format != null && format.length() > 0 && !identifier.matches(format)) {
							log.error("Identifier format is not valid: ({}) {}", format, identifier);
							String msg = Context.getMessageSourceService().getMessage("error.identifier.formatInvalid",
									args, Context.getLocale());
							errors.rejectValue("identifiers", msg);
						}
					} catch (Exception e) {
						log.error("exception thrown with: {} {}", pit.getName(), identifier);
						log.error("Error while adding patient identifiers to savedIdentifier list", e);

						String msg = Context.getMessageSourceService().getMessage("error.identifier.formatInvalid",
								args, Context.getLocale());
						errors.rejectValue("identifiers", msg);
					}

					if (errors.hasErrors()) {
						return ShowFormUtil.showForm(errors, FORM_VIEW);
					}
				}

			} // end "if we're saving the patient"
		}

		return processSubmission(request, patient, errors);
	}

	protected ModelAndView processSubmission(HttpServletRequest request, @ModelAttribute("patient") Patient patient, BindingResult errors)
			throws Exception {

		if (errors.hasErrors()) {
			if (log.isDebugEnabled()) {
				log.debug("Data binding errors: {}", errors.getErrorCount());
			}
			return new ModelAndView(FORM_VIEW);
		}
		log.debug("No errors -> processing submit");
		return processFormSubmission(request, patient, errors);

	}

	/**
	 * The onSubmit function receives the form/command object that was modified by
	 * the input form and saves it to the db
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 * @should void patient when void reason is not empty
	 * @should not void patient when void reason is empty
	 */

	protected ModelAndView processFormSubmission(HttpServletRequest request, @ModelAttribute("patient") Patient patient, BindingResult errors)
			throws Exception {

		HttpSession httpSession = request.getSession();

		if (Context.isAuthenticated()) {

			MessageSourceService mss = Context.getMessageSourceService();
			String action = request.getParameter("action");
			PatientService ps = Context.getPatientService();

			if (action.equals(mss.getMessage("Patient.delete"))) {
				try {
					ps.purgePatient(patient);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Patient.deleted");
					return new ModelAndView(new RedirectView("index.htm"));
				} catch (DataIntegrityViolationException e) {
					log.error("Unable to delete patient because of database FK errors: {}", patient, e);
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Patient.cannot.delete");
					return new ModelAndView(
							new RedirectView(SUBMIT_VIEW + "?patientId=" + patient.getPatientId().toString()));
				}
			} else if (action.equals(mss.getMessage("Patient.void"))) {
				String voidReason = request.getParameter("voidReason");
				if (StringUtils.isBlank(voidReason)) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Patient.error.void.reasonEmpty");
				} else {
					ps.voidPatient(patient, voidReason);
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Patient.voided");
				}
				return new ModelAndView(new RedirectView(SUBMIT_VIEW + "?patientId=" + patient.getPatientId()));
			} else if (action.equals(mss.getMessage("Patient.unvoid"))) {
				ps.unvoidPatient(patient);
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Patient.unvoided");
				return new ModelAndView(new RedirectView(SUBMIT_VIEW + "?patientId=" + patient.getPatientId()));
			} else {
				// boolean isNew = (patient.getPatientId() == null);
				boolean isError = false;

				try {
					Context.getPatientService().savePatient(patient);
				} catch (ValidationException ve) {
					log.error("Error saving patient to PatientService: ", ve);
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, ve.getMessage());
					isError = true;
				} catch (InvalidIdentifierFormatException iife) {
					log.error("Error saving patient to PatientService: ", iife);
					patient.removeIdentifier(iife.getPatientIdentifier());
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "PatientIdentifier.error.formatInvalid");
					isError = true;
				} catch (IdentifierNotUniqueException inue) {
					log.error("Error saving patient to PatientService: ", inue);
					patient.removeIdentifier(inue.getPatientIdentifier());
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "PatientIdentifier.error.notUnique");
					isError = true;
				} catch (DuplicateIdentifierException die) {
					log.error("Error saving patient to PatientService: ", die);
					patient.removeIdentifier(die.getPatientIdentifier());
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "PatientIdentifier.error.duplicate");
					isError = true;
				} catch (InsufficientIdentifiersException iie) {
					log.error("Error saving patient to PatientService: ", iie);
					patient.removeIdentifier(iie.getPatientIdentifier());
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
							"PatientIdentifier.error.insufficientIdentifiers");
					isError = true;
				} catch (PatientIdentifierException pie) {
					log.error("Error saving patient to PatientService: ", pie);
					patient.removeIdentifier(pie.getPatientIdentifier());
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "PatientIdentifier.error.general");
					isError = true;
				}

				// If patient is dead
				if (patient.getDead() && !isError) {
					log.debug("Patient is dead, so let's make sure there's an Obs for it");
					// need to make sure there is an Obs that represents the patient's cause of
					// death, if applicable

					String causeOfDeathConceptId = Context.getAdministrationService()
							.getGlobalProperty("concept.causeOfDeath");
					Concept causeOfDeath = Context.getConceptService().getConcept(causeOfDeathConceptId);

					if (causeOfDeath != null) {
						List<Obs> obssDeath = Context.getObsService().getObservationsByPersonAndConcept(patient,
								causeOfDeath);
						if (obssDeath != null) {
							if (obssDeath.size() > 1) {
								log.error("Multiple causes of death ({})?  Shouldn't be...", obssDeath.size());
							} else {
								Obs obsDeath = null;
								if (obssDeath.size() == 1) {
									// already has a cause of death - let's edit it.
									log.debug("Already has a cause of death, so changing it");

									obsDeath = obssDeath.iterator().next();

								} else {
									// no cause of death obs yet, so let's make one
									log.debug("No cause of death yet, let's create one.");

									obsDeath = new Obs();
									obsDeath.setPerson(patient);
									obsDeath.setConcept(causeOfDeath);
									Location location = Context.getLocationService().getDefaultLocation();
									// TODO person healthcenter //if ( loc == null ) loc =
									// patient.getHealthCenter();
									if (location != null) {
										obsDeath.setLocation(location);
									} else {
										log.error(
												"Could not find a suitable location for which to create this new Obs");
									}
								}

								// put the right concept and (maybe) text in this obs
								Concept currCause = patient.getCauseOfDeath();
								if (currCause == null) {
									// set to NONE
									log.debug("Current cause is null, attempting to set to NONE");
									String noneConcept = Context.getAdministrationService()
											.getGlobalProperty("concept.none");
									currCause = Context.getConceptService().getConcept(noneConcept);
								}

								if (currCause != null) {
									log.debug("Current cause is not null, setting to value_coded");
									obsDeath.setValueCoded(currCause);
									obsDeath.setValueCodedName(currCause.getName()); // ABKTODO: presume current locale?

									Date dateDeath = patient.getDeathDate();
									if (dateDeath == null) {
										dateDeath = new Date();
									}
									obsDeath.setObsDatetime(dateDeath);

									// check if this is an "other" concept - if so, then we need to add value_text
									String otherConcept = Context.getAdministrationService()
											.getGlobalProperty("concept.otherNonCoded");
									Concept conceptOther = Context.getConceptService().getConcept(otherConcept);
									boolean deathReasonChanged = false;
									if (conceptOther != null) {
										String otherInfo = ServletRequestUtils.getStringParameter(request,
												"causeOfDeath_other", "");
										if (conceptOther.equals(currCause)) {
											// seems like this is an other concept - let's try to get the "other" field
											// info
											deathReasonChanged = !otherInfo.equals(obsDeath.getValueText());
											log.debug("Setting value_text as {}", otherInfo);
											obsDeath.setValueText(otherInfo);
										} else {
											// non empty text value implies concept changed from OTHER NON CODED to NONE
											deathReasonChanged = !"".equals(otherInfo);
											log.debug("New concept is NOT the OTHER concept, so setting to blank");
											obsDeath.setValueText("");
										}
									} else {
										log.debug("Don't seem to know about an OTHER concept, so deleting value_text");
										obsDeath.setValueText("");
									}
									boolean shouldSaveObs = (null == obsDeath.getId()) || deathReasonChanged;
									if (shouldSaveObs) {
										if (null == obsDeath.getVoidReason()) {
											obsDeath.setVoidReason("Changed in patient demographics editor");
										}
										Context.getObsService().saveObs(obsDeath, obsDeath.getVoidReason());
									}
								} else {
									log.debug("Current cause is still null - aborting mission");
								}
							}
						}
					} else {
						log.debug(
								"Cause of death is null - should not have gotten here without throwing an error on the form.");
					}

				}

				if (!isError) {
					String view = SUBMIT_VIEW;

					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Patient.saved");

					view = view + "?patientId=" + patient.getPatientId();
					return new ModelAndView(new RedirectView(view));
				} 
					return ShowFormUtil.showForm(errors, FORM_VIEW);
			}
		}
		return new ModelAndView(new RedirectView(FORM_VIEW));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */

	@ModelAttribute("patient")
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {

		Patient patient = null;

		if (Context.isAuthenticated()) {
			PatientService ps = Context.getPatientService();
			String patientId = request.getParameter("patientId");
			Integer id;
			if (patientId != null) {
				try {
					id = Integer.valueOf(patientId);
					patient = ps.getPatientOrPromotePerson(id);
					if (patient == null) {
						HttpSession session = request.getSession();
						session.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "patientDashboard.noPatientWithId");
						session.setAttribute(WebConstants.OPENMRS_ERROR_ARGS, patientId);
						return new Patient();
					}
				} catch (NumberFormatException numberError) {
					log.warn("Invalid patientId supplied: '{}'", patientId, numberError);
				}
			}
		}

		if (patient == null) {
			patient = new Patient();

			String name = request.getParameter("addName");
			if (name != null) {
				String gender = request.getParameter("addGender");
				String date = request.getParameter("addBirthdate");
				String age = request.getParameter("addAge");

				getMiniPerson(patient, name, gender, date, age);
			}
		}

		if (patient.getIdentifiers().size() < 1) {
			patient.addIdentifier(new PatientIdentifier());
		} else {
			// we need to check if current patient has preferred id
			// if no we look for suitable one to set it as preferred
			if (patient.getPatientIdentifier() != null && !patient.getPatientIdentifier().isPreferred()) {

				List<PatientIdentifier> pi = patient.getActiveIdentifiers();
				for (PatientIdentifier patientIdentifier : pi) {
					if (!patientIdentifier.isVoided() && !patientIdentifier.getIdentifierType().isRetired()) {
						patientIdentifier.setPreferred(true);
						break;
					}
				}
			}
		}

		setupFormBackingObject(patient);

		return patient;
	}

	/**
	 * Called prior to form display. Allows for data to be put in the request to be
	 * used in the view
	 *
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@GetMapping
	public String initForm(ModelMap map, @ModelAttribute("patient") Patient patient) throws Exception {

		getModelMap(map, patient);

		return FORM_VIEW;
	}

	private ModelMap getModelMap(ModelMap map, Patient patient) throws Exception {
	
		List<Form> forms = new Vector<Form>();
		List<Encounter> encounters = new Vector<Encounter>();

		if (Context.isAuthenticated() && patient.getPatientId() != null) {
			boolean onlyPublishedForms = true;
			if (Context.hasPrivilege(PrivilegeConstants.VIEW_UNPUBLISHED_FORMS)) {
				onlyPublishedForms = false;
			}
			forms.addAll(Context.getFormService().getForms(null, onlyPublishedForms, null, false, null, null, null));

			List<Encounter> encs = Context.getEncounterService().getEncountersByPatient(patient);
			if (encs != null && encs.size() > 0) {
				encounters.addAll(encs);
			}
		}

		String patientVariation = "";
		if (patient.isDead()) {
			patientVariation = "Dead";
		}

		Concept reasonForExitConcept = Context.getConceptService()
				.getConcept(Context.getAdministrationService().getGlobalProperty("concept.reasonExitedCare"));

		if (reasonForExitConcept != null && patient.getPatientId() != null) {
			List<Obs> patientExitObs = Context.getObsService().getObservationsByPersonAndConcept(patient,
					reasonForExitConcept);
			if (patientExitObs != null && patientExitObs.size() > 0) {
				log.debug("Exit obs is size {}", patientExitObs.size());
				if (patientExitObs.size() == 1) {
					Obs exitObs = patientExitObs.iterator().next();
					Concept exitReason = exitObs.getValueCoded();
					Date exitDate = exitObs.getObsDatetime();
					if (exitReason != null && exitDate != null) {
						patientVariation = "Exited";
					}
				} else {
					log.error("Too many reasons for exit - not putting data into model");
				}
			}
		}
		List<PatientIdentifierType> pits = Context.getPatientService().getAllPatientIdentifierTypes();
		boolean identifierLocationUsed = false;
		for (PatientIdentifierType pit : pits) {
			if (pit.getLocationBehavior() == null || pit.getLocationBehavior() == LocationBehavior.REQUIRED) {
				identifierLocationUsed = true;
			}
		}
		map.put("identifierTypes", pits);
		map.put("identifierLocationUsed", identifierLocationUsed);
		map.put("identifiers", patient.getIdentifiers());
		map.put("patientVariation", patientVariation);

		map.put("forms", forms);

		// empty objects used to create blank template in the view
		map.put("emptyIdentifier", new PatientIdentifier());
		map.put("emptyName", new PersonName());
		map.put("emptyAddress", new PersonAddress());
		map.put("encounters", encounters);

		ModelMap m = setupReferenceData(map, patient);
		return m;
	}
	/**
	 * Updates person attributes based on request parameters
	 * 
	 * @param request
	 * @param errors
	 * @param person
	 */
	protected void updatePersonAttributes(HttpServletRequest request, BindingResult errors, Person person) {
		// look for person attributes in the request and save to person
		for (PersonAttributeType type : Context.getPersonService().getPersonAttributeTypes(PERSON_TYPE.PERSON, null)) {
			String paramName = type.getPersonAttributeTypeId().toString();
			String value = request.getParameter(paramName);

			// if there is an error displaying the attribute, the value will be null
			if (value != null) {
				PersonAttribute attribute = new PersonAttribute(type, value);
				try {
					Object hydratedObject = attribute.getHydratedObject();
					if (hydratedObject == null || "".equals(hydratedObject.toString())) {
						// if null is returned, the value should be blanked out
						attribute.setValue("");
					} else if (hydratedObject instanceof Attributable) {
						attribute.setValue(((Attributable) hydratedObject).serialize());
					} else if (!hydratedObject.getClass().getName().equals(type.getFormat())) {
						// if the classes doesn't match the format, the hydration failed somehow
						// TODO change the PersonAttribute.getHydratedObject() to not swallow all
						// errors?
						throw new APIException();
					}
				} catch (APIException e) {
					errors.rejectValue("attributes", "Invalid value for " + type.getName() + ": '" + value + "'");
					log.warn("Got an invalid value: " + value + " while setting personAttributeType id #" + paramName,
							e);

					// setting the value to empty so that the user can reset the value to something
					// else
					attribute.setValue("");

				}
				person.addAttribute(attribute);
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("Person Attributes: \n {}", person.printAttributes());
		}
	}

	/**
	 * Updates person names based on request parameters
	 * 
	 * @param request
	 * @param person
	 */
	protected void updatePersonNames(HttpServletRequest request, Person person) {
		Object[] objs = null;
		objs = person.getNames().toArray();
		for (int i = 0; i < objs.length; i++) {
			if (request.getParameter("names[" + i + "].givenName") == null) {
				person.removeName((PersonName) objs[i]);
			}
		}

		// String[] prefs = request.getParameterValues("preferred"); (unreliable form
		// info)
		String[] gNames = ServletRequestUtils.getStringParameters(request, "givenName");
		String[] mNames = ServletRequestUtils.getStringParameters(request, "middleName");
		String[] fNamePrefixes = ServletRequestUtils.getStringParameters(request, "familyNamePrefix");
		String[] fNames = ServletRequestUtils.getStringParameters(request, "familyName");
		String[] fName2s = ServletRequestUtils.getStringParameters(request, "familyName2");
		String[] fNameSuffixes = ServletRequestUtils.getStringParameters(request, "familyNameSuffix");
		String[] degrees = ServletRequestUtils.getStringParameters(request, "degree");
		String[] namePrefStatus = ServletRequestUtils.getStringParameters(request, "preferred");

		if (gNames != null) {
			for (int i = 0; i < gNames.length; i++) {
				if (!"".equals(gNames[i])) { // skips invalid and blank address data box
					PersonName pn = new PersonName();
					if (namePrefStatus != null && namePrefStatus.length > i) {
						pn.setPreferred(new Boolean(namePrefStatus[i]));
					}
					if (gNames.length >= i + 1) {
						pn.setGivenName(gNames[i]);
					}
					if (mNames.length >= i + 1) {
						pn.setMiddleName(mNames[i]);
					}
					if (fNamePrefixes.length >= i + 1) {
						pn.setFamilyNamePrefix(fNamePrefixes[i]);
					}
					if (fNames.length >= i + 1) {
						pn.setFamilyName(fNames[i]);
					}
					if (fName2s.length >= i + 1) {
						pn.setFamilyName2(fName2s[i]);
					}
					if (fNameSuffixes.length >= i + 1) {
						pn.setFamilyNameSuffix(fNameSuffixes[i]);
					}
					if (degrees.length >= i + 1) {
						pn.setDegree(degrees[i]);
					}
					person.addName(pn);
				}
			}
			Iterator<PersonName> names = person.getNames().iterator();
			PersonName currentName = null;
			PersonName preferredName = null;
			while (names.hasNext()) {
				currentName = names.next();
				if (currentName.isPreferred()) {
					if (preferredName != null) { // if there's a preferred name already exists, make it preferred=false
						preferredName.setPreferred(false);
					}
					preferredName = currentName;
				}
			}
			if ((preferredName == null) && (currentName != null)) { // No preferred name. Make the last name entry as
																	// preferred.
				currentName.setPreferred(true);
			}
		}
	}

	/**
	 * Updates person addresses based on request parameters
	 * 
	 * @param request
	 * @param person
	 * @param errors
	 * @throws ParseException
	 */
	protected void updatePersonAddresses(HttpServletRequest request, Person person, BindingResult errors)
			throws ParseException {
		String[] add1s = ServletRequestUtils.getStringParameters(request, "address1");
		String[] add2s = ServletRequestUtils.getStringParameters(request, "address2");
		String[] cities = ServletRequestUtils.getStringParameters(request, "cityVillage");
		String[] states = ServletRequestUtils.getStringParameters(request, "stateProvince");
		String[] countries = ServletRequestUtils.getStringParameters(request, "country");
		String[] lats = ServletRequestUtils.getStringParameters(request, "latitude");
		String[] longs = ServletRequestUtils.getStringParameters(request, "longitude");
		String[] pCodes = ServletRequestUtils.getStringParameters(request, "postalCode");
		String[] counties = ServletRequestUtils.getStringParameters(request, "countyDistrict");
		String[] add3s = ServletRequestUtils.getStringParameters(request, "address3");
		String[] addPrefStatus = ServletRequestUtils.getStringParameters(request, "preferred");
		String[] add6s = ServletRequestUtils.getStringParameters(request, "address6");
		String[] add5s = ServletRequestUtils.getStringParameters(request, "address5");
		String[] add4s = ServletRequestUtils.getStringParameters(request, "address4");
		String[] startDates = ServletRequestUtils.getStringParameters(request, "startDate");
		String[] endDates = ServletRequestUtils.getStringParameters(request, "endDate");

		if (add1s != null || add2s != null || cities != null || states != null || countries != null || lats != null
				|| longs != null || pCodes != null || counties != null || add3s != null || add6s != null
				|| add5s != null || add4s != null || startDates != null || endDates != null) {
			int maxAddrs = 0;

			if (add1s != null && add1s.length > maxAddrs) {
				maxAddrs = add1s.length;
			}
			if (add2s != null && add2s.length > maxAddrs) {
				maxAddrs = add2s.length;
			}
			if (cities != null && cities.length > maxAddrs) {
				maxAddrs = cities.length;
			}
			if (states != null && states.length > maxAddrs) {
				maxAddrs = states.length;
			}
			if (countries != null && countries.length > maxAddrs) {
				maxAddrs = countries.length;
			}
			if (lats != null && lats.length > maxAddrs) {
				maxAddrs = lats.length;
			}
			if (longs != null && longs.length > maxAddrs) {
				maxAddrs = longs.length;
			}
			if (pCodes != null && pCodes.length > maxAddrs) {
				maxAddrs = pCodes.length;
			}
			if (counties != null && counties.length > maxAddrs) {
				maxAddrs = counties.length;
			}
			if (add3s != null && add3s.length > maxAddrs) {
				maxAddrs = add3s.length;
			}
			if (add6s != null && add6s.length > maxAddrs) {
				maxAddrs = add6s.length;
			}
			if (add5s != null && add5s.length > maxAddrs) {
				maxAddrs = add5s.length;
			}
			if (add4s != null && add4s.length > maxAddrs) {
				maxAddrs = add4s.length;
			}
			if (startDates != null && startDates.length > maxAddrs) {
				maxAddrs = startDates.length;
			}
			if (endDates != null && endDates.length > maxAddrs) {
				maxAddrs = endDates.length;
			}

			log.debug("There appears to be {} addresses that need to be saved", maxAddrs);

			for (int i = 0; i < maxAddrs; i++) {
				PersonAddress pa = new PersonAddress();
				if (add1s.length >= i + 1) {
					pa.setAddress1(add1s[i]);
				}
				if (add2s.length >= i + 1) {
					pa.setAddress2(add2s[i]);
				}
				if (cities.length >= i + 1) {
					pa.setCityVillage(cities[i]);
				}
				if (states.length >= i + 1) {
					pa.setStateProvince(states[i]);
				}
				if (countries.length >= i + 1) {
					pa.setCountry(countries[i]);
				}
				if (lats.length >= i + 1) {
					pa.setLatitude(lats[i]);
				}
				if (longs.length >= i + 1) {
					pa.setLongitude(longs[i]);
				}
				if (pCodes.length >= i + 1) {
					pa.setPostalCode(pCodes[i]);
				}
				if (counties.length >= i + 1) {
					pa.setCountyDistrict(counties[i]);
				}
				if (add3s.length >= i + 1) {
					pa.setAddress3(add3s[i]);
				}
				if (addPrefStatus != null && addPrefStatus.length > i) {
					pa.setPreferred(new Boolean(addPrefStatus[i]));
				}
				if (add6s.length >= i + 1) {
					pa.setAddress6(add6s[i]);
				}
				if (add5s.length >= i + 1) {
					pa.setAddress5(add5s[i]);
				}
				if (add4s.length >= i + 1) {
					pa.setAddress4(add4s[i]);
				}
				if (startDates.length >= i + 1 && StringUtils.isNotBlank(startDates[i])) {
					pa.setStartDate(Context.getDateFormat().parse(startDates[i]));
				}
				if (endDates.length >= i + 1 && StringUtils.isNotBlank(endDates[i])) {
					pa.setEndDate(Context.getDateFormat().parse(endDates[i]));
				}

				// check if all required address fields are filled
				//Errors addressErrors = new BindException(pa, "personAddress");
				Errors addressErrors = new BeanPropertyBindingResult(pa, "personAddress");
				new PersonAddressValidator().validate(pa, addressErrors);
				if (addressErrors.hasErrors()) {
					for (ObjectError error : addressErrors.getAllErrors()) {
						errors.reject(error.getCode(), error.getArguments(), "");
					}
				}
				if (errors.hasErrors()) {
					return;
				}

				person.addAddress(pa);
			}
			Iterator<PersonAddress> addresses = person.getAddresses().iterator();
			PersonAddress currentAddress = null;
			PersonAddress preferredAddress = null;
			while (addresses.hasNext()) {
				currentAddress = addresses.next();

				// check if all required addres fields are filled
				//Errors addressErrors = new BindException(currentAddress, "personAddress");
				Errors addressErrors = new BeanPropertyBindingResult(currentAddress, "personAddress");
				new PersonAddressValidator().validate(currentAddress, addressErrors);
				if (addressErrors.hasErrors()) {
					for (ObjectError error : addressErrors.getAllErrors()) {
						errors.reject(error.getCode(), error.getArguments(), "");
					}
				}
				if (errors.hasErrors()) {
					return;
				}

				if (currentAddress.isPreferred()) {
					if (preferredAddress != null) { // if there's a preferred address already exists, make it
													// preferred=false
						preferredAddress.setPreferred(false);
					}
					preferredAddress = currentAddress;
				}
			}
			if ((preferredAddress == null) && (currentAddress != null)) { // No preferred address. Make the last address
																			// entry as preferred.
				currentAddress.setPreferred(true);
			}
		}
	}

	/**
	 * Setup the person object. Should be called by the
	 * PersonFormController.formBackingObject(request)
	 * 
	 * @param person
	 * @return the given person object
	 */
	protected Person setupFormBackingObject(Person person) {

		// set a default name and address for the person. This allows us to use
		// person.names[0] binding in the jsp
		if (person.getNames().size() < 1) {
			person.addName(new PersonName());
		}

		if (person.getAddresses().size() < 1) {
			person.addAddress(new PersonAddress());
		}

		// initialize the user/person sets
		// hibernate seems to have an issue with empty lists/sets if they aren't
		// initialized

		person.getAttributes().size();

		return person;
	}

	/**
	 * Setup the reference map object. Should be called by the
	 * PersonFormController.referenceData(...)
	 * 
	 * @param person
	 * @return the given map object
	 */
	protected ModelMap setupReferenceData(ModelMap map, Person person) throws Exception {

		String causeOfDeathOther = "";

		if (Context.isAuthenticated()) {

			String propCause = Context.getAdministrationService().getGlobalProperty("concept.causeOfDeath");
			Concept conceptCause = Context.getConceptService().getConcept(propCause);

			if (conceptCause != null) {
				// TODO add back in for persons
				List<Obs> obssDeath = Context.getObsService().getObservationsByPersonAndConcept(person, conceptCause);
				if (obssDeath.size() == 1) {
					Obs obsDeath = obssDeath.iterator().next();
					causeOfDeathOther = obsDeath.getValueText();
					if (causeOfDeathOther == null) {
						log.debug("cod is null, so setting to empty string");
						causeOfDeathOther = "";
					} else {
						log.debug("cod is valid: {}", causeOfDeathOther);
					}
				} else {
					log.debug("obssDeath is wrong size: {}", obssDeath.size());
				}
			} else {
				log.warn("No concept death cause found");
			}

		}

		map.put("causeOfDeathOther", causeOfDeathOther);

		return map;
	}

	/**
	 * Add the given name, gender, and birthdate/age to the given Person
	 * 
	 * @param <P>    Should be a Patient or User object
	 * @param person
	 * @param name
	 * @param gender
	 * @param date   birthdate
	 * @param age
	 */
	public static <P extends Person> void getMiniPerson(P person, String name, String gender, String date, String age) {

		person.addName(Context.getPersonService().parsePersonName(name));

		person.setGender(gender);
		Date birthdate = null;
		boolean birthdateEstimated = false;
		if (StringUtils.isNotEmpty(date)) {
			try {
				// only a year was passed as parameter
				if (date.length() < 5) {
					Calendar c = Calendar.getInstance();
					c.set(Calendar.YEAR, Integer.valueOf(date));
					c.set(Calendar.MONTH, 0);
					c.set(Calendar.DATE, 1);
					birthdate = c.getTime();
					birthdateEstimated = true;
				}
				// a full birthdate was passed as a parameter
				else {
					birthdate = Context.getDateFormat().parse(date);
					birthdateEstimated = false;
				}
			} catch (ParseException e) {
				log.debug("Error getting date from birthdate", e);
			}
		} else if (age != null && !"".equals(age)) {
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			Integer d = c.get(Calendar.YEAR);
			d = d - Integer.parseInt(age);
			try {
				birthdate = DateFormat.getDateInstance(DateFormat.SHORT).parse("01/01/" + d);
				birthdateEstimated = true;
			} catch (ParseException e) {
				log.debug("Error getting date from age", e);
			}
		}
		if (birthdate != null) {
			person.setBirthdate(birthdate);
		}
		person.setBirthdateEstimated(birthdateEstimated);

	}
}
