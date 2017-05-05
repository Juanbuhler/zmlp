package com.zorroa.common.repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssetDaoImpl extends AbstractElasticDao implements AssetDao {

    @Override
    public String getType() {
        return "asset";
    }

    @Override
    public String getIndex() {
        return "archivist";
    }

    /**
     * Allows us to flush the first batch.
     */
    private final AtomicLong flushTime = new AtomicLong(0);

    private static final JsonRowMapper<Asset> MAPPER = (id, version, score, source) -> {
        Map<String, Object> data = Json.deserialize(source, Json.GENERIC_MAP);
        Asset result = new Asset();
        result.setId(id);
        result.setScore(score);
        result.setDocument(data);
        result.setType("asset");
        return result;
    };

    @Override
    public Asset index(Source source, LinkSpec sourceLink) {
        UpdateRequestBuilder upsert = prepareUpsert(source);
        return new Asset(upsert.get().getId(), source.getDocument());
    }

    @Override
    public DocumentIndexResult index(List<Source> sources, LinkSpec sourceLink) {
        DocumentIndexResult result = new DocumentIndexResult();
        if (sources.isEmpty()) {
            return result;
        }
        List<Source> retries = Lists.newArrayList();
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        /**
         * Force a refresh if we haven't for a while.
         */
        final long time = System.currentTimeMillis();
        if (time - flushTime.getAndSet(time) > 30000) {
            bulkRequest.setRefresh(true);
        }

        for (Source source : sources) {
            bulkRequest.add(prepareUpsert(source));
        }

        BulkResponse bulk = bulkRequest.get();
        List<String> created = Lists.newArrayList();

        int index = 0;
        for (BulkItemResponse response : bulk) {
            if (response.isFailed()) {
                String message = response.getFailure().getMessage();
                Source asset = sources.get(index);
                if (removeBrokenField(asset, message)) {
                    result.warnings++;
                    retries.add(sources.get(index));
                } else {
                    logger.warn("Failed to index {}, {}", response.getId(), message);
                    result.logs.add(new StringBuilder(1024).append(
                            message).append(",").append(asset.getPath()).toString());
                    result.errors++;
                }
            } else {
                UpdateResponse update = response.getResponse();
                if (update.isCreated()) {
                    created.add(update.getId());
                    result.created++;
                } else {
                    result.updated++;
                }
            }
            index++;
        }

        /*
         * TODO: limit number of retries to reasonable number.
         */
        if (!retries.isEmpty()) {
            result.retries++;
            result.add(index(retries, sourceLink));
        }

        if (!created.isEmpty() && sourceLink != null) {
            appendLink(sourceLink.getType(), sourceLink.getId(), created);
        }

        return result;
    }

    private UpdateRequestBuilder prepareUpsert(Source source) {
        byte[] doc = Json.serialize(source.getDocument());
        return client.prepareUpdate(getIndex(), source.getType(), source.getId())
                .setDoc(doc)
                .setUpsert(doc);
    }

    private static final Pattern[] RECOVERABLE_BULK_ERRORS = new Pattern[] {
            Pattern.compile("^MapperParsingException\\[failed to parse \\[(.*?)\\]\\];"),
            Pattern.compile("\"term in field=\"(.*?)\"\""),
            Pattern.compile("mapper \\[(.*?)\\] of different type")
    };

    private boolean removeBrokenField(Source asset, String error) {
        for (Pattern pattern: RECOVERABLE_BULK_ERRORS) {
            Matcher matcher = pattern.matcher(error);
            if (matcher.find()) {
                logger.warn("Removing broken field from {}: {}, {}", asset.getId(), matcher.group(1), error);
                return asset.removeAttr(matcher.group(1));
            }
        }
        return false;
    }

    @Override
    public Map<String, List<Object>> removeLink(String type, Object value, List<String> assets) {
        if (type.contains(".")) {
            throw new IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)");
        }

        Map<String,Object> link = ImmutableMap.of("type", type, "id", value);
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (String id: assets) {
            UpdateRequestBuilder updateBuilder = client.prepareUpdate(getIndex(), getType(), id);
            updateBuilder.setScript(new Script("remove_link",
                    ScriptService.ScriptType.INDEXED, "groovy",
                    link));
            bulkRequest.add(updateBuilder);
        }

        Map<String, List<Object>> result = Maps.newHashMapWithExpectedSize(assets.size());
        result.put("success", Lists.newArrayList());
        result.put("failed", Lists.newArrayList());

        BulkResponse bulk = bulkRequest.setRefresh(true).get();
        for (BulkItemResponse rsp:  bulk.getItems()) {
            if (rsp.isFailed()) {
                result.get("failed").add(ImmutableMap.of("id", rsp.getId(), "error", rsp.getFailureMessage()));
                logger.warn("Failed to unlink asset: {}",
                        rsp.getFailureMessage(), rsp.getFailure().getCause());
            }
            else {
                result.get("success").add(rsp.getId());
            }
        }
        return result;
    }

    @Override
    public Map<String, List<Object>> appendLink(String type, Object value, List<String> assets) {
        if (type.contains(".")) {
            throw new IllegalArgumentException("Attribute cannot contain a sub attribute. (no dots in name)");
        }
        Map<String, Object> link = ImmutableMap.of("type", type, "id", value);

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (String id: assets) {
            UpdateRequestBuilder updateBuilder = client.prepareUpdate(getIndex(), getType(), id);

            updateBuilder.setScript(new Script("append_link",
                    ScriptService.ScriptType.INDEXED, "groovy",
                    link));

            bulkRequest.add(updateBuilder);
        }

        Map<String, List<Object>> result = Maps.newHashMapWithExpectedSize(assets.size());
        result.put("success", Lists.newArrayList());
        result.put("failed", Lists.newArrayList());

        BulkResponse bulk = bulkRequest.setRefresh(true).get();
        for (BulkItemResponse rsp:  bulk.getItems()) {
            if (rsp.isFailed()) {
                result.get("failed").add(ImmutableMap.of("id", rsp.getId(), "error", rsp.getFailureMessage()));
                logger.warn("Failed to link asset: {}",
                        rsp.getFailureMessage(), rsp.getFailure().getCause());
            }
            else {
                result.get("success").add(rsp.getId());
            }
        }

        return result;
    }

    @Override
    public long update(String assetId, Map<String, Object> values) {
        Asset asset = get(assetId);
        for (Map.Entry<String,Object> entry: values.entrySet()) {
            asset.setAttr(entry.getKey(), entry.getValue());
        }

        UpdateRequestBuilder updateBuilder = client.prepareUpdate(getIndex(), getType(), assetId)
            .setDoc(Json.serializeToString(asset.getDocument()))
            .setRefresh(true);

        UpdateResponse response = updateBuilder.get();
        return response.getVersion();
    }

    @Override
    public boolean delete(String id) {
        return client.prepareDelete(getIndex(),getType(),id).get().isFound();
    }

    @Override
    public Asset get(String id) {
        return elastic.queryForObject(id, MAPPER);
    }

    @Override
    public boolean exists(Path path) {
        return client.prepareSearch(getIndex())
                .setQuery(QueryBuilders.termQuery("source.path.raw", path.toString()))
                .setSize(0)
                .get().getHits().getTotalHits() > 0;
    }

    @Override
    public Asset get(Path path) {
        List<Asset> assets = elastic.query(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(1)
                .setQuery(QueryBuilders.termQuery("source.path.raw", path.toString())), MAPPER);

        if (assets.isEmpty()) {
            return null;
        }
        return assets.get(0);
    }

    @Override
    public PagedList<Asset> getAll(String id, String timeout) {
        return elastic.scroll(id ,timeout, MAPPER);
    }

    @Override
    public PagedList<Asset> getAll(Pager page, SearchRequestBuilder search) {
        return elastic.page(search, page, MAPPER);
    }

    @Override
    public void getAll(Pager page, SearchRequestBuilder search, OutputStream stream) throws IOException {
        elastic.page(search, page, MAPPER, stream);
    }

    @Override
    public PagedList<Asset> getAll(Pager page) {
        return elastic.page(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery())
                .setVersion(true), page, MAPPER);

    }

    @Override
    public Map<String, Object> getMapping() {
        ClusterState cs = client.admin().cluster().prepareState().setIndices(
                getIndex()).execute().actionGet().getState();
        // Should only be one concrete index.
        for (String index: cs.getMetaData().concreteAllOpenIndices()) {
            IndexMetaData imd = cs.getMetaData().index(index);
            MappingMetaData mdd = imd.mapping("asset");
            try {
                return mdd.getSourceAsMap();
            } catch (IOException e) {
                throw new ArchivistException(e);
            }
        }
        return ImmutableMap.of();
    }
}
