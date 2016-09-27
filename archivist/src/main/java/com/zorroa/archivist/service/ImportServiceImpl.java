package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.util.FileUtils;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.zorroa.archivist.domain.PipelineType.Import;

/**
 * ImportService provides a simple interface for making Import jobs.
 * An Import itself is just a job running an import pipeline.
 */
@Service
@Transactional
public class ImportServiceImpl implements ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

    @Autowired
    JobService jobService;

    @Autowired
    JobDao jobDao;

    @Autowired
    PipelineService pipelineService;

    @Autowired
    PluginService pluginService;

    @Autowired
    TransactionEventManager transactionEventManager;

    @Autowired
    LogService logService;

    @Override
    public PagedList<Job> getAll(Paging page) {
        return jobService.getAll(page, new JobFilter().setType(PipelineType.Import));
    }

    @Override
    public Job create(DebugImportSpec spec) {
        String syncId = UUID.randomUUID().toString();

        JobSpec jspec = new JobSpec();
        jspec.putToArgs("syncId", syncId);
        jspec.setType(Import);
        jspec.setName(String.format("debugging import by %s (%s)",
                SecurityUtils.getUsername(), FileUtils.filename(spec.getPath())));
        Job job = jobService.launch(jspec);

        List<ProcessorRef> generator = ImmutableList.of(
                new SdkProcessorRef("com.zorroa.sdk.processor.builtin.FileListGenerator")
                        .setArg("paths", ImmutableList.of(spec.getPath())));

        List<ProcessorRef> pipeline = pipelineService.getProcessors(
                spec.getPipelineId(), spec.getPipeline());
        pipeline.add(new SdkProcessorRef("com.zorroa.sdk.processor.builtin.ReturnResponse"));

        ZpsScript script = new ZpsScript();
        script.setGenerate(generator);
        script.setExecute(pipeline);
        script.setInline(true);
        script.setStrict(true);

        jobService.createTask(new TaskSpec().setScript(script)
                .setJobId(job.getJobId())
                .setName("Path Generation"));


        return job;
    }

    @Override
    public Job create(ImportSpec spec) {

        JobSpec jspec = new JobSpec();
        jspec.setType(Import);
        if (spec.getName() == null) {
            jspec.setName(String.format("import by %s", SecurityUtils.getUsername()));
        }
        else {
            jspec.setName(String.format("import ", spec.getName()));
        }

        /**
         * Create the job.
         */
        Job job = jobService.launch(jspec);

        List<ProcessorRef> execute = Lists.newArrayList();

        /**
         * Add an ExpandCollector so we generate right into new tasks.
         */
        execute.add(
                new SdkProcessorRef()
                        .setClassName("com.zorroa.sdk.processor.builtin.ExpandCollector")
                        .setLanguage("java"));

        /**
         * Resolve the user supplied pipeline.
         */
        List<ProcessorRef> pipeline = pipelineService.getProcessors(
                spec.getPipelineId(), spec.getPipeline());
        /**
         * At the end we add an IndexDocumentCollector to index the results of our job.
         */
        pipeline.add(
                new SdkProcessorRef()
                        .setClassName("com.zorroa.sdk.processor.builtin.IndexDocumentCollector")
                        .setLanguage("java")
                        .setArgs(ImmutableMap.of("importId", job.getJobId())));

        /**
         * Now finally, attach the pipeline to the expander as a sub execute list.
         */
        execute.get(0).setExecute(pipeline);

        /**
         * Now attach the pipeline to each generator, be sure to validate each processor
         * since they are coming from the user.
         */
        List<ProcessorRef> generators = Lists.newArrayListWithCapacity(spec.getGenerators().size());
        for (ProcessorRef m: spec.getGenerators()) {
            ProcessorRef gen = pluginService.getProcessorRef(m);
            generators.add(gen);
        }

        /**
         * The execute property holds the current processors to be executed.
         */
        ZpsScript script = new ZpsScript();
        script.setGenerate(generators);
        script.setExecute(execute);

        jobService.createTask(new TaskSpec().setScript(script)
                .setJobId(job.getJobId())
                .setName("Path Generation"));

        transactionEventManager.afterCommitSync(() -> {
            logService.log(LogSpec.build(LogAction.Create, "import", job.getJobId()));
        });

        return job;
    }
}
