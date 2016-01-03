package com.zorroa.archivist.service;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.AssetExecutor;
import com.zorroa.archivist.processors.AggregatorIngestor;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.EventLogMessage;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.IngestPipeline;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.IngestSchema;
import com.zorroa.archivist.sdk.service.EventLogService;
import com.zorroa.archivist.sdk.service.ImageService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.FileUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

@Component
public class IngestExecutorServiceImpl implements IngestExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    @Autowired
    Client client;

    @Autowired
    IngestService ingestService;

    @Autowired
    ImageService imageService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    EventLogService eventLogService;

    @Value("${archivist.ingest.ingestWorkers}")
    private int ingestWorkerCount;

    private final ConcurrentMap<Integer, IngestWorker> runningIngests = Maps.newConcurrentMap();

    private Executor ingestExecutor;

    @PostConstruct
    public void init() {
        ingestExecutor = Executors.newFixedThreadPool(ingestWorkerCount);
    }

    @Override
    public boolean executeIngest(Ingest ingest) {
        return start(ingest, true /* reset counters */);
    }

    @Override
    public boolean resume(Ingest ingest) {
        return start(ingest, false /*don't reset counters*/);
    }

    protected boolean start(Ingest ingest, boolean firstStart) {
        IngestWorker worker = new IngestWorker(ingest);

        if (runningIngests.putIfAbsent(ingest.getId(), worker) == null) {

            if (firstStart) {
                // Reset counters and start time only on first execute, not restart
                ingestService.resetIngestCounters(ingest);
                ingestService.updateIngestStartTime(ingest, System.currentTimeMillis());
            }

            if (ArchivistConfiguration.unittest) {
                worker.run();
            } else {
                if (!ingestService.setIngestQueued(ingest))
                    return false;
                ingestExecutor.execute(worker);
            }
        } else {
            logger.warn("The ingest is already executing: {}", ingest);
        }

        return true;
    }

    @Override
    public boolean pause(Ingest ingest) {
        if (!shutdown(ingest)) {
            return false;
        }
        ingestService.setIngestPaused(ingest);
        return true;
    }

    @Override
    public boolean stop(Ingest ingest) {
        if (!shutdown(ingest)) {
            return false;
        }
        ingestService.setIngestIdle(ingest);
        return true;
    }

    protected boolean shutdown(Ingest ingest) {
        IngestWorker worker = runningIngests.get(ingest.getId());
        if (worker == null) {
            return false;
        }
        worker.shutdown();
        return true;
    }

    public class IngestWorker implements Runnable {

        /**
         * Counters for creates, updates, and errors.
         */
        private LongAdder createdCount = new LongAdder();
        private LongAdder updatedCount = new LongAdder();
        private LongAdder errorCount = new LongAdder();

        /**
         * A queue to store the asset builders generated by asset worker threads.
         * This are added to elasticsearch via bulk operations.
         */
        LinkedBlockingQueue<AssetBuilder> queue = new LinkedBlockingQueue<>();

        /**
         * A timer thread for updating counts.
         */
        private Timer updateCountsTimer = new Timer();

        private AssetExecutor assetExecutor;

        private final Ingest ingest;

        private boolean earlyShutdown = false;

        public IngestWorker(Ingest ingest) {
            this.ingest = ingest;
            assetExecutor = new AssetExecutor(ingest.getAssetWorkerThreads());
        }

        public void shutdown() {
            earlyShutdown = true;           // Force cleanup at end of ingest
            assetExecutor.shutdownNow();
            try {
                while (!assetExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                logger.warn("Asset processing termination interrupted: " + e.getMessage());
            }
        }

        /**
         * Applied the queued assets to search index using a bulk operation.  If
         * max is greater than zero, only N assets are processed during the run.
         *
         * @param max
         */
        public void drainQueue(int max) {

            List<AssetBuilder> assets = Lists.newArrayListWithCapacity(Math.max(max, 50));

            if (max > 0) {
                queue.drainTo(assets, max);
            }
            else {
                queue.drainTo(assets);
            }

            if (assets.isEmpty()) {
                return;
            }

            eventLogService.log(ingest, "Bulk adding {} assets", assets.size());
            BulkResponse bulk = assetDao.bulkUpsert(assets);

            int index = 0;
            for (BulkItemResponse response: bulk) {
                UpdateResponse result = response.getResponse();
                if (response.isFailed()) {
                    errorCount.increment();
                    eventLogService.log(ingest,
                            "Failed to upsert asset: {}, {}",  assets.get(index).getAbsolutePath(),
                            response.getFailureMessage());
                }
                else if (result.isCreated()) {
                    createdCount.increment();
                }
                else {
                    updatedCount.increment();
                }
                index++;
            }

            ingestService.updateIngestCounters(ingest,
                    createdCount.intValue(),
                    updatedCount.intValue(),
                    errorCount.intValue());
        }

        @Override
        public void run() {


            if (!ingestService.setIngestRunning(ingest)) {
                logger.warn("Unable to set ingest {} to the running state.", ingest);
                return;
            }

            // Keep a list of the processor instances which we'll use for
            // running the tear down later on.
            List<IngestProcessor> processors = Lists.newArrayList();

            /*
             * Gather up the supported formats for each of the processors we're running
             * and use that to filter the list of files that gets sent to the processors.
             * This way we don't even send the work to the asset worker threads.
             */
            Set<String> supportedFormats = Sets.newHashSet();

            try {
                /*
                 * Initialize everything we need to run this ingest
                 */
                IngestPipeline pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());
                pipeline.getProcessors().add(new ProcessorFactory<>(AggregatorIngestor.class));
                for (ProcessorFactory<IngestProcessor> factory : pipeline.getProcessors()) {
                    factory.init();
                    IngestProcessor processor = factory.getInstance();
                    supportedFormats.addAll(processor.supportedFormats());

                    if (processor == null) {
                        String msg = "Aborting ingest, processor not found:" + factory.getKlass();
                        logger.warn(msg);
                        throw new IngestException(msg);
                    }
                    Preconditions.checkNotNull(processor, "The IngestProcessor class: " + factory.getKlass() +
                            " was not found, aborting ingest");

                    AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
                    autowire.autowireBean(processor);
                    processors.add(processor);
                    processor.init(ingest);
                }

                updateCountsTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        drainQueue(250);
                    }
                }, 3000, 5000);

                Path start = new File(ingest.getPath()).toPath();
                Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        if (!file.toFile().isFile()) {
                            return FileVisitResult.CONTINUE;
                        }

                        if (file.getFileName().toString().startsWith(".")) {
                            return FileVisitResult.CONTINUE;
                        }

                        if(!supportedFormats.contains(FileUtils.extension(file)) && !supportedFormats.isEmpty()) {
                            return FileVisitResult.CONTINUE;
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("Found file: {}", file);
                        }

                        AssetWorker assetWorker = new AssetWorker(pipeline, ingest, file);
                        if (ArchivistConfiguration.unittest) {
                            assetWorker.run();
                        } else {
                            assetExecutor.execute(assetWorker);
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException e)
                            throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });

                /*
                 * Block forever until the queue is empty and all threads
                 * have stopped working.
                 */
                assetExecutor.waitForCompletion();

            } catch (Exception e) {
                /*
                 * A catch all for anything that could stop ths thread from running.
                 */
                logger.warn("Failed to execute ingest, unexpected: {}", e.getMessage(), e);

            } finally {
                /*
                 * Cancel the update timer, then run a final draining
                 * of the queue manually.
                 */
                updateCountsTimer.cancel();
                drainQueue(-1);

                /*
                 * Force a refresh so the tear downs can see any recently added data.
                 */
                assetDao.refresh();

                /*
                 * Run all of the tear downs
                 */
                processors.forEach(p->p.teardown());

                /*
                 * Remove the current ingest from running ingests.
                 */
                runningIngests.remove(ingest.getId());

                if (!earlyShutdown) {
                    ingestService.setIngestIdle(ingest);
                    ingestService.updateIngestStopTime(ingest, System.currentTimeMillis());
                    eventLogService.log(ingest, "ingest finished , created {}, updated: {}, errors:{}",
                            createdCount.intValue(), updatedCount.intValue(), errorCount.intValue());
                }
                else {
                    eventLogService.log(ingest, "ingest was manually shut down, created {}, updated: {}, errors:{}",
                            createdCount.intValue(), updatedCount.intValue(), errorCount.intValue());
                }
            }
        }

        private class AssetWorker implements Runnable {

            private final IngestPipeline pipeline;
            private final Ingest ingest;
            private final AssetBuilder asset;

            public AssetWorker(IngestPipeline pipeline, Ingest ingest, Path path) {
                this.pipeline = pipeline;
                this.ingest = ingest;
                this.asset = new AssetBuilder(path.toFile());
            }

            @Override
            public void run() {

                try {
                    logger.debug("Ingesting: {}", asset);

                    /*
                     * Add the ingest info to the asset.
                     */
                    IngestSchema ingestSchema = new IngestSchema();
                    ingestSchema.setId(ingest.getId());
                    ingestSchema.setPipeline(pipeline.getId());
                    asset.addSchema(ingestSchema);

                    /*
                     * Set the previous version of the asset.
                     */
                    asset.setPreviousVersion(
                            assetDao.getByPath(asset.getAbsolutePath()));

                    /*
                     * Run the ingest processors
                     */
                    executeProcessors(ingest);

                    /*
                     * Adds the asset to the queue to be processed.
                     */
                    queue.add(asset);

                }
                catch (UnrecoverableIngestProcessorException e) {
                    /*
                     * If a processor throws an IngestProcessorException that indicates the asset is not processable.
                     * For now we'll log that here, but once we know more about the errors we'll come up with
                     * better ways of handling and/or recovering from them.
                     */
                    String message = "Ingest error {} on Asset '{}', Processor: {}";
                    logger.warn(message, e.getMessage(), asset, e.getProcessor().getSimpleName());
                    eventLogService.log(ingest, message, e, e.getMessage(), asset, e.getProcessor().getSimpleName());
                }
                catch (Exception e) {
                    String message = "Failed to execute ingest, unexpected exception for path '{}'";
                    logger.error(message, asset.getAbsolutePath(), e);
                    eventLogService.log(ingest, message, e, asset.getAbsolutePath());
                }
            }

            public void executeProcessors(Ingest ingest) {
                for (ProcessorFactory<IngestProcessor>  factory : pipeline.getProcessors()) {
                    try {
                        IngestProcessor processor = factory.getInstance();
                        if (!processor.isSupportedFormat(asset.getExtension())) {
                            continue;
                        }
                        logger.debug("running processor: {}", processor.getClass());
                        processor.process(asset);

                    } catch (UnrecoverableIngestProcessorException e) {
                        /*
                         * This exception short circuits the processor.
                         */
                        logger.warn("Processor {} failed to run on asset {}",
                                e.getProcessor().getSimpleName(), asset.getFile(), e);
                        errorCount.increment();
                        throw e;

                    } catch (Exception e) {
                        /*
                         * All other exceptions are just logged.
                         */
                        errorCount.increment();
                        String name = factory.getInstance().getClass().getSimpleName();
                        logger.warn("Processor {} failed to run on asset {}", name, asset.getFile(), e);
                        eventLogService.log(
                                new EventLogMessage(ingest, "Processor {} failed to ingest {}", name, asset.getFile())
                                        .setPath(asset.getAbsolutePath())
                                        .setException(e));
                    }
                }
            }
        }
    }
}
