package com.zorroa.archivist.service;

import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
import com.zorroa.sdk.processor.Source;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface AssetService {

    Asset index(Source source);

    Asset get(String id);

    Asset get(Path path);

    /**
     * Fetch the first page of assets.
     *
     * @return
     */
    PagedList<Asset> getAll(Paging page);

    AssetIndexResult index(String index, List<Source> sources);

    AssetIndexResult index(List<Source> sources);

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
}
