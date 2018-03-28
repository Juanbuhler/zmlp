package com.zorroa.analyst.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.ImmutableList
import com.google.common.collect.Maps
import com.google.common.util.concurrent.AbstractScheduledService
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.zorroa.analyst.cluster.ClusterProcess
import com.zorroa.analyst.isUnitTest
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.cluster.client.ClusterConnectionException
import com.zorroa.cluster.client.ClusterException
import com.zorroa.cluster.client.MasterServerClient
import com.zorroa.cluster.thrift.*
import com.zorroa.cluster.zps.MetaZpsExecutor
import com.zorroa.cluster.zps.ZpsTask
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.sdk.processor.Reaction
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.util.Json
import com.zorroa.sdk.util.StringUtils
import com.zorroa.sdk.zps.ZpsError
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PreDestroy

interface ProcessManagerService {

    fun getTaskIds(): List<String>

    fun queueClusterTask(task: TaskStartT): ClusterProcess

    @Throws(IOException::class)

    fun executeClusterTask(task: TaskStartT): TaskResultT?

    fun kill(kill: TaskKillT)

    fun killAllTasks()
}
/**
 * Created by chambers on 5/5/17.
 */
@Component
class ProcessManagerServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val analyzeExecutor: ExecutorService,
        private val networkEnvironment: NetworkEnvironment,
        private val appContext: ApplicationContext
): AbstractScheduledService(), ApplicationListener<ContextRefreshedEvent>, ProcessManagerService {

    @Value("\${analyst.executor.idleMinutesShutdown}")
    private val autoShutdownIdleMinutes: Int = 0

    /**
     * The last time a group of tasks was successfully pulled.
     */
    private var autoShutdownMarker = System.currentTimeMillis()

    /**
     * Executor for handling task manipulation commands that
     * could encounter blocking IO. (hung mounts, etc). We don't want to block
     * thrift threads with these commands.
     */
    private val asyncCommandExecutor = Executors.newSingleThreadExecutor()

    @Value("\${analyst.executor.enabled}")
    private var executeEnabled: Boolean = false

    private val processMap = Maps.newConcurrentMap<String, ClusterProcess>()

    private var hostList: List<String>? = null

    private var hostListLoadedTime: Long = 0

    private val connectioncCache = CacheBuilder.newBuilder()
            .initialCapacity(100)
            .concurrencyLevel(1)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(object : CacheLoader<String, Connection>() {
                @Throws(Exception::class)
                override fun load(addr: String): Connection {
                    val client = MasterServerClient(addr)
                    client.maxRetries = 0
                    client.socketTimeout = 2000
                    client.connectTimeout = 2000
                    return Connection(client)

                }
            })

    override fun getTaskIds(): List<String> {
        return ImmutableList.copyOf(processMap.keys)
    }

    override fun queueClusterTask(task: TaskStartT): ClusterProcess {
        val process = ClusterProcess(task)
        if ((processMap as java.util.Map<String, ClusterProcess>).putIfAbsent(task.getId(), process) != null) {
            logger.warn("The task {} is already queued or executing.", task)
            throw ClusterException("The task is already queued or executing.")
        }
        analyzeExecutor.execute {
            try {
                if (properties.getBoolean("analyst.executor.docker.enabled")) {
                    runDockerClusterProcess(process)
                }
                else {
                    runClusterProcess(process)
                }
            } catch (e: IOException) {
                logger.warn("Failed to run cluster process: ", e)
            }
        }
        return process
    }

    @Throws(IOException::class)
    override fun executeClusterTask(task: TaskStartT): TaskResultT? {
        val p = ClusterProcess(task)
        return runClusterProcess(p)
    }

    override fun kill(kill: TaskKillT) {
        asyncCommandExecutor.execute {

            val p = processMap[kill.getId()]
            if (p == null) {
                logger.warn("The task {} was not queued or executing, {}", kill)
                /**
                 * TODO: The task might be in-flight, need to keep a record of it.
                 * and kill it when it comes in.
                 */
            }
            else {
                val mze = p.zpsExecutor
                mze?.let {
                    if (mze.cancel()) {
                        p.setKilled(true)
                        logger.info("The task {} was killed by:{}, reason: {}",
                                kill.getId(), kill.getUser(), kill.getReason())

                        try {
                            Files.write(Paths.get(p.task.getLogPath()), ImmutableList.of("Process killed, reason: " + kill.getReason()),
                                    StandardOpenOption.APPEND)
                        } catch (e: IOException) {
                            logger.warn("Failed to kill process: {}", p.id, e)
                        }
                    }
                }
            }
        }
    }

    override fun killAllTasks() {

    }

    @Throws(IOException::class)
    private fun runDockerClusterProcess(proc: ClusterProcess): TaskResultT? {
        logger.info("Starting task {}", proc.id)

        val task = proc.task
        val result = AtomicReference(TaskResultT())
        var tmpScript: File? = null
        var exitStatus = -1

        try {

            if (!passesPreflightChecks(proc)) {
                return null
            }

            tmpScript = saveTempZpsScript(task)
            createLogDirectory(task)

            val zpsTask = ZpsTask()
            zpsTask.id = StringUtils.uuid(proc.id)
            zpsTask.args = Json.deserialize(task.getArgMap(), Json.GENERIC_MAP)
            zpsTask.env = task.getEnv()
            zpsTask.logPath = task.getLogPath()
            zpsTask.workPath = task.getWorkDir()
            zpsTask.scriptPath = task.getScriptPath()

            if (task.getId() == null) {
                proc.client.reportTaskStarted(task.getId())
            }

            runDocker(zpsTask, task.sharedDir, task.masterHost)

        } finally {
            if (tmpScript != null) {
                tmpScript.delete()
            }

            if (proc.id != null) {
                // interactive tasks are not in the process map.
                if (processMap.remove(task.getId()) != null && task.getId() != null) {
                    val stop = TaskStopT()
                    stop.setExitStatus(exitStatus)
                    proc.client.reportTaskStopped(task.getId(), stop)
                    proc.client.close()
                }
            }
        }

        return result.get()
    }

    private fun runDocker(zpsTask: ZpsTask, sharedDir: String, masterHost : String) {
        val image = properties.getString("analyst.executor.docker.default-image")
        val zpsFile = File(zpsTask.scriptPath + "/zpsT" + zpsTask.id + ".json")
        Json.Mapper.writeValue(zpsFile, zpsTask)

        // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
        val docker = DefaultDockerClient("unix:///var/run/docker.sock")

        val env = mutableListOf<String>()
        for ((k,v) in zpsTask.env) {
            env.add("$k=$v")
        }

        val hostConfig =
                HostConfig.builder()
                        .appendBinds("$sharedDir:$sharedDir")
                        .appendBinds(HostConfig.Bind.from("/Volumes")
                                .to("/Volumes")
                                .readOnly(true)
                                .build())
                        .build()

        val containerConfig = ContainerConfig.builder()
                .image(image)
                .hostConfig(hostConfig)
                .addVolume(sharedDir)
                .addVolume("/Volumes")
                .env(env)
                .cmd("sh", "-c", "while :; do sleep 1; done")
                .attachStdout(true)
                .build()

        val creation = docker.createContainer(containerConfig)
        val id = creation.id()
        docker.inspectContainer(id)

        // Start container
        docker.startContainer(id)

        // Exec command inside running container with attached STDOUT and STDERR
        val command = arrayOf(
                "java",
                "-cp", "/opt/zorroa/cluster-0.39.0.jar",
                "com.zorroa.cluster.tools.ZpsRunKt",
                "-t", zpsFile.absolutePath,
                "-s", sharedDir,
                "-a", masterHost)

        val execCreation = docker.execCreate(
                id, command, DockerClient.ExecCreateParam.attachStdout(),
                DockerClient.ExecCreateParam.attachStderr())
        val output = docker.execStart(execCreation.id())
        val execOutput = output.readFully()
        logger.info("OUTPUT: container $id output: $execOutput")

        docker.killContainer(id)

        // Remove container
        docker.removeContainer(id)

        // Close the docker client
        docker.close()
    }


    @Throws(IOException::class)
    private fun runClusterProcess(proc: ClusterProcess): TaskResultT? {
        logger.info("Starting task {}", proc.id)

        val task = proc.task
        val result = AtomicReference(TaskResultT())
        var tmpScript: File? = null
        var exitStatus = -1

        try {

            if (!passesPreflightChecks(proc)) {
                return null
            }

            tmpScript = saveTempZpsScript(task)
            createLogDirectory(task)

            val zpsTask = ZpsTask()
            zpsTask.id = StringUtils.uuid(task.getId())
            zpsTask.args = Json.deserialize(task.getArgMap(), Json.GENERIC_MAP)
            zpsTask.env = task.getEnv()
            zpsTask.logPath = task.getLogPath()
            zpsTask.workPath = task.getWorkDir()
            zpsTask.scriptPath = task.getScriptPath()

            val zps = MetaZpsExecutor(zpsTask, SharedData(task.getSharedDir()))
            zps.addReactionHandler { zpsTask1, sharedData, reaction ->

                /**
                 * If the task is interactive, then response and errors
                 * are handled here.
                 */
                if (task.getId() == null) {
                    if (reaction.response != null) {
                        result.get().setResult(Json.serialize(reaction.response))
                    } else if (reaction.error != null) {
                        result.get().addToErrors(newTaskError(reaction.error))
                    }
                } else {
                    handleZpsReaction(zpsTask1, sharedData, reaction)
                }
            }

            proc.zpsExecutor = zps
            if (!passesPreflightChecks(proc)) {
                return null
            }

            if (task.getId() != null) {
                proc.client.reportTaskStarted(task.getId())
            }
            exitStatus = zps.execute()
            proc.exitStatus = exitStatus

        } catch (e:Exception) {
            logger.warn("Failed to start task: {}", proc.id, e)
            throw IOException("Failed to start task " + proc.id, e)

        } finally {
            if (tmpScript != null) {
                tmpScript.delete()
            }
            if (proc.id != null) {
                // interactive tasks are not in the process map.
                if (processMap.remove(task.getId()) != null && task.getId() != null) {
                    val stop = TaskStopT()
                    stop.setExitStatus(exitStatus)
                    stop.setKilled(proc.killed)
                    proc.client.reportTaskStopped(task.getId(), stop)
                    proc.client.close()
                }
            }
        }

        return result.get()
    }

    private fun createLogDirectory(task: TaskStartT) {
        val logPath = File(task.getWorkDir() + "/logs")
        if (logPath.exists()) {
            return
        }
        try {
            logPath.mkdirs()
        } catch (e: Exception) {
            logger.warn("Unable to make log directory '{}'", task.getWorkDir(), e.message)
            return
        }

    }

    private fun handleZpsReaction(zpsTask: ZpsTask, sharedData: SharedData, reaction: Reaction) {
        val process = processMap[zpsTask.id.toString()]
        process?.let {
            val client = process.client

            if (process.killed) {
                return
            }

            /**
             * TODO: queue up a bunch here.
             */
            if (reaction.error != null) {
                client.reportTaskErrors(zpsTask.id.toString(), ImmutableList.of(
                        newTaskError(reaction.error)))
            }

            if (reaction.expand != null) {
                val script = reaction.expand

                val expand = ExpandT()
                expand.setScript(Json.serialize(script))
                expand.setName(script.name)
                if (!isUnitTest) {
                    client.expand(process.id.toString(), expand)
                } else {
                    logger.info("Reacted with expand: {}", reaction.expand)
                }
            }

            if (reaction.stats != null) {
                val stats = reaction.stats
                if (!isUnitTest) {
                    client.reportTaskStats(process.id, TaskStatsT()
                            .setErrorCount(stats.errorCount)
                            .setSuccessCount(stats.successCount)
                            .setWarningCount(stats.warningCount))
                } else {
                    logger.info("Reacted with stats: {}", reaction.stats)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun saveTempZpsScript(task: TaskStartT): File? {
        if (task.getScriptPath() != null) {
            return null
        }

        val temp = File.createTempFile(UUID.randomUUID().toString(), ".json")
        FileOutputStream(temp).use { fos ->
            fos.write(task.getScript())
            fos.close()
            task.setScriptPath(temp.toString())
        }
        return temp
    }

    private fun passesPreflightChecks(proc: ClusterProcess): Boolean {
        if (proc.killed) {
            logger.warn("Task {} did not pass pre-flight check, was killed", proc.id)
            return false
        }

        if (proc.exitStatus != -1) {
            logger.warn("Task {} did not pass pre-flight check, had exit status", proc.id)
        }

        return true
    }

    @Synchronized
    fun syncHostList() {
        if (System.currentTimeMillis() - hostListLoadedTime > 5000) {
            val hosts = properties.getList("analyst.master.host")
            hosts.shuffle()
            hostList = ImmutableList.copyOf(hosts)
            hostListLoadedTime = System.currentTimeMillis()
        }
    }

    /**
     * Convert the ZpsError into a TaskErrorT that can be
     * sent to the archivist.
     *
     * @param zpsError
     * @return
     */
    private fun newTaskError(zpsError: ZpsError): TaskErrorT {
        val error = TaskErrorT()
        error.setMessage(zpsError.message)
        error.setPhase(zpsError.phase)
        error.setProcessor(zpsError.processor)
        error.isSkipped = zpsError.isSkipped
        error.setTimestamp(System.currentTimeMillis())

        error.setStack(ImmutableList.of(StackElementT()
                .setClassName(zpsError.className)
                .setFile(zpsError.file)
                .setLineNumber(zpsError.lineNumber)
                .setMethod(zpsError.method)))

        return error
    }

    private class Connection(var client: MasterServerClient) {
        var backoffTill: Long = 0
        fun backoff() {
            client.close()
            backoffTill = System.currentTimeMillis() + 60 * 1000
        }
    }

    @Throws(Exception::class)
    override fun runOneIteration() {
        if (isUnitTest || analyzeExecutor.isShutdown) {
            return
        }

        val taskCount = processMap.size
        val totalThreads = properties.getInt("analyst.executor.threads")
        val idleThreads = totalThreads - taskCount

        if (idleThreads < 1) {
            autoShutdownMarker = System.currentTimeMillis()
            return
        }

        try {
            syncHostList()
            for (url in hostList!!) {

                val addr = MasterServerClient.convertUriToClusterAddr(url)
                val conn = connectioncCache.get(addr)

                if (conn.backoffTill > System.currentTimeMillis()) {
                    autoShutdownMarker = System.currentTimeMillis()
                    continue
                }

                try {
                    val tasks = conn.client.queuePendingTasks(
                            networkEnvironment.clusterAddr, idleThreads)

                    if (!tasks.isEmpty()) {
                        logger.info("Obtained {} tasks from {}", tasks.size, addr)
                        autoShutdownMarker = System.currentTimeMillis()
                        for (task in tasks) {
                            logger.info("Queuing task to execute: {}. (queue size: {})", task.getId(), processMap.size)
                            queueClusterTask(task)
                        }
                    } else if (autoShutdownIdleMinutes > 0 && taskCount == 0 &&
                            System.currentTimeMillis() - autoShutdownIdleMinutes * 60000 > autoShutdownMarker) {
                        logger.warn("No tasks to process for {} minutes, shutting down", autoShutdownIdleMinutes)
                        initiateShutdown()
                    }
                } catch (e: ClusterConnectionException) {
                    conn.backoff()
                } catch (e: Exception) {
                    conn.backoff()
                    logger.warn("Failed to contact {} for scheduling op, unexpected {}",
                            addr, e.message, e)
                }

            }
        } catch (e: Exception) {
            logger.warn("Unable to determine Archivist host list, {}", e.message)
        }

    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        val pollTime = properties.getInt("analyst.executor.pollTimeMs").toLong()
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(
                5000, pollTime, TimeUnit.MILLISECONDS)
    }

    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        if (executeEnabled) {
            startAsync()
        }
    }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down process manager")
        stopAsync()
    }

    fun initiateShutdown() {
        System.exit(SpringApplication.exit(appContext, ExitCodeGenerator { 0 }))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessManagerServiceImpl::class.java)
    }
}
