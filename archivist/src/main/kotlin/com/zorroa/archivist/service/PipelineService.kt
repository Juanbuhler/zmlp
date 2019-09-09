package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Splitter
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.repository.PipelineDao
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

interface PipelineService {
    fun resolve(name: String): List<ProcessorRef>
    fun resolve(id: UUID): List<ProcessorRef>
    fun resolveDefault(type: PipelineType): List<ProcessorRef>
    fun create(spec: PipelineSpec): Pipeline
    fun get(id: UUID): Pipeline
    fun get(name: String): Pipeline
    fun getAll(): List<Pipeline>
    fun getAll(type: PipelineType): List<Pipeline>
    fun getAll(page: Pager): PagedList<Pipeline>
    fun update(pipeline: Pipeline): Boolean
    fun delete(id: UUID): Boolean
    fun getDefaultPipelineName(type: PipelineType): String
    fun resolve(type: PipelineType, refs: List<ProcessorRef>?): List<ProcessorRef>

    /**
     * Load a pipeline fragment.  A fragment is either a comma delimited list
     * of processors of a path to a json file.  A path must end with .json.
     */
    fun loadFragment(fragment: String): List<ProcessorRef>
}

/**
 * The PipelineService handles the management of pipelines.
 */
@Service
@Transactional
class PipelineServiceImpl @Autowired constructor(
    private val pipelineDao: PipelineDao,
    private val properties: ApplicationProperties
) : PipelineService, ApplicationListener<ContextRefreshedEvent> {

    override fun create(spec: PipelineSpec): Pipeline {
        val p = pipelineDao.create(spec)
        return p
    }

    override fun update(pipeline: Pipeline): Boolean {
        return pipelineDao.update(pipeline)
    }

    override fun get(id: UUID): Pipeline {
        return pipelineDao.get(id)
    }

    override fun get(name: String): Pipeline {
        return pipelineDao.get(name)
    }

    override fun getAll(): List<Pipeline> {
        return pipelineDao.getAll()
    }

    override fun getAll(type: PipelineType): List<Pipeline> {
        return pipelineDao.getAll(type)
    }

    override fun getAll(page: Pager): PagedList<Pipeline> {
        return pipelineDao.getAll(page)
    }

    override fun delete(id: UUID): Boolean {
        return pipelineDao.delete(id)
    }

    override fun getDefaultPipelineName(type: PipelineType): String {
        val names = when (type) {
            PipelineType.Import -> properties.getString("archivist.pipeline.default-import-pipeline")
            PipelineType.Export -> properties.getString("archivist.pipeline.default-export-pipeline")
            else -> throw IllegalArgumentException("There are no default $type pipelines")
        }
        return names?.trim()
    }

    override fun resolveDefault(type: PipelineType): List<ProcessorRef> {
        val name = getDefaultPipelineName(type)
        return resolve(type, pipelineDao.get(name).processors)
    }

    override fun resolve(name: String): List<ProcessorRef> {
        // val processors = mutableListOf<ProcessorRef>()
        val pipeline = pipelineDao.get(name)
        return pipeline.processors
    }

    override fun resolve(id: UUID): List<ProcessorRef> {
        val pipeline = pipelineDao.get(id)
        return resolve(pipeline.type, pipeline.processors)
    }

    override fun resolve(type: PipelineType, refs: List<ProcessorRef>?): MutableList<ProcessorRef> {
        val result = mutableListOf<ProcessorRef>()

        refs?.forEach { ref ->
            if (ref.className.startsWith("pipeline:", ignoreCase = true)) {
                val name = ref.className.split(":", limit = 2)[1]

                val pl = try {
                    pipelineDao.get(UUID.fromString(name))
                } catch (e: IllegalArgumentException) {
                    pipelineDao.get(name)
                }

                if (pl.type != type) {
                    throw throw IllegalArgumentException(
                        "Cannot have pipeline type " +
                            pl.type + " embedded in a " + type + " pipeline"
                    )
                }
                result.addAll(resolve(type, pl.processors))
            } else {
                result.add(ref)
                ref.execute?.let {
                    ref.execute = resolve(type, it)
                }
            }
        }

        return result
    }

    override fun loadFragment(fragment: String): List<ProcessorRef> {
        return if (fragment.endsWith(".json")) {
            Json.Mapper.readValue(File(fragment))
        } else {
            Splitter.on(",").omitEmptyStrings().trimResults().split(fragment).map {
                ProcessorRef(it)
            }
        }
    }

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        val searchPath = properties.getList("archivist.pipeline.search-path")
        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)

        searchPath.forEach {

            if (it.startsWith("classpath:")) {
                val resources = resolver.getResources("$it/*.json")
                for (resource in resources) {
                    loadPipeline(resource.inputStream, "classpath")
                }
            } else {
                val path = Paths.get(it.trim())
                if (Files.exists(path)) {
                    for (file in Files.list(path)) {
                        loadPipeline(FileInputStream(file.toFile()), "external")
                    }
                }
            }
        }
    }

    private fun loadPipeline(stream: InputStream, source: String) {
        val spec = Json.Mapper.readValue<PipelineSpec>(stream)
        try {
            val pipe = pipelineDao.get(spec.name)
            logger.info("Updating ${spec.type} pipeline '${spec.name}' from $source")
            pipelineDao.update(pipe)
        } catch (e: EmptyResultDataAccessException) {
            logger.info("Creating ${spec.type} pipeline '${spec.name}' from $source")
            pipelineDao.create(spec)
        } catch (e: Exception) {
            logger.warn("Failed to load pipeline file:", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
