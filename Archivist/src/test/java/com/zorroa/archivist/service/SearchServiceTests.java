package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.AssetService;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 10/30/15.
 */
public class SearchServiceTests extends ArchivistApplicationTests {


    @Autowired
    AssetDao assetDao;

    @Autowired
    SearchService searchService;

    @Autowired
    UserService userService;

    @Autowired
    FolderService folderService;

    @Autowired
    AssetService assetService;

    @Test
    public void testSearchPermissionsMiss() throws IOException {

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        Permission perm = userService.createPermission(new PermissionBuilder().setName("test").setDescription("test"));

        AssetBuilder builder = new AssetBuilder(filepath);
        builder.setSearchPermissions(perm);
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(0, searchService.search(search).getHits().getTotalHits());

    }

    @Test
    public void testSearchPermissionsHit() throws IOException {

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder builder = new AssetBuilder(filepath);
        builder.setSearchPermissions(userService.getPermissions().get(0));
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(1, searchService.search(search).getHits().getTotalHits());

    }

    @Test
    public void testFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(assetBuilder);
        refreshIndex(100);

        assetService.addToFolder(asset1, folder1);
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testRecursiveFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderBuilder("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderBuilder("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(assetBuilder);
        refreshIndex(100);

        assetService.addToFolder(asset1, folder3);
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSmartFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderBuilder("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderBuilder("Characters", folder2);
        builder.setSearch(new AssetSearch("captain america"));
        Folder folder3 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(assetBuilder);
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testLotsOfSmartFolders() throws IOException {

        FolderBuilder builder = new FolderBuilder("people");
        Folder folder1 = folderService.create(builder);

        for (int i=0; i<100; i++) {
            builder = new FolderBuilder("person" + i, folder1);
            builder.setSearch(new AssetSearch("captain america"));
            folderService.create(builder);
        }

        refreshIndex();

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(assetBuilder);
        refreshIndex();

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testGetTotalFileSize() {

        AssetBuilder assetBuilder1 = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder1.getSource().setFileSize(1000);
        AssetBuilder assetBuilder2 = new AssetBuilder(getStaticImagePath() + "/new_zealand_wellington_harbour.jpg");
        assetBuilder2.getSource().setFileSize(1000);

        assetDao.create(assetBuilder1);
        assetDao.create(assetBuilder2);
        refreshIndex();

        long size = searchService.getTotalFileSize(new AssetSearch());
        assertEquals(2000, size);
    }

    @Test
    public void testHighConfidenceSearch() throws IOException {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.setAsync(false);
        assetBuilder.addKeywords(1, false, "zipzoom");
        assetDao.create(assetBuilder);
        refreshIndex();

        /*
         * High confidence words are found at every level.
         */
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom", 1.0)).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom", 0.5)).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom", 0.01)).getHits().getTotalHits());
    }

    @Test
    public void testLowConfidenceSearch() throws IOException {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.setAsync(false);
        assetBuilder.addKeywords(0.1, false, "zipzoom");
        assetDao.create(assetBuilder);
        refreshIndex();

        /*
         * High confidence words are found at every level.
         */
        assertEquals(0, searchService.search(
                new AssetSearch("zipzoom", 1.0)).getHits().getTotalHits());
        assertEquals(0, searchService.search(
                new AssetSearch("zipzoom", 0.5)).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom", 0.01)).getHits().getTotalHits());
    }

    @Test
    public void testNoConfidenceSearch() throws IOException {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.setAsync(false);
        assetBuilder.addKeywords(0.1, false, "zipzoom");
        assetDao.create(assetBuilder);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom", 0.0)).getHits().getTotalHits());

    }

    @Test
    public void testFuzzySearch() throws IOException {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.addKeywords(0.1, false, "zoolander");
        assetDao.create(assetBuilder);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolandar", 0.0).setFuzzy(true)).getHits().getTotalHits());
    }
}
