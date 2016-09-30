package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.service.IngestService;
import com.zorroa.archivist.web.InvalidObjectException;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Created by chambers on 7/9/16.
 */
@RestController
public class IngestController {

    private static final Logger logger = LoggerFactory.getLogger(IngestController.class);

    @Autowired
    IngestService ingestService;

    @RequestMapping(value="/api/v1/ingests", method= RequestMethod.POST)
    public Ingest create(@Valid @RequestBody IngestSpec spec, BindingResult valid) {
        if (valid.hasErrors()) {
            throw new InvalidObjectException("Failed to create Ingest", valid);
        }
        return ingestService.create(spec);
    }

    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.GET)
    public Ingest get(@PathVariable String id) {
        if (StringUtils.isNumeric(id)) {
            return ingestService.get(Integer.parseInt(id));
        }
        else {
            return ingestService.get(id);
        }
    }

    @RequestMapping(value="/api/v1/ingests", method=RequestMethod.GET)
    public PagedList<Ingest> getPaged(@RequestParam(value="page", required=false) Integer page,
                                      @RequestParam(value="count", required=false) Integer count) {
        return ingestService.getAll(new Pager(page, count));
    }

    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.PUT)
    public Object update(@PathVariable Integer id, @Valid @RequestBody Ingest spec, BindingResult valid) {
        checkValid(valid);
        return HttpUtils.updated("ingests", id, ingestService.update(id, spec), ingestService.get(id));
    }

    @RequestMapping(value="/api/v1/ingests/{id}", method=RequestMethod.DELETE)
    public Object delete(@PathVariable Integer id) {
        return HttpUtils.deleted("ingests", id, ingestService.delete(id));
    }

    public static void checkValid(BindingResult valid) {
        if (valid.hasErrors()) {
            throw new InvalidObjectException("Failed to create Ingest", valid);
        }
    }
}
