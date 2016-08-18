package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.PluginSpec;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 6/30/16.
 */
public class PluginDaoTests extends AbstractTest {

    @Autowired
    PluginDao pluginDao;

    @Autowired
    PluginService pluginService;

    Plugin plugin;
    PluginSpec spec;

    @Before
    public void init() {
        spec = new PluginSpec();
        spec.setLanguage("java");
        spec.setDescription("description");
        spec.setName("test");
        spec.setVersion("1.0");
        spec.setPublisher("Zorroa Corp 2016");
        plugin = pluginDao.create(spec);
    }

    @Test
    public void testGetAll() {
        assertEquals(1, pluginDao.getAll().size());
    }

    @Test
    public void testGetAllWithPaging() {
        assertEquals(1, (long) pluginDao.getAll(new Paging(1)).getPage().getTotalCount());
        assertEquals(1, (long) pluginDao.getAll(new Paging(1)).getPage().getTotalPages());
        assertEquals(1, (long) pluginDao.getAll(new Paging(1)).getPage().getNumber());
    }

    public void validate(Plugin p, PluginSpec s) {
        assertEquals(s.getName(), p.getName());
        assertEquals(s.getDescription(), p.getDescription());
        assertEquals(s.getLanguage(), p.getLanguage());
        assertEquals(s.getPublisher(), p.getPublisher());
        assertEquals(s.getVersion(), p.getVersion());
    }

    @Test
    public void testGet() {
        Plugin p = pluginDao.get("test");
        validate(p, spec);

        p = pluginDao.get(p.getId());
        validate(p, spec);

        p = pluginDao.refresh(p);
        validate(p, spec);
    }

    @Test
    public void testExists() {
        assertTrue(pluginDao.exists("test"));
        assertFalse(pluginDao.exists("foo"));
    }

    @Test
    public void testCount() {
        assertEquals(1, pluginDao.count());
    }

    @Test(expected=DuplicateKeyException.class)
    public void testCreateFailure() {
        pluginDao.create(spec);
    }

    @Test(expected= EmptyResultDataAccessException.class)
    public void testGetFailure() {
        pluginDao.get("zorroa-foo");
    }
}
