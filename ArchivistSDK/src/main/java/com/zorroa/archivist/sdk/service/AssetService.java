package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.AssetUpdateBuilder;

public interface AssetService {

    Asset upsert(AssetBuilder builder);

    String upsertAsync(AssetBuilder builder);

    Asset get(String id);

    boolean assetExistsByPath(String path);

    boolean assetExistsByPathAfter(String path, long afterTime);


    /**
     * Update the given assetId with the supplied AssetUpdateBuilder.  Return
     * the new version number of the asset.
     *
     * @param assetId
     * @param builder
     * @return
     */
    long update(String assetId, AssetUpdateBuilder builder);
}
