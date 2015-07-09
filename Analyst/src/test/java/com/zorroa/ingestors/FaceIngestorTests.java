package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.AssetBuilder;
import org.junit.Test;

public class FaceIngestorTests extends AssetBuilderTests {

    @Test
    public void testProcess() throws InterruptedException {
        FaceIngestor face = new FaceIngestor();
        for (AssetBuilder asset : testAssets) {
            face.process(asset);
        }
    }

}
