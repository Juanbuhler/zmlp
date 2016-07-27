package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpec;
import com.zorroa.archivist.service.PipelineService;
import com.zorroa.archivist.web.InvalidObjectException;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class PipelineController {

    private static final Logger logger = LoggerFactory.getLogger(PipelineController.class);

    @Autowired
    PipelineService pipelineService;

    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.POST)
    public Pipeline create(@Valid @RequestBody PipelineSpec spec, BindingResult valid) {
        if (valid.hasErrors()) {
            throw new InvalidObjectException("Failed to create pipeline", valid);
        }
        return pipelineService.create(spec);
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.GET)
    public Pipeline get(@PathVariable String id) {
        if (StringUtils.isNumeric(id)) {
            return pipelineService.get(Integer.parseInt(id));
        }
        else {
            return pipelineService.get(id);
        }
    }

    @RequestMapping(value="/api/v1/pipelines", method=RequestMethod.GET)
    public PagedList<Pipeline> getPaged(@RequestParam(value="page", required=false) Integer page,
                                      @RequestParam(value="count", required=false) Integer count) {
        return pipelineService.getAll(new Paging(page, count));
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.PUT)
    public Object update(@PathVariable Integer id, @Valid @RequestBody Pipeline spec, BindingResult valid) {
        checkValid(valid);
        boolean result = pipelineService.update(id, spec);
        return ImmutableMap.of("result", result, "object", pipelineService.get(id));
    }

    @RequestMapping(value="/api/v1/pipelines/{id}", method=RequestMethod.DELETE)
    public Object delete(@PathVariable Integer id) {
        boolean result = pipelineService.delete(id);
        return ImmutableMap.of("result", result);
    }

    public static void checkValid(BindingResult valid) {
        if (valid.hasErrors()) {
            throw new InvalidObjectException("Failed to create pipeline", valid);
        }
    }
}
