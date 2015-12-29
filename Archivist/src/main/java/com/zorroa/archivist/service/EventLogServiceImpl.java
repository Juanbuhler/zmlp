package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.EventLogDao;
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.EventLogMessage;
import com.zorroa.archivist.sdk.domain.Id;
import com.zorroa.archivist.sdk.service.EventLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by chambers on 12/29/15.
 */
@Service
public class EventLogServiceImpl implements EventLogService {

    @Autowired
    EventLogDao eventLogDao;

    @Override
    public void log(EventLogMessage logMessageBuilder) {
        eventLogDao.log(logMessageBuilder);
    }

    @Override
    public void log(Id id, String s, Object... objects) {
        eventLogDao.log(id, s, objects);
    }

    @Override
    public void log(String s, Object... objects) {
        eventLogDao.log(s, objects);
    }

    @Override
    public void log(Id id, String s, Throwable throwable, Object... objects) {
        eventLogDao.log(id, s, throwable, objects);
    }

    @Override
    public void log(Asset asset, String s, Object... objects) {
        eventLogDao.log(asset, s, objects);
    }

    @Override
    public void log(Asset asset, String s, Throwable throwable, Object... objects) {
        eventLogDao.log(asset, s, throwable, objects);
    }
}
