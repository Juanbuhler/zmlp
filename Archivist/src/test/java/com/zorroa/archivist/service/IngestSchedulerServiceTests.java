package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.repository.IngestPipelineDao;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Timer;
import java.util.TimerTask;

import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 7/31/15.
 */
public class IngestSchedulerServiceTests extends ArchivistApplicationTests {


    @Autowired
    IngestSchedulerService ingestShedulerService;

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    @Test
    public void testPauseAndResume() {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");
        builder.addToProcessors(new IngestProcessorFactory(
                "com.zorroa.archivist.processors.AssetMetadataProcessor"));
        ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipeline("default"));

        /*
         * Set up a timer to resume the ingest.  This is required due to the fact that
         * ingests run in the main thread during unit tests, so executing a paused
         * ingest would block the main thread forever.
         */
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                assertTrue(ingestSchedulerService.resume(ingest));
            }
        }, 2000);


        ingestSchedulerService.executeIngest(ingest, true);
    }
}
