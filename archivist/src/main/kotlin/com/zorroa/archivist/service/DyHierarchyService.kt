package com.zorroa.archivist.service

import com.google.common.base.Splitter
import com.google.common.collect.*
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.DyHierarchyDao
import com.zorroa.archivist.search.AssetScript
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.domain.ArchivistWriteException
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface DyHierarchyService {

    fun update(id: UUID, spec: DyHierarchy): Boolean

    fun delete(dyhi: DyHierarchy): Boolean

    /**
     * Create a dynamic hierarchy generator.
     *
     * @param spec
     */
    fun create(spec: DyHierarchySpec): DyHierarchy

    fun get(id: UUID): DyHierarchy

    fun get(folder: Folder): DyHierarchy

    /**
     * Generate folders for all DyHis for the current user's organization.
     */
    fun generateAll()

    /**
     * Generate folders for the given DyHi
     *
     * @param dyhi The DyHierarchy to generate folders for.
     * @param clearFirst Set to true if any of the dyhi levels have changed.
     */
    fun generate(dyhi: DyHierarchy, clearFirst: Boolean=false) : Int

}

@Service
class DyHierarchyServiceImpl @Autowired constructor (
    val dyHierarchyDao: DyHierarchyDao,
    val indexRoutingService: IndexRoutingService,
    val transactionEventManager: TransactionEventManager,
    val clusterLockExecutor: ClusterLockExecutor,
    val clusterLockService: ClusterLockService
) : DyHierarchyService {

    @Autowired
    private lateinit var folderService: FolderService;

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var fieldService: FieldService

    @Value("\${archivist.dyhi.maxFoldersPerLevel}")
    private val maxFoldersPerLevel: Int? = null

    @Transactional
    override fun update(id: UUID, spec: DyHierarchy): Boolean {
        val current = dyHierarchyDao.get(id)

        if (!dyHierarchyDao.update(id, spec)) {
            return false
        }

        val updated = dyHierarchyDao.get(id)

        /*
         * Folder ID change
         */
        val folderOld = folderService.get(current.folderId)
        val folderNew = folderService.get(updated.folderId)
        if (folderOld !== folderNew) {
            folderService.removeDyHierarchyRoot(folderOld)
        }

        /*
         * Even if the folder didn't change, we reset so the smart query
         * gets updated.
         */
        val field = updated.getLevel(0).field
        folderService.setDyHierarchyRoot(folderNew, field)

        /*
         * Queue up an event where after this transaction commits.
         */
        transactionEventManager.afterCommit(false) {
            generate(dyHierarchyDao.get(current.id), clearFirst = true)
        }

        return true
    }

    @Transactional
    override fun delete(dyhi: DyHierarchy): Boolean {
        val result = clusterLockExecutor.inline(ClusterLockSpec.hardLock(dyhi.lockName)) {
            if (dyHierarchyDao.delete(dyhi.id)) {
                val folder = folderService.get(dyhi.folderId)
                folderService.removeDyHierarchyRoot(folder)
                folderService.deleteAll(dyhi)
                true
            }
            else {

                false
            }
        }
        return result ?: false
    }

    @Transactional
    override fun create(spec: DyHierarchySpec): DyHierarchy {

        val folder = folderService.get(spec.folderId)
        if (folder.attrs?.getOrDefault("launchpad", false) as Boolean) {
            throw ArchivistWriteException("A Launchpad with the same name already exists. Please choose a different name.")
        }
        val dyhi = dyHierarchyDao.create(spec)
        folderService.setDyHierarchyRoot(folder, spec.levels[0].field)

        if (spec.generate) {
            transactionEventManager.afterCommit(false) {
                generate(dyhi, false)
            }
        }

        return dyhi
    }

    override fun get(id: UUID): DyHierarchy {
        return dyHierarchyDao.get(id)
    }

    override fun get(folder: Folder): DyHierarchy {
        return dyHierarchyDao[folder]
    }

    override fun generateAll() {
        dyHierarchyDao.getAll().forEach { generate(it) }
    }

    override fun generate(dyhi: DyHierarchy, clearFirst: Boolean) : Int {
        val lock = ClusterLockSpec.combineLock(dyhi.lockName).apply { timeout = 5 }
        val result = clusterLockExecutor.inline(lock) {
            try {
                generateInternal(dyhi, clearFirst)
            } catch (e: Exception) {
                logger.warn("Failed to generate dyhi ${dyhi.id}", e)
                null
            }
        }
        return result ?: -1
    }

    /**
     * Only the run() method should call this function.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    private fun generateInternal(dyhi: DyHierarchy, clearFirst: Boolean): Int {
        if (dyhi.levels.isNullOrEmpty()) {
            return 0
        }

        if (clearFirst) {
            folderService.deleteAll(dyhi)
        }

        val rf = folderService.get(dyhi.folderId, cached = false)
        val rest = indexRoutingService.getEsRestClient()

        try {

            val sr = rest.newSearchBuilder()
            if (rf.search != null) {
                sr.source.query(searchService.getQuery(rf.search))
            }
            else {
                sr.source.query(QueryBuilders.matchAllQuery())
            }
            /**
             * Fix the field name to take into account raw.
             */
            fieldService.invalidateFields()
            dyhi.levels.forEach { resolveFieldName(it) }

            /**
             * Build a nested list of Elastic aggregations.
             */
            var terms: AggregationBuilder? = elasticAggregationBuilder(dyhi.levels[0], 0)
            sr.source.aggregation(terms)
            for (i in 1 until dyhi.levels.size) {
                val sub = elasticAggregationBuilder(dyhi.getLevel(i), i)
                terms!!.subAggregation(sub)
                terms = sub
            }

            /**
             * Breadth first walk of the aggregations which creates the folders as it goes.
             */
            val folders = FolderStack(folderService.get(dyhi.folderId), dyhi)
            val queue = ArrayDeque<Tuple<Aggregations, Int>>()
            queue.add(Tuple(rest.client.search(sr.request).aggregations, 0))
            createDynamicHierarchy(queue, folders)

            /**
             * Delete all unmarked folders.
             */
            val unusedFolders = Sets.difference(ImmutableSet.copyOf(folderService.getAllIds(dyhi)), folders.folderIds)
            try {
                folderService.deleteAll(unusedFolders)
            } catch (e: Exception) {
                logger.warn("Failed to delete unused folders: {}, {}", unusedFolders, e)
            }

            logger.event(LogObject.DYHI, LogAction.EXECUTE,
                    mapOf("dyhiId" to dyhi.id,
                            "dyhiLevels" to dyhi.levels.size,
                            "folderId" to dyhi.folderId,
                            "folderCount" to folders.count))

            return folders.count
        } catch (e: Exception) {
            logger.warn("Failed to generate dynamic hierarchy,", e)
        } finally {

        }

        return 0
    }

    private fun elasticAggregationBuilder(level: DyHierarchyLevel, idx: Int): AggregationBuilder? {
        when (level.type) {
            DyHierarchyLevelType.Attr, null -> {
                val terms = AggregationBuilders.terms(idx.toString())
                terms.field(level.field)
                terms.size(maxFoldersPerLevel!!)
                return terms
            }
            DyHierarchyLevelType.Year -> {
                val year = AggregationBuilders.dateHistogram(idx.toString())
                year.field(level.field)
                year.dateHistogramInterval(DateHistogramInterval("1y"))
                year.format("yyyy")
                year.minDocCount(1)
                return year
            }
            DyHierarchyLevelType.Month -> {
                val month = AggregationBuilders.dateHistogram(idx.toString())
                month.field(level.field)
                month.dateHistogramInterval(DateHistogramInterval("1M"))
                month.format("M")
                month.minDocCount(1)
                return month
            }
            DyHierarchyLevelType.Day -> {
                val day = AggregationBuilders.dateHistogram(idx.toString())
                day.field(level.field)
                day.dateHistogramInterval(DateHistogramInterval("1d"))
                day.format("d")
                day.minDocCount(1)
                return day
            }
            DyHierarchyLevelType.Path -> {
                val pathTerms = AggregationBuilders.terms(idx.toString())
                val field = if (level.field.endsWith(".paths")) {
                    level.field
                }
                else {
                    "${level.field}.paths"
                }
                pathTerms.field(field)
                pathTerms.size(maxFoldersPerLevel!!)
                return pathTerms
            }
        }
    }

    /**
     * Breadth first walk of aggregations which builds the folder structure as it walks.
     *
     * @param queue
     */
    private fun createDynamicHierarchy(queue: Queue<Tuple<Aggregations, Int>>, folders: FolderStack) {

        val item = queue.poll()
        val aggs = item.left
        val depth = item.right
        if (aggs == null) {
            return
        }

        val terms = aggs.get<MultiBucketsAggregation>(depth.toString()) ?: return

        val level = folders.dyhi.getLevel(depth)

        val buckets = terms.buckets
        for (bucket in buckets) {

            var popit = true

            val value: String
            var name: String
            when (level.type) {
                DyHierarchyLevelType.Attr -> {
                    value = bucket.keyAsString
                    name = value.replace('/', '_')
                    folders.push(name, level, value, null)
                }
                DyHierarchyLevelType.Path -> {
                    value = bucket.keyAsString
                    val delimiter = (level.options as MutableMap<String, Any>).getOrDefault("delimiter", "/").toString()

                    val peek = folders.peek()
                    folders.stash()
                    popit = false
                    for (`val` in PathIterator(value, delimiter)) {
                        name = Iterables.getLast(Splitter.on(delimiter).omitEmptyStrings().splitToList(`val`))
                        folders.push(name, level, `val`, peek)
                    }
                }
                else -> {
                    value = bucket.keyAsString
                    folders.push(value, level, null, null)
                }
            }

            val child = bucket.aggregations
            if (child.asList().size > 0) {
                queue.add(Tuple(child, depth + 1))
                createDynamicHierarchy(queue, folders)
            }
            if (popit) {
                folders.pop()
            } else {
                folders.unstash()
            }
        }
    }

    inner class FolderStack(val root: Folder, val dyhi: DyHierarchy) {
        private val stack = Stack<Folder>()
        private val stash = Stack<Folder>()
        var count = 0
        val folderIds: MutableSet<UUID> = Sets.newHashSetWithExpectedSize(50)

        init {
            this.stack.push(root)
        }

        /**
         * Stash the current stack as the new root.
         */
        fun stash() {
            stash.clear()
            stash.addAll(stack)
        }

        fun unstash() {
            stack.clear()
            stack.addAll(stash)
        }

        fun pop() {
            stack.pop()
        }

        fun peek(): Folder? {
            try {
                return stack.peek()
            } catch (e: NullPointerException) {
            }

            return null
        }

        /**
         * Create a folder at a given level and pushes the result
         * onto the stack.
         *
         * @param value
         * @param level
         * @return
         */
        fun push(value: String, level: DyHierarchyLevel, queryValue: String?, searchParent: Folder?): Folder {
            val parent = stack.peek()
            val search = getSearch(queryValue ?: value, level)

            /*
             * The parent search is merged into the current folder's search.
             */
            if (searchParent != null) {
                if (searchParent.search != null) {
                    search.filter.merge(searchParent.search?.filter)
                }
            } else if (parent.search != null) {
                search.filter.merge(parent.search?.filter)
            }

            val name = getFolderName(value, level)

            /*
             * Create a non-recursive
             */

            val spec = FolderSpec(name, parent.id)
            spec.recursive = false
            spec.dyhiId = dyhi.id
            spec.search = search

            val folder = folderService.create(spec, false)
            if (level.acl != null && spec.created) {
                /**
                 * If the ACL is set, build an ACL based on the name.
                 */
                val acl = Acl()
                for (entry in level.acl) {
                    val perm = entry.getPermission().replace("%\\{name\\}".toRegex(), name)
                    acl.add(AclEntry().setPermission(perm).setAccess(entry.getAccess()))
                }
                folderService.setAcl(folder, acl, true, true)
            }

            count++
            stack.push(folder)
            // Make note of the folder ID.
            folderIds.add(folder.id)
            return folder
        }

        /**
         * Take a value and a level and return the proper folder name.  This
         * will eventually support more formatting options.
         *
         * @param value
         * @param level
         * @return
         */
        fun getFolderName(value: String, level: DyHierarchyLevel): String {
            return when (level.type) {
                DyHierarchyLevelType.Attr, DyHierarchyLevelType.Year, DyHierarchyLevelType.Day -> value
                DyHierarchyLevelType.Month -> java.text.DateFormatSymbols().months[Integer.parseInt(value) - 1]
                else -> value
            }
        }

        /**
         * Build a search from the given folder name and level.
         *
         * @param value
         * @param level
         * @return
         */
        fun getSearch(value: String, level: DyHierarchyLevel): AssetSearch {
            val search = AssetSearch()
            when (level.type) {
                DyHierarchyLevelType.Attr, null -> search.addToFilter().addToTerms(level.field, Lists.newArrayList<Any>(value))
                DyHierarchyLevelType.Year -> search.addToFilter().addToScripts(AssetScript(
                        String.format("doc['%s'].value.year == params.value", level.field),
                        ImmutableMap.of<String, Any>("value", Integer.valueOf(value))))
                DyHierarchyLevelType.Month -> search.addToFilter().addToScripts(AssetScript(
                        String.format("doc['%s'].value.monthOfYear == params.value;", level.field),
                        ImmutableMap.of<String, Any>("value", Integer.valueOf(value))))
                DyHierarchyLevelType.Day -> search.addToFilter().addToScripts(AssetScript(
                        String.format("doc['%s'].value.dayOfMonth == params.value;", level.field),
                        ImmutableMap.of<String, Any>("value", Integer.valueOf(value))))
                DyHierarchyLevelType.Path -> {
                    // chop off trailing /
                    val modifiedValue = value.substring(0, value.length - 1)
                    search.addToFilter().addToTerms(level.field, ImmutableList.of<Any>(modifiedValue))
                }
            }
            return search
        }
    }

    private fun resolveFieldName(level: DyHierarchyLevel) {
        val type = fieldService.getFieldType(level.field)
        if (type == null) {
            throw IllegalStateException("Attempting to agg on a invalid field ${level.field}")
        }
        else if (type == "path") {
            level.field = level.field + ".paths"
        }
        else if (type ==  "string") {
            if (!level.field.endsWith(".raw")) {
                level.field = level.field + ".raw"
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(DyHierarchyServiceImpl::class.java)
    }
}

