package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.Acl;
import com.zorroa.sdk.domain.Folder;
import com.zorroa.sdk.domain.FolderBuilder;
import com.zorroa.sdk.domain.FolderUpdateBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FolderService {

    Folder get(int id);

    Folder get(int parent, String name);

    boolean exists(String path);

    List<Folder> getAll();

    /**
     * Return all folders with the given unique IDs.
     *
     * @param ids
     * @return
     */
    List<Folder> getAll(Collection<Integer> ids);

    List<Folder> getChildren(Folder folder);

    /**
     * Return a Set of all descendant folders starting from the given start folders.  If
     * includeStartFolders is set to true, the starting folders are included in the result.
     *
     * @param startFolders
     * @param includeStartFolders
     * @return
     */
    Set<Folder> getAllDescendants(Collection<Folder> startFolders, boolean includeStartFolders);

    /**
     * Return a recursive list of all descendant folders from the current folder.
     *
     * @param folder
     * @return
     */
    Set<Folder> getAllDescendants(Folder folder);

    Folder create(FolderBuilder builder);

    boolean update(Folder folder, FolderUpdateBuilder builder);

    boolean delete(Folder folder);

    Folder get(String path);

    void setAcl(Folder folder, Acl acl);

    void addAssets(Folder folder, List<String> assetIds);

    void removeAssets(Folder folder, List<String> assetIds);
}
