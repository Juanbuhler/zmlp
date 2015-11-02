package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.IngestPipeline;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.domain.IngestPipelineUpdateBuilder;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class IngestPipelineDaoImpl extends AbstractDao implements IngestPipelineDao {

    private static final RowMapper<IngestPipeline> MAPPER = (rs, row) -> {
        IngestPipeline result = new IngestPipeline();
        result.setId(rs.getInt("pk_pipeline"));
        result.setName(rs.getString("str_name"));
        result.setDescription(rs.getString("str_description"));
        result.setTimeCreated(rs.getLong("time_created"));
        result.setUserCreated(rs.getString("str_user_created"));
        result.setTimeModified(rs.getLong("time_modified"));
        result.setUserModified(rs.getString("str_user_modified"));
        result.setProcessors((List<ProcessorFactory<IngestProcessor>>) rs.getObject("list_processors"));
        return result;
    };

    private static final String INSERT =
            "INSERT INTO " +
                    "pipeline " +
            "(" +
                    "str_name,"+
                    "str_description,"+
                    "str_user_created,"+
                    "time_created,"+
                    "str_user_modified, "+
                    "time_modified, "+
                    "list_processors " +
            ") "+
            "VALUES (?,?,?,?,?,?,?)";

    @Override
    public IngestPipeline create(IngestPipelineBuilder builder) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_pipeline"});
            ps.setString(1, builder.getName());
            ps.setString(2, builder.getDescription());
            ps.setString(3, SecurityUtils.getUsername());
            ps.setLong(4, time);
            ps.setString(5, SecurityUtils.getUsername());
            ps.setLong(6, time);
            ps.setObject(7, builder.getProcessors());
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public IngestPipeline get(int id) {
        try {
            return jdbc.queryForObject("SELECT * FROM pipeline WHERE pk_pipeline=?", MAPPER, id);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to get pipeline: id=" + id, 1);
        }
    }

    @Override
    public List<IngestPipeline> getAll() {
        return jdbc.query("SELECT * FROM pipeline", MAPPER);
    }

    @Override
    public boolean update(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE pipeline SET ");

        List<String> updates = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        if (builder.getDescription() != null) {
            updates.add("str_description=?");
            values.add(builder.getDescription());
        }

        if (builder.getName() != null) {
            updates.add("str_name=?");
            values.add(builder.getName());
        }

        if (builder.getProcessors() != null) {
            updates.add("list_processors=?");
            values.add(builder.getProcessors());
        }

        if (updates.isEmpty()) {
            return false;
        }

        updates.add("str_user_modified=?");
        values.add(SecurityUtils.getUsername());

        updates.add("time_modified=?");
        values.add(System.currentTimeMillis());

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_pipeline=?");
        values.add(pipeline.getId());

        logger.debug("{} {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public boolean delete(IngestPipeline pipeline) {
        return jdbc.update("DELETE FROM pipeline WHERE pk_pipeline=?", pipeline.getId()) == 1;
    }
}
