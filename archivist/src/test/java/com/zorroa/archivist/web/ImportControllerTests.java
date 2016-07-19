package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.service.JobService;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 7/19/16.
 */
public class ImportControllerTests extends MockMvcTest {

    @Autowired
    JobService jobService;

    ZpsScript script;

    @Before
    public void init() {
        script = new ZpsScript();
        script.setName("foo-bar");
        jobService.launch(script, PipelineType.Import);
    }

    @Test
    public void testGet() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/imports/" + script.getJobId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Job job = deserialize(result, Job.class);
        assertEquals((int) script.getJobId(), job.getId());
    }

    @Test
    public void testCancel() throws Exception {
        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/imports/" + script.getJobId() + "/_cancel")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> status = deserialize(result, Map.class);
        assertEquals(true, (boolean) status.get("status"));
    }

    @Test
    public void testRestart() throws Exception {
        MockHttpSession session = admin();

        MvcResult cancel = mvc.perform(put("/api/v1/imports/" + script.getJobId() + "/_cancel")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> status = deserialize(cancel, Map.class);
        assertEquals(true, (boolean) status.get("status"));

        MvcResult restart = mvc.perform(put("/api/v1/imports/" + script.getJobId() + "/_restart")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        status = deserialize(restart, Map.class);
        assertEquals(true, (boolean) status.get("status"));
    }
}
