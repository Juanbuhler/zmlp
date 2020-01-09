package com.zorroa.zmlp.sdk.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.zorroa.zmlp.sdk.ApiKey;
import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.PagedList;
import com.zorroa.zmlp.sdk.domain.asset.*;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AssetAppTests extends AbstractAppTests {

    AssetApp assetApp;

    @Before
    public void setup() {

        ApiKey key = new ApiKey(UUID.randomUUID(), "1234");
        assetApp = new AssetApp(
                new ZmlpClient(key, webServer.url("/").toString()));
    }

    @Test
    public void testImportFiles() {

        webServer.enqueue(new MockResponse().setBody(getImportFilesMock()));


        AssetCreateBuilder assetCreateBuilder = new AssetCreateBuilder()
                .addAsset(new AssetSpec("gs://zorroa-dev-data/image/pluto.png"))
                .addAsset("gs://zorroa-dev-data/image/earth.png")
                .withAnalyze(false);

        BatchCreateAssetResponse batchCreateAssetResponse = assetApp.importFiles(assetCreateBuilder);

        assertEquals(batchCreateAssetResponse.getStatus().get(0).getAssetId(), "abc123");
        assertEquals(batchCreateAssetResponse.getStatus().get(0).getFailed(), false);
    }

    @Test
    public void testGetById() {

        webServer.enqueue(new MockResponse().setBody(getGetByIdMock()));

        Asset asset = assetApp.getById("abc123");

        assertNotNull(asset.getId());
        assertNotNull(asset.getDocument());
    }

    @Test
    public void testUploadAssets() {

        webServer.enqueue(new MockResponse().setBody(getUploadAssetsMock()));

        List<AssetSpec> assetSpecList = Arrays.asList(new AssetSpec("../../../zorroa-test-data/images/set01/toucan.jpg"));
        BatchCreateAssetResponse response = assetApp.uploadFiles(assetSpecList);

        assertEquals(response.getStatus().get(0).getAssetId(), "abc123");
    }

    @Test
    public void testSearchRaw() {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        AssetSearch assetSearch =
                new AssetSearch()
                .addSearchParam("match_all", new HashMap());


        Map searchResult = assetApp.rawSearch(assetSearch);
        JsonNode jsonNode = Json.mapper.valueToTree(searchResult);

        String path = jsonNode.get("hits").get("hits").get(0).get("_source").get("source").get("path").asText();

        assertEquals(path, "https://i.imgur.com/SSN26nN.jpg");
    }

    @Test
    public void testSearchElement() {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        Map elementQueryTerms = new HashMap();
        elementQueryTerms.put("element.labels", "cat");

        AssetSearch assetSearch = new AssetSearch()
                .addSearchParam("match_all", new HashMap())
                .addElementQueryParam("terms", elementQueryTerms);

        PagedList<Asset> searchResult = assetApp.search(assetSearch);
        Asset asset = searchResult.get(0);
        String path = asset.getAttr("source.path");

        assertEquals(path, "https://i.imgur.com/SSN26nN.jpg");
    }

    @Test
    public void testSearchWrapped() {

        webServer.enqueue(new MockResponse().setBody(getMockSearchResult()));

        AssetSearch assetSearch = new AssetSearch()
                .addSearchParam("match_all", new HashMap());

        PagedList<Asset> searchResult = assetApp.search(assetSearch);

        Asset asset = searchResult.get(0);
        String nestedValue = asset.getAttr("source.nestedSource.nestedKey");
        String path = asset.getAttr("source.path");

        assertEquals(2, searchResult.getList().size());
        assertEquals("nestedValue", nestedValue);
        assertEquals("https://i.imgur.com/SSN26nN.jpg", path);
    }
    
    // Mocks
    private String getImportFilesMock() {
        return getMockData("mock-import-files");
    }

    private String getGetByIdMock() {
        return getMockData("mock-get-by-id");
    }

    private String getUploadAssetsMock() {
        return getMockData("mock-upload-assets");
    }

    private String getMockSearchResult() {
        return getMockData("mock-search-result");
    }

    private String getMockData(String name) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/" + name + ".json")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to find mock data: " + name, e);
        }
    }
}
