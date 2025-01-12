package boonai.archivist.service

import boonai.archivist.domain.ArgSchema
import boonai.archivist.domain.Asset
import boonai.archivist.domain.Category
import boonai.archivist.domain.FileType
import boonai.archivist.domain.Job
import boonai.archivist.domain.ModOp
import boonai.archivist.domain.ModOpType
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelApplyRequest
import boonai.archivist.domain.ModelApplyResponse
import boonai.archivist.domain.ModelCopyRequest
import boonai.archivist.domain.ModelFilter
import boonai.archivist.domain.ModelPatchRequestV2
import boonai.archivist.domain.ModelPublishRequest
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelState
import boonai.archivist.domain.ModelTrainingRequest
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.ModelUpdateRequest
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineModUpdate
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.ProjectDirLocator
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.ReprocessAssetSearchRequest
import boonai.archivist.domain.StandardContainers
import boonai.archivist.repository.DatasetJdbcDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.ModelDao
import boonai.archivist.repository.ModelJdbcDao
import boonai.archivist.repository.PipelineModDao
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.archivist.security.getZmlpActorOrNull
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.FileUtils
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.util.Json
import com.google.pubsub.v1.PubsubMessage
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.UUID

interface ModelService {
    fun createModel(spec: ModelSpec): Model
    fun trainModel(model: Model, request: ModelTrainingRequest): Job
    fun getModel(id: UUID): Model
    fun find(filter: ModelFilter): KPagedList<Model>
    fun findOne(filter: ModelFilter): Model
    fun publishModel(model: Model, req: ModelPublishRequest): PipelineMod
    fun applyModel(model: Model, req: ModelApplyRequest): ModelApplyResponse
    fun testModel(model: Model, req: ModelApplyRequest): ModelApplyResponse
    fun deleteModel(model: Model)
    fun setTrainingArgs(model: Model, args: Map<String, Any>)
    fun patchTrainingArgs(model: Model, patch: Map<String, Any>)
    fun getTrainingArgSchema(type: ModelType): ArgSchema
    fun getModelVersions(model: Model): Set<String>
    fun copyModelTag(model: Model, req: ModelCopyRequest)
    fun updateModel(id: UUID, update: ModelUpdateRequest): Model
    fun patchModel(id: UUID, update: ModelPatchRequestV2): Model
    fun postToModelEventTopic(msg: PubsubMessage)
    fun checkModelPublishArgs(model: Model, req: ModelPublishRequest): Boolean
}

@Service
@Transactional
class ModelServiceImpl(
    val modelDao: ModelDao,
    val modelJdbcDao: ModelJdbcDao,
    val jobLaunchService: JobLaunchService,
    val jobService: JobService,
    val pipelineModService: PipelineModService,
    val pipelineModDao: PipelineModDao,
    val indexRoutingService: IndexRoutingService,
    val assetSearchService: AssetSearchService,
    val fileStorageService: ProjectStorageService,
    val argValidationService: ArgValidationService,
    val datasetService: DatasetService,
    val environment: Environment,
    val publisherService: PublisherService,
    val datasetJdbcDao: DatasetJdbcDao
) : ModelService {

    val topic = "model-events"

    override fun createModel(spec: ModelSpec): Model {
        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor()

        if (!spec.type.enabled) {
            throw IllegalArgumentException(
                "This model type is not currently enabled."
            )
        }

        val locator = ProjectFileLocator(
            ProjectStorageEntity.MODELS, id.toString(), "__TAG__", spec.type.fileName
        )

        validateModelName(spec.name)
        spec.datasetId?.let {
            validateDatasetType(it, spec.type)
            datasetJdbcDao.incrementModelCount(it)
        }

        argValidationService.validateArgsUnknownOnly("training/${spec.type.name}", spec.trainingArgs)

        val state = if (spec.type.uploadable) {
            ModelState.RequiresUpload
        } else {
            ModelState.RequiresTraining
        }

        val model = Model(
            id,
            getProjectId(),
            spec.datasetId,
            state,
            spec.type,
            spec.name,
            spec.name,
            null,
            locator.getFileId(),
            "Training model: ${spec.name} - [${spec.type.objective}]",
            false,
            spec.applySearch, // VALIDATE THIS PARSES.
            spec.trainingArgs,
            (spec.dependencies ?: emptyList()).minus(spec.name),
            time,
            time,
            actor.toString(),
            actor.toString(),
            null, null, null, null, null, null, null, null, null, null
        )

        logger.event(
            LogObject.MODEL, LogAction.CREATE,
            mapOf(
                "modelId" to id,
                "modelType" to spec.type.name
            )
        )

        return modelDao.save(model)
    }

    override fun updateModel(id: UUID, update: ModelUpdateRequest): Model {
        val model = getModel(id)
        update.datasetId?.let {
            validateDatasetType(it, model.type)
        }

        if (update.name != model.name) {
            validateModelName(update.name)
        }
        updateDatasetsModelCount(model.datasetId, update.datasetId)

        model.name = update.name
        model.datasetId = update.datasetId
        model.dependencies = update.dependencies
        model.timeModified = System.currentTimeMillis()
        model.actorModified = getZmlpActor().toString()
        logger.event(
            LogObject.MODEL, LogAction.UPDATE,
            mapOf(
                "modelId" to id,
                "modelType" to model.type.name
            )
        )
        return model
    }

    override fun patchModel(id: UUID, update: ModelPatchRequestV2): Model {
        val model = getModel(id)

        if (update.isFieldSet("datasetId")) {
            update.datasetId?.let {
                validateDatasetType(it, model.type)
            }
            updateDatasetsModelCount(model.datasetId, update.datasetId)
            model.datasetId = update.datasetId
        }

        if (update.isFieldSet("name")) {
            update.name?.let {
                validateModelName(it)
                model.name = it
            }
        }

        if (update.isFieldSet("dependencies")) {
            update.dependencies?.let { model.dependencies = it }
        }

        model.timeModified = System.currentTimeMillis()
        model.actorModified = getZmlpActor().toString()

        logger.event(
            LogObject.MODEL, LogAction.UPDATE,
            mapOf(
                "modelId" to id,
                "modelType" to model.type.name
            )
        )

        return model
    }

    @Transactional(readOnly = true)
    override fun getModel(id: UUID): Model {
        return modelDao.getOneByProjectIdAndId(getProjectId(), id)
            ?: throw EmptyResultDataAccessException("The model $id does not exist", 1)
    }

    @Transactional(readOnly = true)
    override fun find(filter: ModelFilter): KPagedList<Model> {
        return modelJdbcDao.find(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: ModelFilter): Model {
        return modelJdbcDao.findOne(filter)
    }

    override fun trainModel(model: Model, request: ModelTrainingRequest): Job {

        if (!model.type.trainable) {
            throw IllegalStateException("This model type cannot be trained")
        }

        if (model.datasetId == null) {
            throw IllegalStateException("The model must have an assigned Dataset to be trained.")
        }

        val trainArgs = argValidationService.buildArgs(
            getTrainingArgSchema(model.type), model.trainingArgs + (request.trainArgs ?: emptyMap())
        ).plus(
            mapOf<String, Any?>(
                "model_id" to model.id.toString(),
                "post_action" to (request.postAction.name),
                "tag" to "latest",
                "training_bucket" to "gs://${environment.getProperty("GCLOUD_PROJECT")}-training"
            )
        )

        logger.info("Training model ID ${model.id} $trainArgs")
        logger.info("Launching train job ${model.type.trainProcessor} ${request.postAction}")

        model.actorLastTrained = getZmlpActor().toString()
        model.timeLastTrained = System.currentTimeMillis()

        val processor = ProcessorRef(
            model.type.trainProcessor, "boonai/plugins-train", trainArgs
        )

        return jobLaunchService.launchTrainingJob(
            model.trainingJobName, processor, mapOf()
        )
    }

    override fun applyModel(model: Model, req: ModelApplyRequest): ModelApplyResponse {
        val name = "Applying model: ${model.name}"
        var search = req.search ?: model.applySearch

        val analyzeTrainingSet = req.analyzeTrainingSet ?: model.type.deployOnTrainingSet

        if (!analyzeTrainingSet && model.datasetId != null) {
            search = datasetService.wrapSearchToExcludeTrainingSet(model, search)
        }

        val count = assetSearchService.count(search)
        if (count == 0L) {
            return ModelApplyResponse(0, null)
        }

        // Use global settings to override the model tag.
        val repro = ReprocessAssetSearchRequest(
            search,
            listOf(model.getModuleName()),
            name = name,
            replace = true,
            includeStandard = false,
            settings = mapOf("${model.id}:tag" to req.tag)
        )

        val jobId = getZmlpActor().getAttr("jobId")

        model.actorLastApplied = getZmlpActor().toString()
        model.timeLastApplied = System.currentTimeMillis()

        return if (jobId == null) {
            val rsp = jobLaunchService.launchJob(repro)
            ModelApplyResponse(count, rsp.job)
        } else {
            val job = jobService.get(UUID.fromString(jobId), forClient = false)
            if (job.projectId != getProjectId()) {
                throw IllegalArgumentException("Unknown job Id ${job.id}")
            }
            val script = jobLaunchService.getReprocessTask(repro)
            jobService.createTask(job, script)
            ModelApplyResponse(count, job)
        }
    }

    override fun testModel(model: Model, req: ModelApplyRequest): ModelApplyResponse {
        val name = "Testing model: ${model.name}"
        var search = datasetService.buildTestLabelSearch(model)

        val count = assetSearchService.count(search)
        if (count == 0L) {
            return ModelApplyResponse(0, null)
        }

        // Use global settings to override the model tag.
        val repro = ReprocessAssetSearchRequest(
            datasetService.buildTestLabelSearch(model),
            listOf(model.getModuleName()),
            name = name,
            replace = true,
            includeStandard = false,
            settings = mapOf("${model.id}:tag" to req.tag, "maxPredictions" to 1)
        )

        val jobId = getZmlpActor().getAttr("jobId")

        model.actorLastTested = getZmlpActor().toString()
        model.timeLastTested = System.currentTimeMillis()

        return if (jobId == null) {
            val rsp = jobLaunchService.launchJob(repro)
            ModelApplyResponse(count, rsp.job)
        } else {
            val job = jobService.get(UUID.fromString(jobId), forClient = false)
            if (job.projectId != getProjectId()) {
                throw IllegalArgumentException("Unknown job Id ${job.id}")
            }
            val script = jobLaunchService.getReprocessTask(repro)
            jobService.createTask(job, script)
            ModelApplyResponse(count, job)
        }
    }

    override fun publishModel(model: Model, req: ModelPublishRequest): PipelineMod {

        val mod = pipelineModService.findByName(model.moduleName, false)
        val version = versionUp(model)
        val ops = buildModuleOps(model, req, version)

        logger.event(
            LogObject.MODEL, LogAction.PUBLISH,
            mapOf("modelId" to model.id, "modelName" to model.name)
        )

        if (model.isUploadable()) {
            modelJdbcDao.updateState(model.id, ModelState.Deployed)
        } else {
            modelJdbcDao.updateState(model.id, ModelState.Trained)
        }

        if (mod != null) {
            // Set version number to change checksum
            val update = PipelineModUpdate(
                mod.name, mod.description, model.type.provider,
                mod.category, mod.type,
                listOf(FileType.Documents, FileType.Images, FileType.Videos),
                ops
            )
            pipelineModService.update(mod.id, update)
            return pipelineModService.get(mod.id)
        } else {
            val modspec = PipelineModSpec(
                model.moduleName,
                model.type.description,
                model.type.provider,
                Category.TRAINED,
                model.type.objective,
                listOf(FileType.Documents, FileType.Images, FileType.Videos),
                ops
            )
            return pipelineModService.create(modspec)
        }
    }

    override fun checkModelPublishArgs(model: Model, req: ModelPublishRequest): Boolean {
        val keys = req.args.keys
        for (arg in model.type.requiredArgs) {
            if (arg !in keys) {
                logger.warn("The model ${model.name} pub request is missing the required arg: '$arg'")
                return false
            }
        }
        return true
    }

    override fun copyModelTag(model: Model, req: ModelCopyRequest) {
        val modelStorage = ProjectDirLocator(ProjectStorageEntity.MODELS, model.id.toString()).getPath()
        var srcTagPath = "$modelStorage/${req.srcTag}"
        var dstTagPath = "$modelStorage/${req.dstTag}"

        for (file in fileStorageService.listFiles(srcTagPath)) {
            val dstFile = "$dstTagPath/${FileUtils.filename(file)}"
            fileStorageService.copy(file, dstFile)
        }
    }

    override fun setTrainingArgs(model: Model, args: Map<String, Any>) {
        argValidationService.validateArgs("training/${model.type.name}", args)
        model.trainingArgs = args
    }

    override fun patchTrainingArgs(model: Model, patch: Map<String, Any>) {
        argValidationService.validateArgs("training/${model.type.name}", patch)
        val args = Asset(model.trainingArgs.toMutableMap())
        for ((k, v) in patch) {
            args.setAttr(k, v)
        }
        model.trainingArgs = args.document
    }

    override fun getTrainingArgSchema(type: ModelType): ArgSchema {
        return argValidationService.getArgSchema("training/${type.name}")
    }

    override fun deleteModel(model: Model) {
        logger.event(
            LogObject.MODEL, LogAction.DELETE,
            mapOf("modelId" to model.id, "modelName" to model.name)
        )

        modelJdbcDao.delete(model)
        model.datasetId?.let {
            datasetJdbcDao.decrementModelCount(it)
        }

        pipelineModService.findByName(model.moduleName, false)?.let {
            pipelineModService.delete(it.id)
        }

        val msg = PubsubMessage.newBuilder()
            .putAttributes("type", "model-delete")
            .putAttributes("modelId", model.id.toString())
            .putAttributes("deployed", model.type.uploadable.toString())
            .putAttributes("projectId", model.projectId.toString())
            .putAttributes("image", model.imageName())
            .build()

        postToModelEventTopic(msg)

        try {
            fileStorageService.recursiveDelete(
                ProjectDirLocator(ProjectStorageEntity.MODELS, model.id.toString())
            )
        } catch (e: Exception) {
            logger.error("Failed to delete files associated with model: ${model.id}", e)
        }
    }

    fun buildModuleOps(model: Model, req: ModelPublishRequest, version: String): List<ModOp> {
        val ops = mutableListOf<ModOp>()

        // I don't know what this does but might be duplicate
        // of what the pipeline resolver is doing.
        /*
        for (depend in model.type.dependencies) {
            val mod = pipelineModService.findByName(depend, true)
            ops.addAll(mod?.ops ?: emptyList())
        }
         */

        val user = getZmlpActorOrNull() ?: "Unknown"
        logger.info("$user is building module ops  for ' ${model.name} / ${model.id}")
        logger.info("Processor: ${model.name} / ${model.id}")
        logger.info("Args: ${Json.prettyString(req.args)}")

        // Add the dependency before.
        if (model.type.dependencies.isNotEmpty()) {
            ops.add(
                ModOp(
                    ModOpType.DEPEND,
                    model.type.dependencies
                )
            )
        }

        val validModules = pipelineModDao.findByNameIn(model.dependencies)
        if (validModules.isNotEmpty()) {
            ops.add(
                ModOp(
                    ModOpType.DEPEND,
                    validModules.map { it.name }
                )
            )
        }

        ops.add(
            ModOp(
                ModOpType.APPEND,
                listOf(
                    ProcessorRef(
                        model.type.classifyProcessor,
                        StandardContainers.ANALYSIS,
                        mutableMapOf(
                            "model_id" to model.id.toString(),
                            "version" to version
                        ).plus(req.args),
                        module = model.name
                    )
                )
            )
        )

        return ops
    }

    override fun getModelVersions(model: Model): Set<String> {
        val files = fileStorageService.listFiles(
            ProjectDirLocator(ProjectStorageEntity.MODELS, model.id.toString()).getPath()
        )
        return files.map { it.split("/")[4] }.toSet()
    }

    override fun postToModelEventTopic(msg: PubsubMessage) {
        publisherService.publish(topic, msg)
    }

    fun versionUp(model: Model): String {
        val version = "${System.currentTimeMillis()}"
        val versionBytes = version.plus("\n").toByteArray()
        val versionFile = ProjectStorageSpec(
            model.getModelVersionStorageLocator("latest"), mapOf(),
            ByteArrayInputStream(versionBytes), versionBytes.size.toLong()
        )
        fileStorageService.store(versionFile)
        return version
    }

    fun validateDatasetType(dsId: UUID, mtype: ModelType) {
        val ds = datasetService.getDataset(dsId)
        if (ds.type != mtype.datasetType) {
            throw IllegalArgumentException("Invalid Dataset type for this model type")
        }
    }

    fun validateModelName(name: String) {
        if (!name.matches(modelNameRegex)) {
            throw IllegalArgumentException(
                "Model names must be alpha-numeric lowercase, dashes allowed."
            )
        }

        reservedPrefixes.forEach {
            if (name.startsWith(it)) {
                throw IllegalArgumentException(
                    "Model names cannot start with a reserved prefix."
                )
            }
        }
    }

    private fun updateDatasetsModelCount(oldDatasetId: UUID?, newDatasetId: UUID?) {
        if (oldDatasetId != newDatasetId) {
            oldDatasetId?.let { datasetJdbcDao.decrementModelCount(it) }
            newDatasetId?.let { datasetJdbcDao.incrementModelCount(it) }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ModelServiceImpl::class.java)

        private val modelNameRegex = Regex("^[a-z0-9_\\-\\s]{2,}$", RegexOption.IGNORE_CASE)

        private val reservedPrefixes = listOf("boonai-", "gcp-", "aws-", "clarifai-")
    }
}
