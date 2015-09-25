package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.IngestDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 * The ingest service is responsible for processing IngestRequest objects which
 * results in the creation of assets using the assetService.
 *
 * @author chambers
 *
 */
@Component
public class IngestServiceImpl implements IngestService, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    ApplicationContext applicationContext;

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestDao ingestDao;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Override
    public IngestPipeline createIngestPipeline(IngestPipelineBuilder builder) {
        return ingestPipelineDao.create(builder);
    }

    @Override
    public IngestPipeline getIngestPipeline(String name) {
        return ingestPipelineDao.get(name);
    }

    @Override
    public IngestPipeline getIngestPipeline(int id) {
        return ingestPipelineDao.get(id);
    }

    @Override
    public List<IngestPipeline> getIngestPipelines() {
        return ingestPipelineDao.getAll();
    }

    @Override
    public boolean updateIngestPipeline(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder) {
        return ingestPipelineDao.update(pipeline, builder);
    }

    @Override
    public boolean deleteIngestPipeline(IngestPipeline pipeline) {
        return ingestPipelineDao.delete(pipeline);
    }

    @Override
    public boolean setIngestRunning(Ingest ingest) {
        return ingestDao.setState(ingest, IngestState.Running);
    }

    @Override
    public void resetIngestCounters(Ingest ingest) {
        ingestDao.resetCounters(ingest);
    }

    @Override
    public boolean setIngestIdle(Ingest ingest) {
        return ingestDao.setState(ingest, IngestState.Idle);
    }

    @Override
    public boolean setIngestQueued(Ingest ingest) {
        return ingestDao.setState(ingest, IngestState.Queued);
    }

    @Override
    public boolean setIngestPaused(Ingest ingest) {
        return ingestDao.setState(ingest, IngestState.Paused);
    }

    @Override
    public void updateIngestCounters(Ingest ingest, int created, int updated, int errors) {
        ingestDao.updateCounters(ingest, created, updated, errors);
    }

    @Override
    public void updateIngestStartTime(Ingest ingest, long time) {
        ingest.setTimeStarted(time);
        ingestDao.updateStartTime(ingest, time);
    }

    @Override
    public void updateIngestStopTime(Ingest ingest, long time) {
        ingest.setTimeStopped(time);
        ingestDao.updateStoppedTime(ingest, time);
    }

    @Override
    public Ingest createIngest(IngestBuilder builder) {
        IngestPipeline pipeline = ingestPipelineDao.get(builder.getPipeline());
        return ingestDao.create(pipeline, builder);
    }

    @Override
    public boolean deleteIngest(Ingest ingest) {
        return ingestDao.delete(ingest);
    }

    @Override
    public boolean updateIngest(Ingest ingest, IngestUpdateBuilder builder) {

        // Update active ingest thread counts
        if (ingest.getState() == IngestState.Running && builder.getAssetWorkerThreads() > 0 &&
                ingest.getAssetWorkerThreads() != builder.getAssetWorkerThreads()) {
            ingest.setAssetWorkerThreads(builder.getAssetWorkerThreads());  // set new worker count
            ingestExecutorService.pause(ingest);
            ingestExecutorService.resume(ingest);
        }

        if (builder.getPipeline() != null) {
            builder.setPipelineId(ingestPipelineDao.get(builder.getPipeline()).getId());
        }

        return ingestDao.update(ingest, builder);
    }

    @Override
    public Ingest getIngest(long id) {
        return ingestDao.get(id);
    }

    @Override
    public List<Ingest> getAllIngests() {
        return ingestDao.getAll();
    }

    @Override
    public List<Ingest> getIngests(IngestFilter filter) {
        return ingestDao.getAll(filter);
    }

    @Override
    public List<Ingest> getAllIngests(IngestState state, int limit) {
        return ingestDao.getAll(state, limit);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }
}

