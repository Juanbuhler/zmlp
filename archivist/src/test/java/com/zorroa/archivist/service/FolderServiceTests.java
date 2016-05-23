package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.*;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;

import static org.junit.Assert.*;

public class FolderServiceTests extends AbstractTest {

    @Test(expected=EmptyResultDataAccessException.class)
    public void testSetAcl() {
        authenticate("admin");

        FolderBuilder builder = new FolderBuilder("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        folderService.setAcl(folder, new Acl().addEntry(
                userService.getPermission("group::superuser"), Access.Read));
        folderService.get(folder.getId());
    }

    @Test
    public void testCreateAndGet() {
        FolderBuilder builder = new FolderBuilder("Da Kind Assets");
        Folder folder1 = folderService.create(builder);

        Folder folder2 = folderService.get(folder1.getId());
        assertEquals(folder1.getName(), folder2.getName());
    }

    @Test
    public void testDescendants() {
        Folder grandpa = folderService.create(new FolderBuilder("grandpa"));
        Folder dad = folderService.create(new FolderBuilder("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderBuilder("uncle", grandpa.getId()));
        folderService.create(new FolderBuilder("child", dad.getId()));
        folderService.create(new FolderBuilder("cousin", uncle.getId()));
        List<Folder> descendents = folderService.getAllDescendants(grandpa, false);
        assertEquals(4, descendents.size());
    }

    @Test
    public void testGetAllDescendants() {
        Folder grandpa = folderService.create(new FolderBuilder("grandpa"));
        Folder dad = folderService.create(new FolderBuilder("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderBuilder("uncle", grandpa.getId()));
        folderService.create(new FolderBuilder("child", dad.getId()));
        folderService.create(new FolderBuilder("cousin", uncle.getId()));
        assertEquals(5, folderService.getAllDescendants(Lists.newArrayList(grandpa), true, true).size());
        assertEquals(4, folderService.getAllDescendants(Lists.newArrayList(grandpa), false, true).size());

        logger.info("{}", folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), true, true));

        assertEquals(5, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), true, true)).size());
        assertEquals(4, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), false, true)).size());
    }

    @Test
    public void testGetChildren() {
        Folder folder1 = folderService.create(new FolderBuilder("test1"));
        Folder folder1a = folderService.create(new FolderBuilder("test1a", folder1));
        Folder folder1b = folderService.create(new FolderBuilder("test1b", folder1));
        Folder folder1c = folderService.create(new FolderBuilder("test1c", folder1));

        List<Folder> children = folderService.getChildren(folder1);
        assertEquals(3, children.size());
        assertTrue(children.contains(folder1a));
        assertTrue(children.contains(folder1b));
        assertTrue(children.contains(folder1c));
        assertFalse(children.contains(folder1));
    }

    @Test
    public void testGetByPath() {
        Folder folder1 = folderService.create(new FolderBuilder("test1"));
        Folder folder1a = folderService.create(new FolderBuilder("test1a", folder1));
        Folder folder1b = folderService.create(new FolderBuilder("test1b", folder1a));
        Folder folder1c = folderService.create(new FolderBuilder("test1c", folder1b));

        Folder folder = folderService.get("/test1/test1a/test1b/test1c");
        assertEquals(folder1b.getId(), folder.getParentId().intValue());
        assertEquals(folder1c.getName(), folder.getName());
    }

    @Test
    public void testGetByPathFail() {
        Folder folder = folderService.get("/foo/bar/bam");
        assertEquals(null, folder);
    }

    @Test
    public void testExistsByPath() {
        Folder folder1 = folderService.create(new FolderBuilder("test1"));
        Folder folder1a = folderService.create(new FolderBuilder("test1a", folder1));
        Folder folder1b = folderService.create(new FolderBuilder("test1b", folder1a));
        Folder folder1c = folderService.create(new FolderBuilder("test1c", folder1b));

        assertTrue(folderService.exists("/test1/test1a/test1b/test1c"));
        assertTrue(folderService.exists("/test1/test1a"));
        assertFalse(folderService.exists("/testb"));
        assertFalse(folderService.exists("/testb/test123"));
    }

    @Test
    public void testUpdate() {
        Folder folder = folderService.create(new FolderBuilder("orig"));
        boolean ok = folderService.update(folder, new FolderUpdateBuilder().setName("new"));
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertEquals("new", revised.getName());
    }

    @Test
    public void testUpdateWithNewParent() {
        Folder folder1 = folderService.create(new FolderBuilder("orig"));
        Folder folder2 = folderService.create(new FolderBuilder("unorig"));
        boolean ok = folderService.update(folder2, new FolderUpdateBuilder().setParentId(folder1.getId()));
        assertTrue(ok);

        Folder revised = folderService.get(folder2.getId());
        assertEquals(folder1.getId(), revised.getParentId().intValue());

        List<Folder> folders = folderService.getAllDescendants(Lists.newArrayList(folder1), false, false);
        assertTrue(folders.contains(folder2));

        folders = folderService.getAllDescendants(Lists.newArrayList(folder2), false, false);
        assertTrue(folders.isEmpty());
    }

    @Test
    public void testUpdateRecursive() {
        Folder folder = folderService.create(new FolderBuilder("orig"));
        assertTrue(folder.isRecursive());
        boolean ok = folderService.update(folder, new FolderUpdateBuilder().setRecursive(Boolean.FALSE));
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertFalse(revised.isRecursive());
    }

    @Test(expected=DataIntegrityViolationException.class)
    public void testCreateFailureInRoot() {
        FolderBuilder builder = new FolderBuilder("shizzle");
        builder.setExpectCreate(true);
        Folder folder1 = folderService.create(builder);
        folderService.create(builder);
    }

    @Test(expected=DataIntegrityViolationException.class)
    public void testDeepCreateFailure() {
        FolderBuilder builder = new FolderBuilder("shizzle");
        Folder folder1 = folderService.create(builder);
        assertEquals(Folder.ROOT_ID, folder1.getParentId());

        builder = new FolderBuilder("shizzle");
        builder.setParentId(folder1.getId());
        builder.setExpectCreate(true);

        Folder folder2 = folderService.create(builder);
        assertEquals(folder2.getParentId().intValue(), folder1.getId());
        folderService.create(builder);
    }

    @Test
    public void testDelete() {
        FolderBuilder builder = new FolderBuilder("shizzle");
        Folder start = folderService.create(builder);
        Folder root = start;
        for (int i=0; i<10; i++) {
            builder = new FolderBuilder("shizzle"+i, start);
            start = folderService.create(builder);
        }
        assertTrue(folderService.delete(root));
    }
}
