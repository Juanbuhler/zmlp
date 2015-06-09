package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;

public interface RoomDao {

    Room create(RoomBuilder builder);

    Room get(long id);

    String getPassword(long id);
}
