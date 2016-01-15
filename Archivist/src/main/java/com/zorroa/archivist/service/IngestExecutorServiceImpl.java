package com.zorroa.archivist.service;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.ArchivistConfiguration;
import com.zorroa.archivist.AssetExecutor;
import com.zorroa.archivist.domain.BulkAssetUpsertResult;
import com.zorroa.archivist.ingestors.AggregatorIngestor;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.IngestSchema;
import com.zorroa.archivist.sdk.service.EventLogService;
import com.zorroa.archivist.sdk.service.ImageService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.service.MessagingService;
import com.zorroa.archivist.sdk.util.FileUtils;
import org.apache.tika.Tika;
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

    @Autowired
    MessagingService messagingService;

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
         * A queue to store the asset builders generated by asset worker threads.
         * This are added to elasticsearch via bulk operations.
         */
        LinkedBlockingQueue<AssetBuilder> queue = new LinkedBlockingQueue<>();

        /**
         * A timer thread for updating counts.
         */
        private Timer bulkIndexTimer = new Timer();

        private AssetExecutor assetExecutor;

        private final Ingest ingest;

        private boolean earlyShutdown = false;

        private Set<String> supportedFormats = Sets.newHashSet();

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
        public void bulkIndex(int max) {

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

            BulkAssetUpsertResult result = assetDao.bulkUpsert(assets);
            ingestService.incrementIngestCounters(
                    ingest, result.created, result.updated, result.errorsNotRecoverable);
            eventLogService.log(ingest, "Bulk asset indexing result {}", result);
            for (String error: result.errors) {
                eventLogService.log(ingest, error);
            }
        }

        @Override
        public void run() {

            if (!ingestService.setIngestRunning(ingest)) {
                logger.warn("Unable to set ingest {} to the running state.", ingest);
                return;
            }

            List<IngestProcessor> processors = null;
            IngestPipeline pipeline;

            try {
                try {
                    pipeline = ingestService.getIngestPipeline(ingest.getPipelineId());
                    processors = setupIngestProcessors(pipeline);
                }
                catch (Exception e) {
                    /*
                     * Something went wrong setting up the ingestor classes.
                     */
                    logger.warn("Failed to setup the ingest pipeline, unexpected: {}", e.getMessage(), e);
                    eventLogService.log(ingest, "Failed to setup the ingest pipeline", e);
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                    return;
                }

                /*
                 * Start a timer to bulk index any queued work from the asset processing threads.
                 */
                bulkIndexTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        bulkIndex(250);
                    }
                }, 3000, 5000);


                try {
                    walkIngestPaths(ingest, pipeline);
                }
                catch (Exception e) {
                    /*
                     * Something went wrong while walking the file system, however the asset
                     * threads might still be working so we don't want to jump right into
                     * the lower finally block, but wait until the asset threads are done.
                     */
                    eventLogService.log(ingest, "Failed to execute ingest on paths {}", e, ingest.getPaths());
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                }

                /*
                 * Block forever until the queue is empty and all threads
                 * have stopped working.
                 */
                assetExecutor.waitForCompletion();

            } finally {
                /*
                 * Cancel the update timer, then run one fina bulk index.
                 */
                bulkIndexTimer.cancel();
                bulkIndex(-1);

                /*
                 * Force a refresh so the tear downs can see any recently added data.
                 */
                assetDao.refresh();

                /*
                 * Run all of the tear downs
                 */
                if (processors != null) {
                    processors.forEach(p -> p.teardown());
                }

                /*
                 * Remove the current ingest from running ingests.
                 */
                runningIngests.remove(ingest.getId());

                /*
                 * Pull a new copy of the ingest with all updated fields.
                 */
                Ingest finishedIngest = ingestService.getIngest(ingest.getId());

                if (!earlyShutdown) {
                    ingestService.setIngestIdle(finishedIngest);

                    eventLogService.log(finishedIngest, "ingest finished , created {}, updated: {}, errors: {}",
                            finishedIngest.getCreatedCount(), finishedIngest.getUpdatedCount(), finishedIngest.getErrorCount());
                }
                else {
                    eventLogService.log(finishedIngest, "ingest was manually shut down, created {}, updated: {}, errors: {}",
                            finishedIngest.getCreatedCount(), finishedIngest.getUpdatedCount(), finishedIngest.getErrorCount());
                }
            }
        }

        /**
         * Setup the ingest processors for the given pipeline.  This involves
         * creating an instance of the processor and wiring it up with
         * dependencies.
         *
         * Return a list of ready to run IngestProcessors
         *
         * @param pipeline
         * @return
         */
        public List<IngestProcessor> setupIngestProcessors(IngestPipeline pipeline) {

            List<IngestProcessor> processors = Lists.newArrayListWithCapacity(pipeline.getProcessors().size());

            pipeline.getProcessors().add(new ProcessorFactory<>(AggregatorIngestor.class));
            for (ProcessorFactory<IngestProcessor> factory : pipeline.getProcessors()) {
                factory.init();
                IngestProcessor processor = factory.getInstance();
                supportedFormats.addAll(processor.supportedFormats());

                if (processor == null) {
                    throw new IngestException("Aborting ingest, processor not found:" + factory.getKlass());
                }

                AutowireCapableBeanFactory autowire = applicationContext.getAutowireCapableBeanFactory();
                autowire.autowireBean(processor);
                processors.add(processor);
                processor.init(ingest);
            }

            return processors;
        }

        /**
         * Walks the file paths specified on an ingest. When a valid asset is found its handed
         * to the asset processor threads.
         *
         * @param ingest
         * @param pipeline
         * @throws IOException
         */
        private void walkIngestPaths(Ingest ingest, IngestPipeline pipeline) throws IOException {

            for (String path: ingest.getPaths()) {
                Path start = new File(path).toPath();

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

                        if(!supportedFormats.contains(FileUtils.extension(file).toLowerCase())
                                && !supportedFormats.isEmpty()) {
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
            }
        }

        private class AssetWorker implements Runnable {
            private final Tika tika = new Tika();

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

                logger.debug("Ingesting: {}", asset);

                /*
                 * This first block tries to load in past data and determine the
                 * file type of the asset.  The ingest data is then attached
                 * to the asset.
                 */
                try {
                    /*
                     * Set the previous version of the asset.
                     */
                    asset.setPreviousVersion(
                            assetDao.getByPath(asset.getAbsolutePath()));

                    /*
                     * Use Tika to detect the asset type.
                     */
                    asset.getSource().setType(tika.detect(asset.getSource().getPath()));

                    /*
                     * Add or overwrite the ingest info to the asset.
                     */
                    IngestSchema ingestSchema = new IngestSchema();
                    ingestSchema.setId(ingest.getId());
                    ingestSchema.setPipeline(pipeline.getId());
                    asset.addSchema(ingestSchema);

                } catch (Exception e) {
                    eventLogService.log(ingest, "Ingest error '{}', could not determine asset type on '{}'",
                            e, e.getMessage(), asset.getAbsolutePath());
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));

                    /*
                     * Can't go further, return.
                     */
                    return;
                }

                /*
                 * Once we know we have what is on the surface a valid file, we execute
                 * the processors on the asset.  The only exception that can be thrown
                 * from executeProcessors() is an UnrecoverableIngestProcessorException.  All
                 * other exceptions are handled and logged by executeProcessors() but are
                 * not considered critical.
                 */
                try {
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
                    eventLogService.log(ingest, "Critical ingest pipeline error '{}' on asset '{}', Processor: {} failed.",
                            e, e.getMessage(), asset, e.getProcessor().getSimpleName());
                    messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));

                    /*
                     * When an asset is not added/updated, then we increment the error count.  Note that
                     * the bulkIndex() function increments the error count when all processors run
                     * but elastic rejects the data.  This increments the error count when a processor
                     * actually fails.
                     */
                    ingestService.incrementIngestCounters(ingest, 0, 0, 1);
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
                         * This exception short circuits the processor. This is handled above.
                         */
                        throw e;

                    } catch (Exception e) {
                        /*
                         * All other exceptions are just logged and don't bubble out.
                         */
                        eventLogService.log(
                                new EventLogMessage(ingest, "Ingest pipeline error '{}', on asset '{}', Processor '{}' failed.",
                                        e.getMessage(), asset.getAbsolutePath(), factory.getKlassName())
                                        .setPath(asset.getAbsolutePath())
                                        .setException(e));
                        messagingService.broadcast(new Message(MessageType.INGEST_EXCEPTION, ingest));
                    }
                }
            }
        }
    }
}
