package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.ScanAndScrollAssetIterator
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.KwConfFilter
import com.zorroa.archivist.search.RangeQuery
import com.zorroa.archivist.search.SimilarityFilter
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getOrganizationFilter
import com.zorroa.archivist.security.getPermissionsFilter
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.common.clients.SearchBuilder
import com.zorroa.common.util.Json
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.Strings
import org.elasticsearch.common.lucene.search.function.CombineFunction
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.DeprecationHandler
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.geoBoundingBoxQuery
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.SearchModule
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.collapse.CollapseBuilder
import org.elasticsearch.search.sort.FieldSortBuilder.DOC_FIELD_NAME
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.suggest.SuggestBuilder
import org.elasticsearch.search.suggest.SuggestBuilders
import org.elasticsearch.search.suggest.completion.CompletionSuggestion
import org.elasticsearch.search.suggest.completion.context.CategoryQueryContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.OutputStream
import java.util.Arrays
import java.util.UUID
import java.util.stream.Collectors

interface SearchService {

    fun count(builder: AssetSearch): Long

    fun count(ids: List<UUID>, search: AssetSearch?): List<Long>

    fun count(folder: Folder): Long

    fun getSuggestTerms(text: String): List<String>

    fun scanAndScroll(search: AssetSearch, maxResults: Long, clamp: Boolean = false): Iterable<Document>

    /**
     * Execute a scan and scroll and for every hit, call the given function.
     * @param search An asset search
     * @param fetchSource Set to true if your function requires the full doc
     * @param func the function to call for each batch
     */
    fun scanAndScroll(search: AssetSearch, fetchSource: Boolean, func: (hits: SearchHits) -> Unit)

    /**
     * Execute the AssetSearch with the given Paging object.
     *
     * @param page
     * @param search
     * @return
     */
    fun search(page: Pager, search: AssetSearch): PagedList<Document>

    fun search(search: AssetSearch): SearchResponse

    @Throws(IOException::class)
    fun search(page: Pager, search: AssetSearch, stream: OutputStream)

    /**
     * Return the next page of an asset scroll.
     *
     * @param id
     * @param timeout
     * @return
     */
    fun scroll(id: String, timeout: String): PagedList<Document>

    fun buildSearch(search: AssetSearch, type: String): SearchBuilder

    fun getQuery(search: AssetSearch): QueryBuilder
}

class SearchContext(
    val linkedFolders: MutableSet<UUID>,
    val perms: Boolean,
    val postFilter: Boolean,
    var score: Boolean = false
)

@Service
class SearchServiceImpl @Autowired constructor(
    val indexDao: IndexDao,
    val indexRoutingService: IndexRoutingService,
    val properties: ApplicationProperties

) : SearchService {
    @Autowired
    internal lateinit var folderService: FolderService

    @Autowired
    internal lateinit var fieldService: FieldService

    @Autowired
    internal lateinit var fieldSystemService: FieldSystemService

    override fun count(builder: AssetSearch): Long {
        val rest = indexRoutingService.getOrgRestClient()
        val rsp = rest.client.search(
            buildSearch(builder, "asset").request, RequestOptions.DEFAULT
        )
        return rsp.hits.totalHits
    }

    override fun count(ids: List<UUID>, search: AssetSearch?): List<Long> {
        val counts = Lists.newArrayListWithCapacity<Long>(ids.size)
        if (search != null) {
            // Replace any existing folders with each folder to get count.
            // FIXME: Use aggregation for simple folders.
            var filter: AssetFilter? = search.filter
            if (filter == null) {
                filter = AssetFilter()
            }
            var links: MutableMap<String, List<Any>>? = filter.links
            if (links == null) {
                links = Maps.newHashMap()
            }
            for (id in ids) {
                links!!["folder"] = Arrays.asList<Any>(id)
                filter.links = links
                search.filter = filter
                val count = count(search)
                counts.add(count)
            }
        } else {
            for (id in ids) {
                try {
                    val count = count(folderService.get(id))
                    counts.add(count)
                } catch (ignore: Exception) {
                    // probably don't have access to the folder.
                    counts.add(0L)
                }
            }
        }

        return counts
    }

    override fun count(folder: Folder): Long {
        var search: AssetSearch? = folder.search
        return if (search == null) {
            search = AssetSearch()
            search.addToFilter().addToLinks("folder", folder.id)
            count(search)
        } else {
            count(search)
        }
    }

    override fun getSuggestTerms(text: String): List<String> {
        val rest = indexRoutingService.getOrgRestClient()
        val builder = SearchSourceBuilder()
        val suggestBuilder = SuggestBuilder()
        val req = rest.newSearchRequest()

        val ctx = mapOf(
            "organization" to
                listOf<ToXContent>(
                    CategoryQueryContext.builder()
                        .setCategory(getOrgId().toString())
                        .setBoost(1)
                        .setPrefix(false).build()
                )
        )

        val completion = SuggestBuilders.completionSuggestion("system.suggestions")
            .text(text)
            .contexts(ctx)
        suggestBuilder.addSuggestion("suggest", completion)
        builder.suggest(suggestBuilder)
        req.source(builder)

        val response = rest.client.search(req, RequestOptions.DEFAULT).suggest
        val comp: CompletionSuggestion? = response.getSuggestion("suggest")

        return comp?.flatMap {
            it.options.map { opt ->
                opt.text.string()
            }
        }.orEmpty()
    }

    override fun scanAndScroll(search: AssetSearch, fetchSource: Boolean, func: (hits: SearchHits) -> Unit) {
        val rest = indexRoutingService.getOrgRestClient()
        val builder = rest.newSearchBuilder()
        builder.source.query(getQuery(search))
        builder.source.fetchSource(fetchSource)
        builder.source.size(100)
        builder.request.scroll(TimeValue(60000))

        var rsp = rest.client.search(builder.request, RequestOptions.DEFAULT)
        try {
            var totalHits: Long = 0
            do {
                func(rsp.hits)
                totalHits += rsp.hits.totalHits
                val sr = SearchScrollRequest(rsp.scrollId)
                sr.scroll(TimeValue.timeValueSeconds(30))
                rsp = rest.client.scroll(sr, RequestOptions.DEFAULT)
            } while (rsp.hits.hits.isNotEmpty())
        } finally {
            try {
                val cs = ClearScrollRequest()
                cs.addScrollId(rsp.scrollId)
                rest.client.clearScroll(cs, RequestOptions.DEFAULT)
            } catch (e: IOException) {
                logger.warn("failed to clear scan/scroll request, ", e)
            }
        }
    }

    override fun scanAndScroll(search: AssetSearch, maxResults: Long, clamp: Boolean): Iterable<Document> {
        val rest = indexRoutingService.getOrgRestClient()
        val builder = rest.newSearchBuilder()
        builder.source.query(getQuery(search))
        builder.source.size(100)
        builder.request.scroll(TimeValue(60000))

        val rsp = rest.client.search(builder.request, RequestOptions.DEFAULT)

        if (!clamp && maxResults > 0 && rsp.hits.totalHits > maxResults) {
            throw IllegalArgumentException("Asset search has returned more than $maxResults results.")
        }
        return ScanAndScrollAssetIterator(rest.client, rsp, maxResults)
    }

    override fun search(search: AssetSearch): SearchResponse {
        val rest = indexRoutingService.getOrgRestClient()
        return rest.client.search(buildSearch(search, "asset").request, RequestOptions.DEFAULT)
    }

    override fun search(page: Pager, search: AssetSearch): PagedList<Document> {
        val rest = indexRoutingService.getOrgRestClient()
        if (search.scroll != null) {
            val scroll = search.scroll
            if (scroll.id != null) {
                val result = indexDao.getAll(scroll.id, scroll.timeout)
                if (result.size() == 0) {
                    val req = ClearScrollRequest()
                    req.addScrollId(scroll.id)
                    rest.client.clearScroll(req, RequestOptions.DEFAULT)
                }
                return result
            }
        }
        return indexDao.getAll(page, buildSearch(search, "asset"))
    }

    @Throws(IOException::class)
    override fun search(page: Pager, search: AssetSearch, stream: OutputStream) {
        indexDao.getAll(page, buildSearch(search, "asset"), stream)
    }

    override fun scroll(id: String, timeout: String): PagedList<Document> {
        /**
         * Only log valid searches (the ones that are not for the whole repo)
         * since otherwise it creates a lot of logs of empty searches.
         */
        val rest = indexRoutingService.getOrgRestClient()
        val result = indexDao.getAll(id, timeout)
        if (result.size() == 0) {
            val req = ClearScrollRequest()
            req.addScrollId(id)
            rest.client.clearScroll(req, RequestOptions.DEFAULT)
        }
        return result
    }

    override fun buildSearch(search: AssetSearch, type: String): SearchBuilder {
        val rest = indexRoutingService.getOrgRestClient()

        val ssb = SearchSourceBuilder()
        applyCollapse(search, ssb)
        ssb.query(getQuery(search))

        val req = rest.newSearchRequest()
        req.indices("archivist")
        req.types(type)
        req.searchType(SearchType.DFS_QUERY_THEN_FETCH)
        req.preference(getUserId().toString())
        req.source(ssb)

        if (search.aggs != null) {
            val result = mutableMapOf<String, Any>()
            for ((name, agg) in search.aggs) {
                if (agg.containsKey("filter")) {
                    /**
                     * ES no longer supports empty filter aggs, so if curator
                     * submits one, it gets replaced with a match_all query.
                     */
                    val filter = agg["filter"] as Map<String, Any>
                    if (filter.isEmpty()) {
                        agg["filter"] = mapOf<String, Map<String, Any>>("match_all" to mapOf())
                    }
                }

                result[name] = agg
            }
            val map = mutableMapOf("aggs" to search.aggs)
            val json = Json.serializeToString(map)

            val parser = XContentFactory.xContent(XContentType.JSON).createParser(
                xContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, json
            )

            val ssb2 = SearchSourceBuilder.fromXContent(parser)
            ssb2.aggregations().aggregatorFactories.forEach { ssb.aggregation(it) }
        }

        if (search.postFilter != null) {
            val query = QueryBuilders.boolQuery()
            applyFilterToQuery(search.postFilter, query, mutableSetOf())
            ssb.postFilter(query)
        }

        if (search.scroll != null) {
            req.searchType(SearchType.QUERY_THEN_FETCH)
            if (search.scroll.timeout != null) {
                req.scroll(search.scroll.timeout)
            }
        }

        if (search.fields != null) {
            ssb.fetchSource(search.fields, arrayOf("media.content"))
        }

        if (search.scroll != null) {
            ssb.sort(DOC_FIELD_NAME, SortOrder.ASC)
        } else {
            if (search.order != null) {
                val fields = fieldService.getFields("asset")
                for (searchOrder in search.order) {
                    val sortOrder = if (searchOrder.ascending) SortOrder.ASC else SortOrder.DESC
                    // Make sure to use .raw for strings
                    if (!searchOrder.field.endsWith(".raw") &&
                        fields.getValue("string").contains(searchOrder.field)
                    ) {
                        searchOrder.field = searchOrder.field + ".raw"
                    }
                    ssb.sort(searchOrder.field, sortOrder)
                }
            } else {
                ssb.sort("_score", SortOrder.DESC)
                getDefaultSort().forEach { (t, u) ->
                    ssb.sort(t, u)
                }
            }
        }

        if (properties.getBoolean("archivist.debug-mode.enabled")) {
            logger.debug("SEARCH : {}", Strings.toString(ssb, true, true))
        }

        return rest.newSearchBuilder(req, ssb)
    }

    override fun getQuery(search: AssetSearch): QueryBuilder {
        return getQuery(search, mutableSetOf(), perms = true, postFilter = false)
    }

    fun applyCollapse(search: AssetSearch, ssb: SearchSourceBuilder) {
        search.collapse?.let {
            val parser = XContentFactory.xContent(XContentType.JSON).createParser(
                xContentRegistry,
                DeprecationHandler.THROW_UNSUPPORTED_OPERATION, Json.serialize(search.collapse)
            )
            ssb.collapse(CollapseBuilder.fromXContent(parser))
        }
    }

    /**
     *
     * @param postFilter should only be true if applying from folder query.
     */
    private fun getQuery(
        search: AssetSearch,
        linkedFolders: MutableSet<String>,
        perms: Boolean,
        postFilter: Boolean
    ): QueryBuilder {

        val query = QueryBuilders.boolQuery()
        query.filter(getOrganizationFilter())

        if (perms) {
            val permsQuery = getPermissionsFilter(search.access)
            if (permsQuery != null) {
                query.filter(permsQuery)
            }
        }

        if (search == null || (search.filter == null && search.query == null)) {
            query.must(QueryBuilders.matchAllQuery())
        } else {
            val assetBool = QueryBuilders.boolQuery()

            query.minimumShouldMatch(1)
            if (search.isQuerySet) {
                assetBool.must(getQueryStringQuery(search))
            }

            var filter: AssetFilter? = search.filter
            if (filter != null) {
                applyFilterToQuery(filter, assetBool, linkedFolders)
            }

            if (assetBool.hasClauses()) {
                query.should(assetBool)
            }
        }

        // Folders apply their post filter, but the main search// applies the post filter in the SearchRequest.
        // Aggs will be limited to the folders (correct), but
        // not to the filters in the top-level search.
        if (postFilter) {
            if (search.postFilter != null) {
                val postBool = QueryBuilders.boolQuery()
                applyFilterToQuery(search.postFilter, postBool, linkedFolders)
                query.must(postBool)
            }
        }

        applyAssetSearchMetrics(search)

        return query
    }

    private fun linkQuery(query: BoolQueryBuilder, filter: AssetFilter, linkedFolders: MutableSet<String>) {

        val staticBool = QueryBuilders.boolQuery()

        val links = filter.links
        for ((key, value) in links) {
            if (key == "folder") {
                continue
            }
            staticBool.should(QueryBuilders.termsQuery("system.links.$key", value.toString()))
        }

        /*
         * Now do the recursive bit on just folders.
         */
        if (links.containsKey("folder")) {

            val folders = links["folder"]!!
                .stream()
                .map { f -> UUID.fromString(f.toString()) }
                .filter { f -> !linkedFolders.contains(f.toString()) }
                .collect(Collectors.toSet())

            val recursive = if (filter.recursive == null) true else filter.recursive

            if (recursive) {
                val childFolders = mutableSetOf<String>()

                for (folder in folderService.getAllDescendants(
                    folderService.getAll(folders),
                    includeStartFolders = true, forSearch = true
                )) {

                    if (linkedFolders.contains(folder.id.toString())) {
                        continue
                    }
                    linkedFolders.add(folder.id.toString())

                    /**
                     * Not going to allow people to add assets manually
                     * to smart folders, unless its to the smart query itself.
                     */
                    if (folder.search != null) {
                        folder.search.aggs = null
                        logger.info("Getting folder post query!! {}", Json.prettyString(folder.search))
                        staticBool.should(
                            getQuery(
                                folder.search,
                                linkedFolders, perms = false, postFilter = true
                            )
                        )
                    }

                    /**
                     * We don't allow dyhi folders to have manual entries.
                     */
                    if (folder.dyhiId == null && !folder.dyhiRoot) {
                        childFolders.add(folder.id.toString())
                        if (childFolders.size >= 1024) {
                            break
                        }
                    }
                }

                if (childFolders.isNotEmpty()) {
                    staticBool.should(QueryBuilders.termsQuery("system.links.folder", childFolders))
                }
            } else {
                staticBool.should(QueryBuilders.termsQuery("system.links.folder", folders))
            }
        }

        query.must(staticBool)
    }

    private fun getQueryStringQuery(search: AssetSearch): QueryBuilder {
        val queryFields = fieldSystemService.getKeywordAttrNames(search.isExactQuery)
        val qstring = QueryBuilders.queryStringQuery(search.query)
        if (search.isExactQuery) {
            qstring.analyzer("keyword")
        }
        qstring.allowLeadingWildcard(false)
        qstring.lenient(true) // ignores qstring errors

        if (search.queryFields != null) {
            search.queryFields.forEach { (field, boost) ->
                qstring.field(field, boost)
            }
        } else {
            queryFields.forEach { (field, boost) ->
                qstring.field(field, boost)
            }
            qstring.field("system.taxonomy.keywords")
        }
        return qstring
    }

    /**
     * Apply the given filter to the overall boolean query.
     *
     * @param filter
     * @param query
     * @return
     */
    private fun applyFilterToQuery(filter: AssetFilter, query: BoolQueryBuilder, linkedFolders: MutableSet<String>) {

        if (filter.links != null) {
            linkQuery(query, filter, linkedFolders)
        }

        if (filter.prefix != null) {

            val prefixMust = QueryBuilders.boolQuery()
            // models elasticsearch, the Map<String,Object> allows for a boost property
            for ((key, value) in filter.prefix) {
                val prefixFilter = QueryBuilders.prefixQuery(
                    key,
                    value["prefix"] as String
                ).boost(
                    ((value as java.util.Map<String, Any>).getOrDefault(
                        "boost", 1.0
                    ) as Double).toFloat()
                )

                prefixMust.should(prefixFilter)
            }
            query.must(prefixMust)
        }

        if (filter.exists != null) {
            for (term in filter.exists) {
                val existsFilter = QueryBuilders.existsQuery(term)
                query.must(existsFilter)
            }
        }

        if (filter.missing != null) {
            for (term in filter.missing) {
                val existsFilter = QueryBuilders.existsQuery(term)
                query.mustNot(existsFilter)
            }
        }

        if (filter.terms != null) {
            for ((key, value) in filter.terms) {
                val values = value.orEmpty().filterNotNull()
                    .map {
                        if (it is UUID) {
                            it.toString()
                        } else {
                            it
                        }
                    }
                if (values.isNotEmpty()) {
                    val termsQuery = QueryBuilders.termsQuery(fieldService.dotRaw(key), values)
                    query.must(termsQuery)
                }
            }
        }

        if (filter.range != null) {
            for ((field, rq) in filter.range) {
                val rqb = RangeQueryBuilder(field)

                for (f in RangeQuery::class.java.declaredFields) {
                    try {
                        val m = RangeQueryBuilder::class.java.getMethod(f.name, f.type)
                        val v = f.get(rq) ?: continue
                        m.invoke(rqb, v)
                    } catch (e: Exception) {
                        throw IllegalArgumentException(
                            "RangeQueryBuilder has no '" + f.name + "' method"
                        )
                    }
                }
                query.must(rqb)
            }
        }

        if (filter.scripts != null) {

            for (script in filter.scripts) {
                val scriptFilterBuilder = QueryBuilders.scriptQuery(
                    Script(
                        ScriptType.INLINE,
                        "painless", script.script, script.params
                    )
                )
                query.must(scriptFilterBuilder)
            }
        }

        if (filter.similarity != null) {
            handleHammingFilter(filter.similarity, query)
        }

        if (filter.kwconf != null) {
            handleKwConfFilter(filter.kwconf, query)
        }

        if (filter.query != null) {
            query.must(getQueryStringQuery(filter.query))
        }

        // Recursively add bool sub-filters for must, must_not and should
        if (filter.must != null) {
            for (f in filter.must) {
                val must = QueryBuilders.boolQuery()
                this.applyFilterToQuery(f, must, linkedFolders)
                query.must(must)
            }
        }

        if (filter.mustNot != null) {
            for (assetFilter in filter.mustNot) {
                val mustNot = QueryBuilders.boolQuery()
                this.applyFilterToQuery(assetFilter, mustNot, linkedFolders)
                query.mustNot(mustNot)
            }
        }

        if (filter.should != null) {
            for (assetFilter in filter.should) {
                val should = QueryBuilders.boolQuery()
                this.applyFilterToQuery(assetFilter, should, linkedFolders)
                query.should(should)
            }
        }

        if (filter.geo_bounding_box != null) {
            val bbox = filter.geo_bounding_box
            for ((field, value) in bbox) {
                if (value != null) {
                    val tl = value.topLeftPoint()
                    val br = value.bottomRightPoint()
                    val bboxQuery = geoBoundingBoxQuery(field)
                        .setCorners(tl[0], tl[1], br[0], br[1])
                    query.should(bboxQuery)
                }
            }
        }
    }

    private fun handleKwConfFilter(filters: Map<String, KwConfFilter>, query: BoolQueryBuilder) {
        val bool = QueryBuilders.boolQuery()
        query.must(bool)

        for ((field, filter) in filters) {
            val args = mutableMapOf<String, Any>()
            args["field"] = field
            args["keywords"] = filter.keywords
            args["range"] = filter.range

            val fsqb = QueryBuilders.functionScoreQuery(
                ScoreFunctionBuilders.scriptFunction(
                    Script(
                        ScriptType.INLINE,
                        "zorroa-kwconf", "kwconf", args
                    )
                )
            )

            fsqb.minScore = filter.range[0].toFloat()
            fsqb.boostMode(CombineFunction.REPLACE)
            fsqb.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)

            val fbool = QueryBuilders.boolQuery()
            fbool.must(fsqb)
            fbool.must(QueryBuilders.termsQuery("$field.keyword.raw", filter.keywords))
            bool.should(fbool)
        }
    }

    private fun handleHammingFilter(filters: Map<String, SimilarityFilter>, query: BoolQueryBuilder) {

        val hammingBool = QueryBuilders.boolQuery()
        query.must(hammingBool)

        for ((field, filter) in filters) {

            /**
             * Resolve any asset Ids in the hash list.
             */

            val hashes = mutableListOf<String>()
            val weights = mutableListOf<Float>()

            for (hash in filter.hashes) {
                var hashValue: String? = hash.hash
                if (JdbcUtils.isUUID(hashValue)) {
                    hashValue = indexDao.getFieldValue(hashValue as String, field)
                }

                if (hashValue != null) {
                    hashes.add(hashValue)
                    weights.add(if (hash.weight == null) 1.0f else hash.weight)
                } else {
                    logger.warn("could not find value at: {} {}", hashValue, field)
                }
            }

            val args = mutableMapOf<String, Any>()
            args["field"] = field
            args["hashes"] = hashes.joinToString(",")
            args["weights"] = weights.joinToString(",")
            args["minScore"] = filter.minScore

            val fsqb = QueryBuilders.functionScoreQuery(
                ScoreFunctionBuilders.scriptFunction(
                    Script(
                        ScriptType.INLINE,
                        "zorroa-similarity", "similarity", args
                    )
                )
            )

            fsqb.minScore = filter.minScore / 100.0f
            fsqb.boostMode(CombineFunction.REPLACE)
            fsqb.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)

            hammingBool.should(fsqb)
        }
    }

    fun getDefaultSort(): Map<String, SortOrder> {
        val result = linkedMapOf<String, SortOrder>()
        val sortFields = properties.getList("archivist.search.sortFields")

        sortFields.asSequence()
            .map { it.split(":", limit = 2) }
            .forEach {
                val order = try {
                    SortOrder.valueOf(it[1])
                } catch (e: IllegalArgumentException) {
                    SortOrder.ASC
                }
                result[it[0]] = order
            }

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SearchServiceImpl::class.java)

        /**
         * Used for ES XContentParsers
         */
        val searchModule = SearchModule(Settings.EMPTY, false, emptyList())

        /**
         * Used for ES XContentParsers
         */
        val xContentRegistry = NamedXContentRegistry(searchModule.namedXContents)
    }
}
