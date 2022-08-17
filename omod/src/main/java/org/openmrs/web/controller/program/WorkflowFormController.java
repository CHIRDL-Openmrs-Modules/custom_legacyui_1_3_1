/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.controller.program;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.APIException;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
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
@RequestMapping(value = "admin/programs/workflow.form")
public class WorkflowFormController {

	private static final String FORM_VIEW = "/module/legacyui/admin/programs/workflowForm";
	private static final String SUBMIT_VIEW = "program.list";

	private static final Logger log = LoggerFactory.getLogger(WorkflowFormController.class);

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(java.lang.Integer.class, new CustomNumberEditor(java.lang.Integer.class, true));
	}

	/**
	 * This is called prior to displaying a form for the first time. It tells Spring
	 * the form/command object to load into the request
	 *
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@ModelAttribute("workflow")
	protected Object formBackingObject(HttpServletRequest request) {
		log.debug("called formBackingObject");

		ProgramWorkflow wf = null;

		if (Context.isAuthenticated()) {
			ProgramWorkflowService ps = Context.getProgramWorkflowService();
			String programWorkflowId = request.getParameter("programWorkflowId");
			String programId = request.getParameter("programId");
			if (programWorkflowId != null) {
				Program program = ps.getProgram(Integer.valueOf(programId));
				wf = program.getWorkflow(Integer.valueOf(programWorkflowId));
			}

			if (wf == null) {
				throw new IllegalArgumentException("Can't find workflow");
			}
		}

		if (wf == null) {
			wf = new ProgramWorkflow();
		}

		return wf;
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
	public ModelAndView processSubmit(HttpServletRequest request, @ModelAttribute("workflow") ProgramWorkflow wf)
			throws Exception {
		log.debug("about to save {}", wf);
		HttpSession httpSession = request.getSession();
		String view = FORM_VIEW;
		if (Context.isAuthenticated()) {
			// get list of states to be deleted
			String statesToDelete = request.getParameter("deleteStates");
			Set<Integer> cantBeDeleted = new HashSet<>(); // holds concept ids that cant be deleted
			if (!"".equals(statesToDelete)) {
				// then delete listed states first
				Map<Integer, ProgramWorkflowState> toRemove = new HashMap<>();
				for (StringTokenizer std = new StringTokenizer(statesToDelete, "|"); std.hasMoreTokens();) {
					String str = std.nextToken();
					String[] state = str.split(",");
					Integer conceptIdDelete = Integer.valueOf(state[0]);

					for (ProgramWorkflowState s : wf.getStates()) {
						if (s.getConcept().getConceptId().equals(conceptIdDelete)) {
							toRemove.put(conceptIdDelete, s);
							break;
						}
					}

				}

				for (Map.Entry<Integer, ProgramWorkflowState> remove : toRemove.entrySet()) {
					try {
						wf.removeState(remove.getValue());
						// Context.getProgramWorkflowService().updateWorkflow(wf);
						httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Workflow.saved");
						log.debug("removed {}", remove);
					} catch (DataIntegrityViolationException e) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR,
								"error.object.state.inuse.cannot.delete");
						wf.addState(remove.getValue());
						// add to cant be deleted so it would be skipped from getting retired
						cantBeDeleted.add(remove.getKey());
					} catch (APIException e) {
						httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.general");
						wf.addState(remove.getValue());
						// add to cant be deleted so it would be skipped from getting retired
						cantBeDeleted.add(remove.getKey());
					}
				}

			}
			// get list of states, and update the command object
			String statesStr = request.getParameter("newStates");
			if (!"".equals(statesStr)) {
				// This is a brute-force algorithm, but n will be small.
				Set<Integer> doneSoFar = new HashSet<>(); // concept ids done so far
				for (StringTokenizer st = new StringTokenizer(statesStr, "|"); st.hasMoreTokens();) {
					String str = st.nextToken();
					String[] tmp = str.split(",");
					Integer conceptId = Integer.valueOf(tmp[0]);
					doneSoFar.add(conceptId);
					ProgramWorkflowState pws = null;
					for (ProgramWorkflowState s : wf.getStates()) {
						if (s.getConcept().getConceptId().equals(conceptId)) {
							pws = s;
							break;
						}
					}
					if (pws == null) {
						pws = new ProgramWorkflowState();
						pws.setConcept(Context.getConceptService().getConcept(conceptId));
						wf.addState(pws);
					} else {
						// un-retire if necessary
						if (pws.isRetired()) {
							pws.setRetired(false);
						}
					}
					pws.setInitial(Boolean.valueOf(tmp[1]));
					pws.setTerminal(Boolean.valueOf(tmp[2]));
				}
				// retire states if we didn't see their concept during the loop above
				for (ProgramWorkflowState s : wf.getStates()) {
					if (!doneSoFar.contains(s.getConcept().getConceptId())) {
						s.setRetired(true);
					}
				}
				try {
					Context.getProgramWorkflowService().saveProgram(wf.getProgram());
					httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Workflow.saved");
				} catch (APIException e) {
					httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.general");
				}
			} else {
				// no new state sent therefore retire all excluding deleted
				for (ProgramWorkflowState s : wf.getStates()) {
					if (!cantBeDeleted.contains(s.getConcept().getConceptId())) {
						s.setRetired(true);
					}
				}
				Context.getProgramWorkflowService().saveProgram(wf.getProgram());
			}
		}
		view = SUBMIT_VIEW;
		return new ModelAndView(new RedirectView(view));
	}

	@GetMapping
	public String initForm() {
		return FORM_VIEW;
	}
}
