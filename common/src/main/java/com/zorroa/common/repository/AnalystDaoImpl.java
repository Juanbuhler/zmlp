package com.zorroa.common.repository;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystBuilder;
import com.zorroa.sdk.domain.AnalystState;
import com.zorroa.sdk.domain.AnalystUpdateBuilder;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;

/**
 * Created by chambers on 6/16/16.
 */
public class AnalystDaoImpl  extends AbstractElasticDao implements AnalystDao {

    private NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

    @Override
    public String getType() {
        return "analyst";
    }

    @Override
    public String getIndex() {
        return "analyst";
    }

    @Override
    public String register(AnalystBuilder builder)  {
        String id = uuidGenerator.generate(builder.getUrl()).toString();
        byte[] doc = Json.serialize(builder);
        return elastic.index(client.prepareIndex(getIndex(), "analyst", id)
                .setSource(doc)
                .setOpType(IndexRequest.OpType.INDEX));
    }

    @Override
    public void update(String id, AnalystUpdateBuilder builder) {
        byte[] doc = Json.serialize(builder);
        client.prepareUpdate(getIndex(), getType(), id)
                .setDoc(doc)
                .setRefresh(true)
                .get();
    }

    private static final JsonRowMapper<Analyst> MAPPER =
            (id, version, source) -> Json.deserialize(source, Analyst.class).setId(id);

    @Override
    public Analyst get(String id) {
        if (id.startsWith("http")) {
            return elastic.queryForObject(client.prepareSearch(getIndex())
                    .setTypes(getType())
                    .setQuery(QueryBuilders.termQuery("url", id)), MAPPER);
        }
        else {
            return elastic.queryForObject(id, MAPPER);
        }
    }

    @Override
    public long count() {
        return elastic.count(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setQuery(QueryBuilders.matchAllQuery()));
    }

    @Override
    public PagedList<Analyst> getAll(Paging page) {
        return elastic.page(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(page.getSize())
                .setFrom(page.getFrom())
                .setQuery(QueryBuilders.matchAllQuery()), page, MAPPER);
    }

    @Override
    public List<Analyst> getActive(Paging paging) {
        QueryBuilder query =
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal()));

        return elastic.query(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(paging.getSize())
                .setFrom(paging.getFrom())
                .addSort("queueSize", SortOrder.ASC)
                .setQuery(query), MAPPER);
    }

    @Override
    public List<Analyst> getActive(Paging paging, int maxQueueSize) {
        QueryBuilder query =
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("state", AnalystState.UP.ordinal()))
                        .must(QueryBuilders.rangeQuery("queueSize").lt(maxQueueSize));

        return elastic.query(client.prepareSearch(getIndex())
                .setTypes(getType())
                .setSize(paging.getSize())
                .setFrom(paging.getFrom())
                .addSort("queueSize", SortOrder.ASC)
                .setQuery(query), MAPPER);
    }
}
