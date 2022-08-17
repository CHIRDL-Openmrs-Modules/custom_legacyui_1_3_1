/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleConstants;
import org.openmrs.module.ModuleException;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.ModuleFileParser;
import org.openmrs.module.ModuleUtil;
import org.openmrs.module.web.WebModuleUtil;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.WebConstants;
import org.openmrs.web.WebUtil;
import org.openmrs.web.dwr.OpenmrsDWRServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller that backs the /admin/modules/modules.list page. This controller makes a list of
 * modules available and lets the user start, stop, and unload modules one at a time.
 */
@Controller
@RequestMapping(value = "admin/modules/module.list")
public class ModuleListController implements ServletContextAware {

    private static final String FORM_VIEW = "/module/legacyui/admin/modules/moduleList";
    private static final String SUBMIT_VIEW = "module.list";
    
    /**
     * Logger for this class and subclasses
     */
    private static final Logger log = LoggerFactory.getLogger(ModuleListController.class);

    ServletContext servletContext;

    /**
     * Handles the submission of the Module List form.
     *
     * @param request The HTTP request information
     * @return The name of the next view
     */
    @PostMapping
    public ModelAndView processSubmit(HttpServletRequest request) throws Exception {
        
        if (!Context.hasPrivilege(PrivilegeConstants.MANAGE_MODULES)) {
            throw new APIAuthenticationException("Privilege required: " + PrivilegeConstants.MANAGE_MODULES);
        }
        
        HttpSession httpSession = request.getSession();
        String moduleId = ServletRequestUtils.getStringParameter(request, "moduleId", "");
        String success = "";
        String error = "";
        MessageSourceService mss = Context.getMessageSourceService();
        
        String action = ServletRequestUtils.getStringParameter(request, "action", "");
        if (ServletRequestUtils.getStringParameter(request, "start.x", null) != null) {
            action = "start";
        } else if (ServletRequestUtils.getStringParameter(request, "stop.x", null) != null) {
            action = "stop";
        } else if (ServletRequestUtils.getStringParameter(request, "unload.x", null) != null) {
            action = "unload";
        }
        
        // handle module upload
        if ("upload".equals(action)) {
            // double check upload permissions
            if (!ModuleUtil.allowAdmin()) {
                error = mss.getMessage("Module.disallowUploads",
                    new String[] { ModuleConstants.RUNTIMEPROPERTY_ALLOW_ADMIN }, Context.getLocale());
            } else {
                InputStream inputStream = null;
                File moduleFile = null;
                Module module = null;
                Boolean updateModule = ServletRequestUtils.getBooleanParameter(request, "update", false);
                Boolean downloadModule = ServletRequestUtils.getBooleanParameter(request, "download", false);
                List<Module> dependentModulesStopped = null;
                try {
                    if (downloadModule) {
                        String downloadURL = request.getParameter("downloadURL");
                        if (downloadURL == null) {
                            throw new MalformedURLException("Couldn't download module because no url was provided");
                        }
                        String fileName = downloadURL.substring(downloadURL.lastIndexOf("/") + 1);
                        final URL url = new URL(downloadURL);
                        inputStream = ModuleUtil.getURLStream(url);
                        moduleFile = ModuleUtil.insertModuleFile(inputStream, fileName);
                    } else if (request instanceof MultipartHttpServletRequest) {
                        
                        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
                        MultipartFile multipartModuleFile = multipartRequest.getFile("moduleFile");
                        if (multipartModuleFile != null && !multipartModuleFile.isEmpty()) {
                            String filename = WebUtil.stripFilename(multipartModuleFile.getOriginalFilename());
                            // if user is using the "upload an update" form instead of the main form
                            if (updateModule) {
                                // parse the module so that we can get the id
                                
                                Module tmpModule = new ModuleFileParser(multipartModuleFile.getInputStream()).parse();
                                Module existingModule = ModuleFactory.getModuleById(tmpModule.getModuleId());
                                if (existingModule != null) {
                                    dependentModulesStopped = ModuleFactory.stopModule(existingModule, false, true); // stop the module with these parameters so that mandatory modules can be upgraded
                                    
                                    for (Module depMod : dependentModulesStopped) {
                                        WebModuleUtil.stopModule(depMod, this.servletContext, true);
                                    }
                                    
                                    WebModuleUtil.stopModule(existingModule, this.servletContext, true);
                                    ModuleFactory.unloadModule(existingModule);
                                }
                                inputStream = new FileInputStream(tmpModule.getFile());
                                moduleFile = ModuleUtil.insertModuleFile(inputStream, filename); // copy the omod over to the repo folder
                            } else {
                                // not an update, or a download, just copy the module file right to the repo folder
                                inputStream = multipartModuleFile.getInputStream();
                                moduleFile = ModuleUtil.insertModuleFile(inputStream, filename);
                            }
                        }
                    }
                    module = ModuleFactory.loadModule(moduleFile);
                }
                catch (ModuleException me) {
                    log.warn("Unable to load and start module", me);
                    error = me.getMessage();
                }
                finally {
                    // clean up the module repository folder
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                    catch (IOException io) {
                        log.warn("Unable to close temporary input stream", io);
                    }
                    
                    if (module == null && moduleFile != null) {
                        moduleFile.delete();
                    }
                }
                
                // if we didn't have trouble loading the module, start it
                if (module != null) {
                    ModuleFactory.startModule(module);
                    boolean someModuleNeedsARefresh = WebModuleUtil.startModule(module, this.servletContext, true);
                    if (module.isStarted()) {
                        success = mss.getMessage("Module.loadedAndStarted", new String[] { module.getName() }, Context.getLocale());
                        
                        if (updateModule && dependentModulesStopped != null) {
                            for (Module depMod : sortStartupOrder(dependentModulesStopped)) {
                                ModuleFactory.startModule(depMod);
                                boolean thisModuleCausesRefresh = WebModuleUtil.startModule(depMod, this.servletContext, true);
                                someModuleNeedsARefresh = someModuleNeedsARefresh || thisModuleCausesRefresh;
                                if (someModuleNeedsARefresh) {
                                    WebModuleUtil.loadServlets(depMod, this.servletContext);
                                    WebModuleUtil.loadFilters(depMod, this.servletContext);
                                }
                            }
                        }

                        if (someModuleNeedsARefresh) {
                            WebModuleUtil.loadServlets(module, this.servletContext);
                            WebModuleUtil.loadFilters(module, this.servletContext);
                            WebModuleUtil.refreshWAC(this.servletContext, false, module);
                            ((OpenmrsDWRServlet)WebModuleUtil.getServlet("dwr-invoker")).reInitServlet();
                        }
                        
                    } else {
                        success = mss.getMessage("Module.loaded", new String[] { module.getName() }, Context.getLocale());
                    }
                }
            }
        } else if ("".equals(moduleId)) {
            if (action.equals(mss.getMessage("Module.startAll"))) {
                boolean someModuleNeedsARefresh = false;
                Collection<Module> modules = ModuleFactory.getLoadedModules();
                Collection<Module> modulesInOrder = ModuleFactory.getModulesInStartupOrder(modules);
                for (Module module : modulesInOrder) {
                    if (ModuleFactory.isModuleStarted(module)) {
                        continue;
                    }
                    
                    ModuleFactory.startModule(module);
                    boolean thisModuleCausesRefresh = WebModuleUtil.startModule(module, this.servletContext, true);
                    someModuleNeedsARefresh = someModuleNeedsARefresh || thisModuleCausesRefresh;
                    if (someModuleNeedsARefresh) {
                        WebModuleUtil.loadServlets(module, this.servletContext);
                        WebModuleUtil.loadFilters(module, this.servletContext);
                    }
                }
                
                if (someModuleNeedsARefresh) {
                    WebModuleUtil.refreshWAC(this.servletContext, false, null);
                    ((OpenmrsDWRServlet)WebModuleUtil.getServlet("dwr-invoker")).reInitServlet();
                }
            } else {
                ModuleUtil.checkForModuleUpdates();
            }
        } else if (action.equals(mss.getMessage("Module.installUpdate"))) {
            // download and install update
            if (!ModuleUtil.allowAdmin()) {
                error = mss.getMessage("Module.disallowAdministration",
                    new String[] { ModuleConstants.RUNTIMEPROPERTY_ALLOW_ADMIN }, Context.getLocale());
            }
            Module mod = ModuleFactory.getModuleById(moduleId);
            if (mod.getDownloadURL() != null) {
                ModuleFactory.stopModule(mod, false, true); // stop the module with these parameters so that mandatory modules can be upgraded
                WebModuleUtil.stopModule(mod, this.servletContext);
                Module newModule = ModuleFactory.updateModule(mod);
                WebModuleUtil.startModule(newModule, this.servletContext, false);
            }
        } else { // moduleId is not empty
            if (!ModuleUtil.allowAdmin()) {
                error = mss.getMessage("Module.disallowAdministration",
                    new String[] { ModuleConstants.RUNTIMEPROPERTY_ALLOW_ADMIN }, Context.getLocale());
            } else {
                log.debug("Module id: {}", moduleId);
                Module mod = ModuleFactory.getModuleById(moduleId);
                
                // Argument to pass to the success/error message
                Object[] args = new Object[] { moduleId };
                
                if (mod == null) {
                    error = mss.getMessage("Module.invalid", args, Context.getLocale());
                } else {
                    if ("stop".equals(action)) {
                        mod.clearStartupError();
                        ModuleFactory.stopModule(mod);
                        WebModuleUtil.stopModule(mod, this.servletContext);
                        success = mss.getMessage("Module.stopped", args, Context.getLocale());
                    } else if ("start".equals(action)) {
                        ModuleFactory.startModule(mod);
                        WebModuleUtil.startModule(mod, this.servletContext, false);
                        if (mod.isStarted()) {
                            success = mss.getMessage("Module.started", args, Context.getLocale());
                        } else {
                            error = mss.getMessage("Module.not.started", args, Context.getLocale());
                        }
                    } else if ("unload".equals(action)) {
                        if (ModuleFactory.isModuleStarted(mod)) {
                            ModuleFactory.stopModule(mod); // stop the module so that when the web stop is done properly
                            WebModuleUtil.stopModule(mod, this.servletContext);
                        }
                        ModuleFactory.unloadModule(mod);
                        success = mss.getMessage("Module.unloaded", args, Context.getLocale());
                    }
                }
            }
        }

        if (!"".equals(success)) {
            httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success);
        }
        
        if (!"".equals(error)) {
            httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error);
        }

        return new ModelAndView(new RedirectView(SUBMIT_VIEW));
    }
    
    /**
     * @param modulesToStart
     * @return a new list, with the same elements as modulesToStart, sorted so that no module is before a module it depends on
     * @should sort modules correctly
     */
    List<Module> sortStartupOrder(List<Module> modulesToStart) {
        // can't use Collections.sort--we need a slower algorithm that guarantees to compare every pair of elements
        List<Module> candidates = new LinkedList<>(modulesToStart);
        List<Module> ret = new ArrayList<>();
        while (!candidates.isEmpty()) {
            Module mod = removeModuleWithNoDependencies(candidates);
            if (mod == null) {
                log.warn("Unable to determine suitable startup order for {}", modulesToStart);
                return modulesToStart;
            }
            ret.add(mod);
        }
        return ret;
    }
    
    /**
     * Looks for a module in the list that doesn't depend on any other modules in the list.
     * If any is found, that module is removed from the list and returned.
     *
     * @param candidates
     * @return
     */
    private static Module removeModuleWithNoDependencies(List<Module> candidates) {
        for (Iterator<Module> i = candidates.iterator(); i.hasNext();) {
            Module candidate = i.next();
            boolean suitable = true;
            for (Module other : candidates) {
                if (candidate.getRequiredModules().contains(other.getPackageName())) {
                    suitable = false;
                    break;
                }
            }
            if (suitable) {
                i.remove();
                return candidate;
            }
        }
        return null;
    }
    
    /**
     * This is called prior to displaying a form for the first time. It tells Spring the
     * form/command object to load into the request
     */
    @ModelAttribute("moduleList")
    protected Object formBackingObject() {

        Collection<Module> modules = ModuleFactory.getLoadedModules();

        log.info("Returning {} modules", modules.size());

        return modules;
    }

    @GetMapping
    public String initForm(ModelMap map) {
        MessageSourceService mss = Context.getMessageSourceService();

        map.put("allowAdmin", ModuleUtil.allowAdmin().toString());
        map.put("disallowUploads", mss.getMessage("Module.disallowUploads",
            new String[] { ModuleConstants.RUNTIMEPROPERTY_ALLOW_ADMIN }, Context.getLocale()));

        map.put("openmrsVersion", OpenmrsConstants.OPENMRS_VERSION_SHORT);
        map.put("moduleRepositoryURL", WebConstants.MODULE_REPOSITORY_URL);

        map.put("loadedModules", ModuleFactory.getLoadedModules());

        return FORM_VIEW;
    }
    
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
}
