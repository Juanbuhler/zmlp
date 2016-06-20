package com.zorroa.analyst.service;

import com.google.common.collect.Lists;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.analyst.UnitTestIngestor;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.AnalyzeRequest;
import com.zorroa.sdk.domain.AnalyzeResult;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.exception.IngestException;
import com.zorroa.sdk.processor.ProcessorFactory;
import com.zorroa.sdk.schema.ImportSchema;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Note: we currently only create the mapping in Archivist.
 */
public class AnalyzeServiceTests extends AbstractTest {

    @Autowired
    AnalyzeService analyzeService;

    @Autowired
    AssetDao assetDao;

    File testFile = new File("src/test/resources/toucan.jpg");

    @Test
    public void testAnalyze() {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(new Random().nextInt(9999));
        req.setIngestPipelineId(new Random().nextInt(9999));
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>(UnitTestIngestor.class)));
        req.addToAssets(testFile);

        AnalyzeResult result = analyzeService.inlineAnalyze(req);
        assertEquals(1, result.created);
    }

    @Test
    public void testImportSchemaInitialCreation() {

        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(new Random().nextInt(9999));
        req.setIngestPipelineId(new Random().nextInt(9999));
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>(UnitTestIngestor.class)));
        req.addToAssets(testFile);

        AnalyzeResult result = analyzeService.inlineAnalyze(req);
        assertEquals(1, result.created);
        refreshIndex();

        Asset asset = assetDao.getByPath(testFile.getAbsolutePath());
        assertNotNull(asset);

        ImportSchema schema = asset.getAttr("imports", ImportSchema.class);
        for (ImportSchema.IngestProperties prop: schema) {
            assertTrue(prop.getIngestProcessors().contains(UnitTestIngestor.class.getName()));
            assertEquals((int)req.getIngestId(), prop.getId());
        }
    }
    @Test
    public void testImportSchemaUpdated() {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(1000);
        req.setIngestPipelineId(1000);
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>(UnitTestIngestor.class)));
        req.addToAssets(testFile);

        analyzeService.inlineAnalyze(req);
        refreshIndex();

        req = new AnalyzeRequest();
        req.setIngestId(2000);
        req.setIngestPipelineId(2000);
        req.setProcessors(Lists.newArrayList(
                new ProcessorFactory<>(UnitTestIngestor.class)));
        req.addToAssets(testFile);

        AnalyzeResult result = analyzeService.inlineAnalyze(req);
        assertEquals(1, result.updated);
        refreshIndex();

        Asset asset = assetDao.getByPath(testFile.getAbsolutePath());
        assertNotNull(asset);

        ImportSchema schema = asset.getAttr("imports", ImportSchema.class);
        assertEquals(2, schema.size());
        assertEquals(2, (int) asset.getAttr("unittest.value"));
    }

    @Test
    public void testIgnoredUnsupportedPaths() {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(new Random().nextInt(9999));
        req.setIngestPipelineId(new Random().nextInt(9999));
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>(UnitTestIngestor.class)));
        req.addToAssets(new File("src/test/resources/test.properties"));

        AnalyzeResult result = analyzeService.inlineAnalyze(req);
        assertEquals(0, result.created);
        assertEquals(0, result.updated);
        assertEquals(0, result.tried);
    }

    @Test(expected=IngestException.class)
    public void testAnalyzeInitFailure() {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(new Random().nextInt(9999));
        req.setIngestPipelineId(new Random().nextInt(9999));
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>("com.foo.Bar")));
        req.addToAssets(testFile);

        AnalyzeResult result = analyzeService.inlineAnalyze(req);

    }
}
