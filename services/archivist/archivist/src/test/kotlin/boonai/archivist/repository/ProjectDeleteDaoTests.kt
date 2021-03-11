package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.PipelineMode
import boonai.archivist.domain.emptyZpsScript
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.Provider
import boonai.archivist.domain.Category
import boonai.archivist.domain.ModelObjective
import boonai.archivist.domain.FileType
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.AutomlSessionSpec
import boonai.archivist.domain.DataSourceSpec
import boonai.archivist.security.getProjectId
import boonai.archivist.service.AutomlService
import boonai.archivist.service.CredentialsService
import boonai.archivist.service.JobService
import boonai.archivist.service.PipelineModService
import boonai.archivist.service.ModelService
import boonai.archivist.service.DataSourceService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ProjectDeleteDaoTests : AbstractTest() {

    @Autowired
    lateinit var projectDeleteDao: ProjectDeleteDao

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var automlService: AutomlService

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Test
    fun testDeleteProjectRelatedObjects() {
        createJobAndTasks()
        createPipelineAndModule()
        createAutoMl()
        createDataSource()

        val indexRoute = indexRoutingService.findOne(IndexRouteFilter(projectIds = listOf(getProjectId())))
        indexRoutingService.closeAndDeleteIndex(indexRoute)

        projectDeleteDao.deleteProjectRelatedObjects(getProjectId())

        val listOfTables = listOf(
            "project_quota",
            "project_quota_time_series",
            "processor",
            "module",
            "credentials",
            "pipeline",
            "automl",
            "model",
            "job",
            "datasource",
            "project"

        )
        listOfTables.forEach {
            assertEquals(
                0,
                jdbc.queryForObject("SELECT COUNT(*) FROM $it where pk_project=?", Int::class.java, getProjectId())
            )
        }
    }

    private fun createJobAndTasks() {

        val tspec = listOf(
            emptyZpsScript("foo"),
            emptyZpsScript("bar")
        )
        tspec[0].children = listOf(emptyZpsScript("foo1"))
        tspec[1].children = listOf(emptyZpsScript("bar"))

        val spec2 = JobSpec(
            null,
            tspec,
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(spec2)
    }

    private fun createPipelineAndModule() {
        val modularSpec = PipelineSpec(
            "mod-test",
            mode = PipelineMode.MODULAR,
            processors = listOf(
                ProcessorRef("com.zorroa.IngestImages", "image-foo"),
                ProcessorRef("com.zorroa.IngestVideo", "image-foo")
            )
        )

        val modSpec = PipelineModSpec(
            "test0", "test",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Documents),
            listOf(),
            true
        )
        pipelineModService.create(modSpec)
        modularSpec.modules = listOf(modSpec.name)
        pipelineService.create(modularSpec)
    }

    private fun createAutoMl() {
        val modelSpec = ModelSpec("animals", ModelType.GCP_AUTOML_CLASSIFIER)
        val model = modelService.createModel(modelSpec)

        val automlSpec = AutomlSessionSpec(
            "project/foo/region/us-central/datasets/foo",
            "/foo/bar"
        )

        automlService.createSession(model, automlSpec)
    }

    private fun createDataSource() {
        val spec = DataSourceSpec(
            "dev-data",
            "gs://zorroa-dev-data",
            fileTypes = FileType.allTypes()
        )
        dataSourceService.create(spec)
    }
}