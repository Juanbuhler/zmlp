package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.sdk.domain.*;

import java.util.List;

public interface IngestDao {

    Ingest get(int id);

    Ingest create(IngestPipeline pipeline, IngestBuilder builder);

    List<Ingest> getAll();

    List<Ingest> getAll(IngestState state, int limit);

    List<Ingest> getAll(IngestFilter filter);

    boolean update(Ingest ingest, IngestUpdateBuilder builder);

    boolean delete(Ingest ingest);

    boolean setState(Ingest ingest, IngestState newState, IngestState oldState);

    boolean setState(Ingest ingest, IngestState newState);

    void resetCounters(Ingest ingest);

    void incrementCounters(Ingest ingest, int created, int updated, int errors, int warnings);

    boolean updateStartTime(Ingest ingest, long time);

    boolean updateStoppedTime(Ingest ingest, long time);

    List<Ingest> getAll(IngestSchedule schedule);
}
