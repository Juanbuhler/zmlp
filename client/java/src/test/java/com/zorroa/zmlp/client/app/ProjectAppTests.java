package com.zorroa.zmlp.client.app;

import com.google.common.collect.Lists;
import com.zorroa.zmlp.client.ApiKey;
import com.zorroa.zmlp.client.Json;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.*;
import com.zorroa.zmlp.client.domain.exception.ZmlpRequestException;
import com.zorroa.zmlp.client.domain.project.Project;
import com.zorroa.zmlp.client.domain.project.ProjectFilter;
import com.zorroa.zmlp.client.domain.project.ProjectSpec;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.zorroa.zmlp.client.UtilsTests.getMockData;
import static org.junit.Assert.assertEquals;

public class ProjectAppTests extends AbstractAppTests {

    ProjectApp projectApp;

    @Before
    public void setup() {
        ApiKey key = new ApiKey("abcd", "1234");
        projectApp = new ProjectApp(
                new ZmlpClient(key, webServer.url("/").toString()));
    }

    @Test
    public void testGetProject() {
        Map<String, Object> body = getProjectBodyMock();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));
        UUID id = UUID.randomUUID();
        Project proj = projectApp.getProject(id);

        assertEquals(body.get("id").toString(), proj.getId().toString());
        assertEquals(body.get("actorCreated"), proj.getActorCreated());
        assertEquals(body.get("actorModified"), proj.getActorModified());
        assertEquals(new Date((Long) body.get("timeCreated")), proj.getTimeCreated());
        assertEquals(new Date((Long) body.get("timeModified")), proj.getTimeModified());
    }

    @Test
    public void testCreateProject409Exception() {
        webServer.enqueue(new MockResponse()
                .setBody(getProjectExceptionMock())
                .setResponseCode(409));

        ProjectSpec unittest = new ProjectSpec().setName("unittest");


        try {
            Project proj = projectApp.createProject(unittest);
        }catch(ZmlpRequestException ex){
            assertEquals("/api/v1/projects", ex.getPath());
            assertEquals("409", ex.getStatus());
            assertEquals("status code: 409, reason phrase: Client Error", ex.getMessage());
        }
    }

    @Test
    public void testCreateProject() {
        Map<String, Object> body = getProjectBodyMock();

        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        ProjectSpec unittest = new ProjectSpec().setName("unittest");
        Project proj = projectApp.createProject(unittest);

        assertEquals(body.get("id").toString(), proj.getId().toString());
        assertEquals(body.get("actorCreated"), proj.getActorCreated());
        assertEquals(body.get("actorModified"), proj.getActorModified());
        assertEquals(new Date((Long) body.get("timeCreated")), proj.getTimeCreated());
        assertEquals(new Date((Long) body.get("timeModified")), proj.getTimeModified());
    }

    @Test
    public void testFindProject() {
        Map<String, Object> body = getProjectBodyMock();

        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        ProjectFilter filter = new ProjectFilter()
                .setIds(Lists.newArrayList(UUID.randomUUID()));
        Project proj = projectApp.findProject(filter);

        assertEquals(body.get("id").toString(), proj.getId().toString());
        assertEquals(body.get("actorCreated"), proj.getActorCreated());
        assertEquals(body.get("actorModified"), proj.getActorModified());
        assertEquals(new Date((Long) body.get("timeCreated")), proj.getTimeCreated());
        assertEquals(new Date((Long) body.get("timeModified")), proj.getTimeModified());
    }

    @Test
    public void testSearchProjects() {
        Map<String, Object> responseProject = getProjectBodyMock();
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("list", Lists.newArrayList(responseProject));
        responseBody.put("page", new Page().setSize(10));

        webServer.enqueue(new MockResponse().setBody(Json.asJson(responseBody)));
        ProjectFilter filter = new ProjectFilter()
                .setIds(Lists.newArrayList(UUID.randomUUID()));
        PagedList<Project> projects = projectApp.searchProjects(filter);

        assertEquals(1, projects.size());
        Project proj = projects.getList().get(0);

        assertEquals(responseProject.get("id").toString(), proj.getId().toString());
        assertEquals(responseProject.get("actorCreated"), proj.getActorCreated());
        assertEquals(responseProject.get("actorModified"), proj.getActorModified());
        assertEquals(new Date((Long) responseProject.get("timeCreated")), proj.getTimeCreated());
        assertEquals(new Date((Long) responseProject.get("timeModified")), proj.getTimeModified());
    }

    private Map<String, Object> getProjectBodyMock() {
        Map<String, Object> body = new HashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("actorCreated", "test@test");
        body.put("actorModified", "test@test");
        body.put("timeCreated", System.currentTimeMillis());
        body.put("timeModified", System.currentTimeMillis());
        return body;
    }

    private String getProjectExceptionMock() {
        return getMockData("mock-create-project-error");
    }

}


