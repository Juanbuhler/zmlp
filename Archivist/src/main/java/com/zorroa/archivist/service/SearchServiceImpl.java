package com.zorroa.archivist.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.ScanAndScrollAssetIterator;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.security.SecurityUtils;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Created by chambers on 9/25/15.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);


    @Autowired
    FolderService folderService;

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Override
    public SearchResponse search(AssetSearch search) {
        return buildSearch(search).get();
    }

    @Override
    public CountResponse count(AssetSearch builder) {
        return buildCount(builder).get();
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
    public long getTotalFileSize(AssetSearch search) {
        Sum sum = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(search))
                .addAggregation(AggregationBuilders.sum("totalFileSize").field("source.fileSize"))
                .setSearchType(SearchType.COUNT)
                .get().getAggregations().get("totalFileSize");

        return (long) sum.getValue();
    }

    public Iterable<Asset> scanAndScroll(AssetSearch search) {

        SearchResponse rsp = client.prepareSearch(alias)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(getQuery(search))
                .setSize(100).execute().actionGet();

        return new ScanAndScrollAssetIterator(client, rsp);
    }

    private SearchRequestBuilder buildSearch(AssetSearch search) {

        SearchRequestBuilder request = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(search));

        if (search.getFrom() != null) {
            request.setFrom(search.getFrom());
        }

        if (search.getSize() != null) {
            request.setSize(search.getSize());
        }

        logger.info(request.toString());

        /*
         * TODO: alternative sorting and paging here.
         */

        return request;
    }

    private CountRequestBuilder buildCount(AssetSearch search) {
        CountRequestBuilder count = client.prepareCount(alias)
                .setTypes("asset")
                .setQuery(getQuery(search));
        logger.info(count.toString());
        return count;
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

    private QueryBuilder getQuery(@Nullable AssetSearch search) {
        if (search == null) {
            return QueryBuilders.filteredQuery(
                    QueryBuilders.matchAllQuery(), getFilter(null));
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (search.isQuerySet()) {
            query.must(getQueryStringQuery(search));
        }

        AssetFilter filter = search.getFilter();
        query.must(folderQuery(filter));

        return QueryBuilders.filteredQuery(query, getFilter(filter));
    }

    // Combine the folder search and filter using SHOULD.
    // Merged into the main query as a MUST above.
    private QueryBuilder folderQuery(AssetFilter filter) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (filter.getFolderIds() != null) {
            Set<Integer> folderIds = Sets.newHashSetWithExpectedSize(64);
            for (Folder folder : folderService.getAllDescendants(
                    folderService.getAll(filter.getFolderIds()), true)) {
                if (folder.getSearch() != null) {
                    query.should(getQuery(folder.getSearch()));
                }
                folderIds.add(folder.getId());
            }

            if (!folderIds.isEmpty()) {
                query.should(QueryBuilders.termsQuery("folders", folderIds));
            }
        }
        return query;
    }

    private QueryBuilder getQueryStringQuery(AssetSearch search) {

        String query = search.getQuery();
        if (search.isFuzzy() && query != null) {
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

        /*
         * Note: the default boost is 1
         *
         * With 5 keyword buckets, the boosts work out to:
         * 1 = 0.1
         * 2 = 0.5
         * 3 = 1.0 (DEFAULT)
         * 4 = 1.5
         * 5 = 2.0
         *
         * The indexed fields get 1/2 as much boost.
         *
         * TODO: switch to BigDecmial to deal with rounding errors.
         * TODO: function score query is another option
         */
        long highBucket = KeywordsSchema.getBucket(search.getConfidence());
        for (long i=highBucket; i<=KeywordsSchema.BUCKET_COUNT; i++) {
            float boost = Math.max(0.1f, (i - 1) * 0.5f);
            qstring.field(String.format("keywords.level%d.raw", i), boost);
            qstring.field(String.format("keywords.level%d", i), boost * 0.5f);
        }
        /*
         * leading wildcards can cause load issues.
         */
        qstring.allowLeadingWildcard(false);
        qstring.lenient(true);
        qstring.fuzziness(Fuzziness.AUTO);
        return qstring;
    }


    /**
     * Builds an "AND" filter based on all the options in the AssetFilter.
     *
     * @param builder
     * @return
     */
    private FilterBuilder getFilter(AssetFilter builder) {
        AndFilterBuilder filter = FilterBuilders.andFilter();
        filter.add(SecurityUtils.getPermissionsFilter());

        if (builder == null) {
            return filter;
        }

        if (builder.getAssetIds() != null) {
            FilterBuilder assetsFilterrBuilder = FilterBuilders.termsFilter("_id", builder.getAssetIds());
            filter.add(assetsFilterrBuilder);
        }

        if (builder.getExportIds() != null) {
            for (Integer id : builder.getExportIds()) {
                FilterBuilder exportFilterBuilder = FilterBuilders.termFilter("exports", id);
                filter.add(exportFilterBuilder);
            }
        }

        if (builder.getExistFields() != null) {
            for (String term : builder.getExistFields()) {
                FilterBuilder existsFilterBuilder = FilterBuilders.existsFilter(term);
                filter.add(existsFilterBuilder);
            }
        }

        if (builder.getFieldTerms() != null) {
            for (AssetFieldTerms fieldTerms : builder.getFieldTerms()) {
                FilterBuilder termsFilterBuilder = FilterBuilders.termsFilter(fieldTerms.getField(), fieldTerms.getTerms());
                filter.add(termsFilterBuilder);
            }
        }

        if (builder.getFieldRanges() != null) {
            for (AssetFieldRange fieldRange : builder.getFieldRanges()) {
                FilterBuilder rangeFilterBuilder = FilterBuilders.rangeFilter(fieldRange.getField())
                        .gte(fieldRange.getMin())
                        .lt(fieldRange.getMax());
                filter.add(rangeFilterBuilder);
            }
        }

        if (builder.getScripts() != null) {
            for (AssetScript script : builder.getScripts()) {
                FilterBuilder scriptFilterBuilder = FilterBuilders.scriptFilter(script.getScript())
                        .lang("native")
                        .params(script.getParams());
                filter.add(scriptFilterBuilder);
            }
        }

        return filter;
    }
}
