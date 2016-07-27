package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 7/14/16.
 */
public class DyHierarchyServiceTests extends AbstractTest {

    @Autowired
    FolderService folderService;

    @Autowired
    DyHierarchyService dyhiService;

    @Autowired
    AssetDao assetDao;


    @Before
    public void init() throws ParseException {

        for (File f: getTestImagePath("set01").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            ab.setAttr("source.date",
                    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("04-07-2014 11:22:33"));
            assetDao.index(ab);
        }
        for (File f: getTestPath("office").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            ab.setAttr("source.date",
                    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("03-05-2013 09:11:14"));
            assetDao.index(ab);
        }
        for (File f: getTestPath("video").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            ab.setAttr("source.date",
                    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("11-12-2015 06:14:10"));
            assetDao.index(ab);
        }
        refreshIndex();
    }

    @Test
    public void testGenerate() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));


        dyhiService.generate(agg);
    }

    @Test
    public void testGenerateDateHierarchy() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Month),
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day)));

        dyhiService.generate(agg);

        Folder year = folderService.get(f.getId(), "2014");
        assertEquals(5, searchService.search(year.getSearch()).getHits().getTotalHits());

        Folder month = folderService.get(year.getId(), "July");
        assertEquals(5, searchService.search(month.getSearch()).getHits().getTotalHits());

        Folder day = folderService.get(month.getId(), "4");
        assertEquals(5, searchService.search(day.getSearch()).getHits().getTotalHits());

        year = folderService.get(f.getId(), "2013");
        assertEquals(1, searchService.search(year.getSearch()).getHits().getTotalHits());
    }

    @Test
    public void testGenerateAndUpdate() {

        Folder folder = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(folder.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));

        dyhiService.generate(agg);

        for (File f: getTestImagePath("set02").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetDao.index(ab);
        }

        for (File f: getTestImagePath("set03").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetDao.index(ab);
        }

        refreshIndex();
        dyhiService.generate(agg);
    }
}
