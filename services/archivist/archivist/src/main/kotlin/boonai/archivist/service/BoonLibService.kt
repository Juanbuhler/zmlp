package boonai.archivist.service

import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibUpdateSpec
import boonai.archivist.domain.BoonLibImportResponse
import boonai.archivist.domain.Dataset
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.domain.BoonLibState
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.Job
import boonai.archivist.domain.DatasetType
import boonai.archivist.repository.BoonLibDao
import boonai.archivist.repository.BoonLibJdbcDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.getZmlpActor
import boonai.archivist.storage.BoonLibStorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface BoonLibService {

    fun createBoonLib(spec: BoonLibSpec): BoonLib
    fun updateBoonLib(boonlib: BoonLib, spec: BoonLibUpdateSpec)
    fun getBoonLib(id: UUID): BoonLib
    fun importBoonLib(boonlib: BoonLib): BoonLibImportResponse
    fun importBoonLibInto(boonlib: BoonLib, dataset: Dataset): BoonLibImportResponse
    fun findOneBoonLib(boonLibFilter: BoonLibFilter): BoonLib
    fun findAll(boonLibFilter: BoonLibFilter): KPagedList<BoonLib>
}

@Service
class BoonLibServiceImpl(
    val boonLibDao: BoonLibDao,
    val boonLibJdbcDao: BoonLibJdbcDao,
    val datasetService: DatasetService,
    val jobLaunchService: JobLaunchService,
    val boonLibStorageService: BoonLibStorageService
) : BoonLibService {

    @Transactional
    override fun createBoonLib(spec: BoonLibSpec): BoonLib {

        if (!spec.entityType.isCompatible(spec.entity)) {
            throw IllegalArgumentException("The entity is not compatible with the entity type.")
        }

        val time = System.currentTimeMillis()
        val id = UUIDGen.uuid1.generate()
        val actor = getZmlpActor().toString()

        val item = BoonLib(
            id,
            spec.name,
            spec.entity,
            spec.entityType,
            spec.description,
            spec.license,
            BoonLibState.EMPTY,
            time,
            time,
            actor,
            actor
        )

        boonLibDao.save(item)

        spec?.entityId?.let {
            when (spec.entity) {
                BoonLibEntity.Dataset -> {
                    val ds = datasetService.getDataset(it)
                    launchExportJob(ds, item)
                }
            }
        }

        return item
    }

    @Transactional
    override fun updateBoonLib(boonlib: BoonLib, spec: BoonLibUpdateSpec) {

        spec.name?.let {
            boonlib.name = it
        }

        spec.description?.let {
            boonlib.description = it
        }

        spec.license?.let {
            boonlib.license = it
        }

        spec.state?.let {
            boonlib.state = it
        }

        boonlib.timeModified = System.currentTimeMillis()
        boonlib.actorModified = getZmlpActor().toString()
    }

    @Transactional(readOnly = true)
    override fun getBoonLib(id: UUID): BoonLib {
        return boonLibDao.getOne(id)
    }

    fun launchExportJob(dataset: Dataset, boonlib: BoonLib): Job {
        return jobLaunchService.launchBoonLibDatasetExport(dataset, boonlib)
    }

    override fun importBoonLib(boonlib: BoonLib): BoonLibImportResponse {
        when (boonlib.entity) {
            BoonLibEntity.Dataset -> {
                val dspec = DatasetSpec(
                    "BoonLib ${boonlib.name}",
                    DatasetType.Classification,
                    boonlib.description
                )
                val ds = datasetService.createDataset(dspec)
                return importBoonLibInto(boonlib, ds)
            }
        }
    }

    override fun importBoonLibInto(boonlib: BoonLib, dataset: Dataset): BoonLibImportResponse {
        return boonLibStorageService.importAssetsInto(boonlib, dataset)
    }

    @Transactional(readOnly = true)
    override fun findOneBoonLib(boonLibFilter: BoonLibFilter): BoonLib {
        return boonLibJdbcDao.findOneBoonLib(boonLibFilter)
    }

    @Transactional(readOnly = true)
    override fun findAll(boonLibFilter: BoonLibFilter): KPagedList<BoonLib> {
        return boonLibJdbcDao.findAll(boonLibFilter)
    }
}
