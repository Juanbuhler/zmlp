package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Acl;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface AssetService {

    Asset get(String id);

    Asset get(Path path);

    /**
     * Fetch the first page of assets.
     *
     * @return
     */
    PagedList<Asset> getAll(Pager page);

    Asset index(Source source, LinkSpec link);
    Asset index(Source source);
    /**
     * Index the given list of sources, optionally attaching the given
     * source link to created assets.
     *
     * @param sources
     * @param link
     * @return
     */
    DocumentIndexResult index(List<Source> sources, LinkSpec link);
    DocumentIndexResult index(List<Source> sources);

    Map<String, List<Object>> removeLink(String type, String value, List<String> assets);
    Map<String, List<Object>> appendLink(String type, String value, List<String> assets);

    boolean exists(Path path);

    /**
     * Update the given assetId with the supplied Map of attributes.  Return
     * the new version number of the asset.
     *
     * @param id
     * @param attrs
     * @return
     */
    long update(String id, Map<String, Object> attrs);

    boolean delete(String id);

    void setPermissionsAsync(AssetSearch search, Acl acl);

    void setPermissions(AssetSearch search, Acl acl);

    Map<String, Object> getMapping();
}
