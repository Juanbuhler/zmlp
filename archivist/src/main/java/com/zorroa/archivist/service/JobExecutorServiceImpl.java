package com.zorroa.archivist.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.cluster.client.WorkerNodeClient;
import com.zorroa.common.cluster.thrift.TaskKillT;
import com.zorroa.common.cluster.thrift.TaskResultT;
import com.zorroa.common.cluster.thrift.TaskStartT;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystState;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.client.exception.ArchivistReadException;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * JobExecutorService is responsible for pulling tasks out of elastic and
 * scheduling them onto analysts.
 *
 * Process constists of:
 *
 * 1. Pull list of analysts with space in their run queue.
 * 2. Pulling list of N tasks from the tasks table in the waiting state.
 * 3. Queue tasks to analyst, updating the task so its not requeued.
 * 4. Handle cases where analysts go down and lose tasks by requeuing them.
 */
@Component
public class JobExecutorServiceImpl extends AbstractScheduledService
        implements JobExecutorService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutorServiceImpl.class);

    @Autowired
    AnalystDao analystDao;

    @Autowired
    TaskDao taskDao;

    @Autowired
    JobService jobService;

    @Autowired
    ApplicationProperties properties;

    private final ExecutorService commandQueue = Executors.newSingleThreadExecutor();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!ArchivistConfiguration.unittest) {
            startAsync();
        }
    }

    @Override
    protected void runOneIteration() throws Exception {
        /**
         * Note that, if this function throws then scheduling will stop
         * so just in case we're not letting anything bubble up from here.
         */
        try {
            checkForUnresponsiveAnalysts();
            checkForExpiredTasks();
        } catch (Exception e) {
            logger.warn("Job executor failed to schedule tasks, ", e);
        }
    }

    @Override
    public Future<List<TaskStartT>> queueWaitingTasks(String url, int count) {
        return commandQueue.submit(() -> getWaitingTasks(url, count));
    }

    @Override
    public List<TaskStartT> getWaitingTasks(String url, int count) {
        if (url == null) {
            throw new ArchivistWriteException("Failed to query for tasks, return URL is null. " +
                    "Analyst may be badly configured");
        }
        if (count < 1) {
            return ImmutableList.of();
        }

        List<TaskStartT> result = Lists.newArrayListWithCapacity(count);
        for (TaskStartT task : taskDao.getWaiting(count)) {
            if (jobService.setTaskQueued(new TaskIdImpl(task), url)) {
                result.add(task);
            }
        }
        if (!result.isEmpty()) {
            String taskIds = result
                    .stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(Collectors.joining(","));
            logger.info("{} asking for {} tasks, returned {}", url, taskIds, result.size());
        }
        return result;
    }

    private final Cache<Integer, SynchronousQueue<Object>> returnQueue = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    @Override
    public void handleResponse(Task task, TaskResultT response) {
        logger.info("Processing job response, id:{}", task.getJobId());
        try {
            SynchronousQueue<Object> queue = returnQueue.asMap().get(task.getJobId());
            if (queue == null) {
                logger.warn("Synchronous queue expired for job: {}", task.getJobId());
                return;
            }
            queue.offer(response.getResult(), 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Waiting thread disappeared for job response ID: {}",
                    task.getJobId(), e);
        }
    }

    @Override
    public Object waitOnResponse(Job job) {
        returnQueue.put(job.getId(), new SynchronousQueue<>());

        /*
         * Wait for the job to complete and submit and object.
         */
        try {
            return returnQueue.asMap().get(job.getId()).poll(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ArchivistReadException("Failed waiting on response, ", e);
        }
    }

    /**
     * Look for analysts that we have not heard from in some time
     *
     * TODO: may need to verify with analyst that its still around.
     */
    public void checkForUnresponsiveAnalysts() {
        long timeout = properties.getInt("archivist.maintenance.analyst.inactiveTimeoutSeconds") * 1000L;
        for (Analyst a: analystDao.getUnresponsive(25, timeout)) {
            logger.warn("Setting analyst {}/{} to DOWN state", a.getUrl(), a.getId());
            analystDao.setState(a.getId(), AnalystState.DOWN);
        }
    }

    /**
     * Look for tasks that have been queued or running without a ping from an analyst.
     *
     * TODO: may need to verify with analyst that its still around.
     */
    public void checkForExpiredTasks() {
        List<Task> expired = taskDao.getOrphanTasks(10, 10, TimeUnit.MINUTES);
        if (!expired.isEmpty()) {
            logger.warn("Found {} expired tasks!", expired.size());
            for (Task task : expired) {
                logger.warn("resetting task {} to Waiting", task.getTaskId());
                jobService.setTaskState(task, TaskState.Waiting);
                /**
                 * TODO: contact analyst and kill
                 */
            }
        }
    }

    @Override
    @Async
    public void retryTask(Task task) {
        if (TaskState.requiresStop(task.getState())) {
            killRunningTaskOnAnalyst(task, TaskState.Waiting);
        }
        else {
            if (jobService.setTaskState(task, TaskState.Waiting)) {
                jobService.decrementStats(task);
            }
        }
    }

    @Override
    @Async
    public void retryAllFailures(JobId job) {
        for (Task task: jobService.getTasks(job.getJobId(), TaskState.Failure)) {
            if (jobService.setTaskState(task, TaskState.Waiting)) {
                jobService.decrementStats(task);
            }
        }
    }

    @Override
    @Async
    public void skipTask(Task task) {
        if (TaskState.requiresStop(task.getState())) {
            killRunningTaskOnAnalyst(task, TaskState.Skipped);
        }
        else {
            jobService.setTaskState(task, TaskState.Skipped);
        }
    }

    @Override
    public boolean cancelJob(JobId job) {
        boolean result = jobService.setJobState(job, JobState.Cancelled, JobState.Active);

        for (Task task: taskDao.getAll(job.getJobId(),
                new TaskFilter().setStates(ImmutableSet.of(TaskState.Queued, TaskState.Running)))) {
            retryTask(task);
        }
        return result;
    }

    @Override
    public boolean restartJob(JobId job) {
        return jobService.setJobState(job, JobState.Active, JobState.Cancelled);
    }

    public void killRunningTaskOnAnalyst(Task task, TaskState newState) {
        WorkerNodeClient client = new WorkerNodeClient(task.getHost());
        try {
            logger.info("Killing runinng task: {}", task);
            client.killTask(new TaskKillT().setId(task.getTaskId())
                    .setReason("Manually killed  by " + SecurityUtils.getUsername())
                    .setUser(SecurityUtils.getUsername()));
        } catch (Exception e) {
            logger.warn("Failed to kill running task an analyst {}", task.getHost(), e);
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(10, 2, TimeUnit.SECONDS);
    }
}
