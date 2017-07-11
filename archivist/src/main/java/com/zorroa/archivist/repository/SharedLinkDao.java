package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.SharedLink;
import com.zorroa.archivist.domain.SharedLinkSpec;

/**
 * Created by chambers on 7/7/17.
 */
public interface SharedLinkDao {

    SharedLink create(SharedLinkSpec spec);

    SharedLink get(int id);

    int deleteExpired(long olderThan);
}
