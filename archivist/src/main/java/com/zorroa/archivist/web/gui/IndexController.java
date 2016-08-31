package com.zorroa.archivist.web.gui;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.PermissionSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.*;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.search.AssetSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * Created by chambers on 6/3/16.
 */
@Controller
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    PipelineService pipelineService;

    @Autowired
    IngestService ingestService;

    @Autowired
    JobService jobService;

    @Autowired
    ImportService importService;

    @Autowired
    AnalystService analystService;

    @Autowired
    PluginService pluginService;

    @Autowired
    FolderService folderService;

    @Autowired
    LogService logService;

    @Autowired
    SearchService searchService;

    @Autowired
    AssetService assetService;

    @Autowired
    UserService userService;

    @Autowired
    HealthEndpoint healthEndpoint;

    @Autowired
    Validator validator;

    @RequestMapping("/")
    public String index() {
        return "redirect:/gui";
    }

    @RequestMapping("/gui")
    public String index(Model model) {
        standardModel(model);
        model.addAttribute("assetCount", searchService.count(new AssetSearch()));
        model.addAttribute("userCount", userService.getCount());
        model.addAttribute("folderCount", folderService.count());
        model.addAttribute("analystCount", analystService.getCount());
        model.addAttribute("user", SecurityUtils.getUser());
        return "overview";
    }

    @RequestMapping("/login")
    public String login(){
        return "login";
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest req) throws ServletException {
        req.logout();
        return "redirect:/login?logout";
    }

    @RequestMapping("/gui/permissions")
    public String permissions(Model model,
                              @RequestParam(value="page", required=false) Integer page,
                              @RequestParam(value="count", required=false) Integer count) {

        Paging paging = new Paging(page, count);
        standardModel(model);
        model.addAttribute("page", paging);
        model.addAttribute("perms", userService.getPermissions(paging));
        model.addAttribute("permSpec", new PermissionSpec());
        return "permissions";
    }

    @RequestMapping(value="/gui/permissions", method=RequestMethod.POST)
    public String permissions(Model model, @ModelAttribute("permSpec") PermissionSpec permSpec,
                              BindingResult bindingResult) {
        standardModel(model);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            return "permissions";
        }
        else {
            userService.createPermission(permSpec);
            return "redirect:/gui/permissions";
        }
    }

    @RequestMapping("/gui/permissions/{id}")
    public String getPermission(Model model, @PathVariable int id) {
        standardModel(model);
        model.addAttribute("perm", userService.getPermission(id));
        model.addAttribute("permSpec", new PermissionSpec());
        return "permission";
    }

    @RequestMapping("/gui/assets")
    public String assets(Model model,
                         @RequestParam(value="page", required=false) Integer page,
                         @RequestParam(value="count", required=false) Integer count, @RequestParam(value="query", required=false) String query) {

        standardModel(model);
        Paging paging = new Paging(page, count);
        model.addAttribute("page", paging);
        model.addAttribute("assets", searchService.search(paging,
                new AssetSearch(query)));
        return "assets";
    }

    @RequestMapping("/gui/fields")
    public String fields(Model model) {
        standardModel(model);
        model.addAttribute("fields", searchService.getFields());
        return "fields";
    }

    @RequestMapping("/gui/analysts")
    public String analysts(Model model,
                           @RequestParam(value="page", required=false) Integer page,
                           @RequestParam(value="count", required=false) Integer count) {
        standardModel(model);
        Paging paging = new Paging(page, count);
        model.addAttribute("page", paging);
        model.addAttribute("analysts", analystService.getAll(paging));
        return "analysts";
    }

    @RequestMapping("/gui/ingests")
    public String getIngests(Model model) {
        standardModel(model);
        model.addAttribute("pipelines", pipelineService.getAll());
        model.addAttribute("ingests", ingestService.getAll());
        model.addAttribute("ingestForm", new NewIngestForm());
        return "ingests";
    }

    @RequestMapping(value="/gui/ingests", method=RequestMethod.POST)
    public String createIngest(Model model,
                               @Valid @ModelAttribute("ingestForm") NewIngestForm ingestForm,
                               BindingResult bindingResult) {
        standardModel(model);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", true);
            model.addAttribute("pipelines", pipelineService.getAll());
            return "ingests";
        }

        model.addAttribute("ingests", ingestService.getAll());
        model.addAttribute("ingestForm", new NewIngestForm());

        IngestSpec spec = new IngestSpec();
        spec.setName(ingestForm.getName());
        spec.setSchedule(ingestForm.getSchedule());
        spec.setRunNow(ingestForm.isRunNow());
        spec.setAutomatic(ingestForm.isAutomatic());
        spec.setFolderId(ingestForm.getFolderId());
        spec.setPipelineId(ingestForm.getPipelineId());

        for (String path: ingestForm.getPaths()) {
            ProcessorRef ref = pluginService.getProcessorRef("com.zorroa.core.generator.FileSystemGenerator");
            ref.setArg("path", path);
            spec.addToGenerators(ref);
        }

        ingestService.create(spec);

        return "redirect:/gui/ingests";
    }

    @RequestMapping("/gui/pipelines")
    public String pipelines(Model model) {
        standardModel(model);
        model.addAttribute("pipelines", pipelineService.getAll());
        return "pipelines";
    }

    @RequestMapping("/gui/status")
    public String status(Model model) {
        standardModel(model);
        Health health = healthEndpoint.invoke();
        model.addAttribute("health", health.getDetails());
        model.addAttribute("status", health.getDetails());
        return "status";
    }

    @RequestMapping("/gui/logs")
    public String events(Model model,
                         @RequestParam(value="query", required=false) String query,
                         @RequestParam(value="page", required=false) Integer page) {
        standardModel(model);

        LogSearch search = new LogSearch();
        if (query != null) {
            search.setQuery(ImmutableMap.of("query_string", ImmutableMap.of("query", query)));
        }

        Paging paging = new Paging(page);
        model.addAttribute("search", search);
        model.addAttribute("logs", logService.search(search, paging));
        model.addAttribute("page", paging);
        return "logs";
    }

    /**
     * Supports the data necessary for the overall template. (layout.html)
     * @param model
     */
    private void standardModel(Model model) {
        //model.addAttribute("stdJobs", ingestService.getAllIngests(IngestState.Running, 10));
    }
}
