package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.PermissionBuilder;
import com.zorroa.archivist.sdk.domain.User;

import java.util.Collection;
import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
public interface PermissionDao {
    Permission create(PermissionBuilder builder);

    Permission update(Permission permission);

    Permission get(int id);

    Permission get(String name);

    List<Permission> getAll();

    List<Permission> getAll(User user);

    void setPermissions(User user, Collection<? extends Permission> perms);

    void setPermissions(User user, Permission... perms);
}
