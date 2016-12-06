package com.zorroa.archivist.service;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.ArchivistReadException;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.search.*;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Created by chambers on 9/25/15.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    AssetDao assetDao;

    @Autowired
    FolderService folderService;

    @Autowired
    LogService logService;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Autowired
    ApplicationProperties properties;

    private Map<String, Float> defaultQueryFields =
            ImmutableMap.of("keywords.all", 1.0f, "keywords.all.raw", 2.0f);

    @PostConstruct
    public void init() {
        initializeDefaultQueryFields();
    }

    @Override
    public SearchResponse search(AssetSearch search) {
        SearchResponse rsp =  buildSearch(search)
                .setFrom(search.getFrom() == null ? 0 : search.getFrom())
                .setSize(search.getSize() == null ? 10 : search.getSize()).get();
        return rsp;
    }

    @Override
    public long count(AssetSearch builder) {
        return buildSearch(builder).setSize(0).get().getHits().getTotalHits();
    }

    @Override
    public long count(Folder folder) {
        AssetSearch search = folder.getSearch();
        if (search != null && search.getFilter() != null) {
            search.getFilter().addToLinks("folder", String.valueOf(folder.getId()));
            return count(search);
        }
        else {
            search = new AssetSearch();
            search.addToFilter().addToLinks("folder", String.valueOf(folder.getId()));
            return count(search);
        }
    }

    @Override
    public SuggestResponse suggest(AssetSuggestBuilder builder) {
        return buildSuggest(builder).get();
    }

    @Override
    public SearchResponse aggregate(AssetAggregateBuilder builder) {
        return buildAggregate(builder).get();
    }

    @Override
    public Iterable<Asset> scanAndScroll(AssetSearch search, int maxResults) {
        SearchResponse rsp = client.prepareSearch(alias)
                .setScroll(new TimeValue(60000))
                .addSort("_doc", SortOrder.ASC)
                .setQuery(getQuery(search))
                .setSize(100).execute().actionGet();

        if (rsp.getHits().getTotalHits() > maxResults) {
            throw new ArchivistReadException("Asset search has returned more than " + maxResults + " results.");
        }
        return new ScanAndScrollAssetIterator(client, rsp);
    }

    private boolean isSearchLogged(Pager page, AssetSearch search) {
        if (!search.isEmpty() && page.getClosestPage() == 1) {
            Scroll scroll = search.getScroll();
            if (scroll != null) {
                // Don't log subsequent searchs.
                if (scroll.getId() != null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public PagedList<Asset> search(Pager page, AssetSearch search) {
        /**
         * If the search is not empty (its a valid search) and the page
         * number is 1, then log the search.
         */
        if (isSearchLogged(page, search)) {
            logService.log(new LogSpec().build(LogAction.Search, search));
        }

        if (search.getScroll() != null) {
            Scroll scroll = search.getScroll();
            if (scroll.getId() != null) {
                return assetDao.getAll(scroll.getId(), scroll.getTimeout());
            }
        }

        return assetDao.getAll(page, buildSearch(search));
    }

    @Override
    public PagedList<Asset> scroll(String id, String timeout) {
        /**
         * Only log valid searches (the ones that are not for the whole repo)
         * since otherwise it creates a lot of logs of empty searches.
         */
        return assetDao.getAll(id, timeout);
    }

    private SearchRequestBuilder buildSearch(AssetSearch search) {

        SearchRequestBuilder request = client.prepareSearch(alias)
                .setTypes("asset")
                .setPreference(SecurityUtils.getCookieId())
                .setQuery(getQuery(search));

        if (search.getAggs() != null) {
            request.setAggregations(getAggregations(search));
        }

        if (search.getPostFilter() != null) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            applyFilterToQuery(search.getPostFilter(), query);
            request.setPostFilter(query);
        }

        if (search.getScroll()!= null) {
            if (search.getScroll().getTimeout() != null) {
                request.setScroll(search.getScroll().getTimeout());
            }
        }

        if (search.getFields() != null) {
            request.setFetchSource(search.getFields(), new String[] { "links", "permissions"} );
        }

        if (search.getScroll() != null) {
            request.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC);
        }
        else {
            if (search.getOrder() != null) {
                for (AssetSearchOrder searchOrder : search.getOrder()) {
                    SortOrder sortOrder = searchOrder.getAscending() ? SortOrder.ASC : SortOrder.DESC;
                    request.addSort(searchOrder.getField(), sortOrder);
                }
            }
            else {
                /**
                 * The default sort, if we are not using scroll, is first
                 * by score, then by date (newest first), then tie breaker is
                 * doc id.
                 */
                request.addSort(SortParseElement.SCORE_FIELD_NAME, SortOrder.DESC);
                request.addSort("_timestamp", SortOrder.DESC);
            }
        }
        return request;
    }

    private SuggestRequestBuilder buildSuggest(AssetSuggestBuilder builder) {
        // FIXME: We need to use builder.search in here somehow!
        CompletionSuggestionBuilder completion = new CompletionSuggestionBuilder("completions")
                .text(builder.getText())
                .field("keywords.suggest");
        SuggestRequestBuilder suggest = client.prepareSuggest(alias)
                .addSuggestion(completion);
        return  suggest;
    }

    private SearchRequestBuilder buildAggregate(AssetAggregateBuilder builder) {
        SearchRequestBuilder aggregation = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(builder.getSearch()))
                .setAggregations(builder.getAggregations())
                .setSearchType(SearchType.COUNT);
        return aggregation;
    }

    private Map<String, Object> getAggregations(AssetSearch search) {
        if (search.getAggs() == null) {
            return null;
        }
        Map<String, Object> result = Maps.newHashMap();
        for (Map.Entry<String, Map<String, Object>> entry: search.getAggs().entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private QueryBuilder getQuery(AssetSearch search) {
        return getQuery(search, true, false);
    }

    private QueryBuilder getQuery(AssetSearch search, boolean perms, boolean postFilter) {
        if (search == null) {
            return QueryBuilders.filteredQuery(
                    QueryBuilders.matchAllQuery(), SecurityUtils.getPermissionsFilter());
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (perms) {
            query.must(SecurityUtils.getPermissionsFilter());
        }

        if (search.isQuerySet()) {
            query.must(getQueryStringQuery(search));
        }

        AssetFilter filter = search.getFilter();
        if (filter != null) {
            applyFilterToQuery(filter, query);
        }

        // Folders apply their post filter, but the main search
        // applies the post filter in the SearchRequest.
        // Aggs will be limited to the folders (correct), but
        // not to the filters in the top-level search.
        if (postFilter) {
            filter = search.getPostFilter();
            if (filter != null) {
                applyFilterToQuery(filter, query);
            }
        }

        return query;
    }

    private void linkQuery(BoolQueryBuilder query, AssetFilter filter) {

        BoolQueryBuilder staticBool = QueryBuilders.boolQuery();

        Map<String, List<Object>> links = filter.getLinks();
        for (Map.Entry<String, List<Object>> link: links.entrySet()) {
            if (link.getKey().equals("folder")) {
                continue;
            }
            staticBool.should(QueryBuilders.termsQuery("links." + link.getKey(), link.getValue()));
        }

        /*
         * Now do the recursive bit on just folders.
         */
        if (links.containsKey("folder")) {

            List<Integer> folders = links.get("folder")
                    .stream().map(f->Integer.valueOf(f.toString())).collect(Collectors.toList());

            Set<String> childFolders = Sets.newHashSet();
            for (Folder folder : folderService.getAllDescendants(
                    folderService.getAll(folders), true, true)) {

                /**
                 * Not going to allow people to add assets manually
                 * to smart folders, unless its to the smart query itself.
                 */
                if (folder.getSearch() != null) {
                    staticBool.should(getQuery(folder.getSearch(), false, true));
                }

                /**
                 * We don't allow dyhi folders to have manual entries.
                 */
                if (folder.getDyhiId() == null && !folder.isDyhiRoot()) {
                    childFolders.add(String.valueOf(folder.getId()));
                }
            }

            if (!childFolders.isEmpty()) {
                staticBool.should(QueryBuilders.termsQuery("links.folder", childFolders));
            }
        }

        query.must(staticBool);
    }

    private QueryBuilder getQueryStringQuery(AssetSearch search) {

        /**
         * Note: fuzzy defaults to true.
         */
        String query = search.getQuery();
        boolean fuzzy = search.getFuzzy() != null ? search.getFuzzy() : true;
        if (fuzzy && query != null) {
            StringBuilder sb = new StringBuilder(query.length() + 10);
            for (String part: Splitter.on(" ").omitEmptyStrings().trimResults().split(query)) {
                sb.append(part);
                if (part.endsWith("~")) {
                    sb.append(" ");
                }
                else {
                    sb.append("~ ");
                }
            }
            sb.deleteCharAt(sb.length()-1);
            query = sb.toString();
        }

        QueryStringQueryBuilder qstring = QueryBuilders.queryStringQuery(query);

        Map<String, Float> queryFields = null;

        if (JdbcUtils.isValid(search.getQueryFields())) {
            queryFields = search.getQueryFields();
        }
        else if (JdbcUtils.isValid(SecurityUtils.getUser().getSettings().getSearch())) {
            queryFields = SecurityUtils.getUser().getSettings().getSearch().getQueryFields();
        }

        if (!JdbcUtils.isValid(queryFields)) {
            queryFields = defaultQueryFields;
        }

        queryFields.forEach((k,v)-> qstring.field(k, v));
        qstring.allowLeadingWildcard(false);
        qstring.lenient(true);
        qstring.fuzziness(Fuzziness.AUTO);
        return qstring;
    }


    /**
     * Apply the given filter to the overall boolean query.
     *
     * @param filter
     * @param query;
     * @return
     */
    private void applyFilterToQuery(AssetFilter filter, BoolQueryBuilder query) {

        if (filter.getLinks() != null) {
            linkQuery(query, filter);
        }

        if (filter.getColors() != null) {
            for (Map.Entry<String, List<ColorFilter>> entry : filter.getColors().entrySet()) {
                for (ColorFilter color: entry.getValue()) {
                    String field = entry.getKey();
                    BoolQueryBuilder colorFilter = QueryBuilders.boolQuery();
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".ratio"))
                            .gte(color.getMinRatio()).lte(color.getMaxRatio()));
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".hue"))
                            .gte(color.getHue() - color.getHueRange())
                            .lte(color.getHue() + color.getHueRange()));
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".saturation"))
                            .gte(color.getSaturation() - color.getSaturationRange())
                            .lte(color.getSaturation() + color.getSaturationRange()));
                    colorFilter.must(QueryBuilders.rangeQuery(field.concat(".brightness"))
                            .gte(color.getBrightness() - color.getBrightnessRange())
                            .lte(color.getBrightness() + color.getBrightnessRange()));

                    QueryBuilder colorFilterBuilder = QueryBuilders.nestedQuery(field,
                            colorFilter);
                    query.must(colorFilterBuilder);
                }
            }
        }

        if (filter.getExists() != null) {
            for (String term : filter.getExists()) {
                QueryBuilder existsFilter = QueryBuilders.existsQuery(term);
                query.must(existsFilter);
            }
        }

        if (filter.getMissing() != null) {
            for (String term : filter.getMissing()) {
                QueryBuilder missingFilter = QueryBuilders.missingQuery(term);
                query.must(missingFilter);
            }
        }

        if (filter.getTerms()!= null) {
            for (Map.Entry<String, List<Object>> term : filter.getTerms().entrySet()) {
                QueryBuilder termsQuery = QueryBuilders.termsQuery(term.getKey(), term.getValue());
                query.must(termsQuery);
            }
        }

        if (filter.getRange() != null) {
            for (Map.Entry<String, RangeQuery> entry: filter.getRange().entrySet()) {
                String field = entry.getKey();
                RangeQuery rq = entry.getValue();
                RangeQueryBuilder rqb = new RangeQueryBuilder(field);

                for (Field f: RangeQuery.class.getDeclaredFields()) {
                    try {
                        Method m = RangeQueryBuilder.class.getMethod(f.getName(), f.getType());
                        Object v = f.get(rq);
                        if (v == null) {
                            continue;
                        }
                        m.invoke(rqb, v);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                "RangeQueryBuilder has no '" + f.getName() + "' method");
                    }
                }
                query.must(rqb);
            }
        }

        if (filter.getScripts() != null) {
            for (AssetScript script : filter.getScripts()) {
                QueryBuilder scriptFilterBuilder = QueryBuilders.scriptQuery(new Script(
                        script.getScript(), ScriptService.ScriptType.INLINE, script.getType(), script.getParams()));
                query.must(scriptFilterBuilder);
            }
        }

        if (filter.getHamming() != null) {
            QueryBuilder hammingScript = QueryBuilders.scriptQuery(new Script(
                    "hammingDistance", ScriptService.ScriptType.INLINE, "native",
                    ImmutableMap.of("field", filter.getHamming().getField(),
                            "hash", filter.getHamming().getHash())));
            query.must(hammingScript);
        }
    }

    @Override
    public Map<String, Set<String>> getFields() {
        Map<String, Set<String>> result = Maps.newHashMapWithExpectedSize(16);
        ClusterState cs = client.admin().cluster().prepareState().setIndices(alias).execute().actionGet().getState();
        for (String index: cs.getMetaData().concreteAllOpenIndices()) {
            IndexMetaData imd = cs.getMetaData().index(index);
            MappingMetaData mdd = imd.mapping("asset");
            try {
                getList(result, "", mdd.getSourceAsMap());
            } catch (IOException e) {
                throw new ArchivistException(e);
            }
        }
        return result;
    }

    private static final Set<String> NAME_TYPE_OVERRRIDES = ImmutableSet.of("point");

    private static void getList(Map<String, Set<String>> result, String fieldName, Map<String, Object> mapProperties) {
        Map<String, Object> map = (Map<String, Object>) mapProperties.get("properties");
        for (String key : map.keySet()) {
            Map<String, Object> item = (Map<String, Object>) map.get(key);

            if (item.containsKey("type")) {
                String type = (String) item.get("type");
                if (NAME_TYPE_OVERRRIDES.contains(key)) {
                    type = key;
                }
                Set<String> fields = result.get(type);
                if (fields == null) {
                    fields = new TreeSet<>();
                    result.put(type, fields);
                }
                fields.add(String.join("", fieldName, key));
            } else {
                getList(result, String.join("", fieldName, key, "."), item);
            }
        }
    }

    private void initializeDefaultQueryFields() {
        Map<String, Object> queryFieldProps =
                properties.getMap("archivist.search.queryFields");

        if (!queryFieldProps.isEmpty()) {
            /**
             * Using ImmutableMap.Builder to ensure this default cannot
             * be modified by accident.
             */
            ImmutableMap.Builder<String, Float> builder = ImmutableMap.builder();
            queryFieldProps.forEach((k,v)-> builder.put(
                    k.replace("archivist.search.queryFields.",""),
                    Float.valueOf(v.toString())));
            defaultQueryFields = builder.build();
        }
        logger.info("Default search fields: {}", defaultQueryFields);
    }
}
