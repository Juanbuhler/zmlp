package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.BulkAssetUpsertResult;
import com.zorroa.archivist.sdk.domain.*;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.update.UpdateResponse;

import java.util.List;

public interface AssetDao {

    Asset upsert(AssetBuilder builder);

    String upsertAsync(AssetBuilder builder);

    String upsertAsync(AssetBuilder builder, ActionListener<UpdateResponse> listener);

    Asset get(String id);

    List<Asset> getAll();

    boolean existsByPath(String path);

    Asset getByPath(String path);

    boolean existsByPathAfter(String path, long afterTime);

    int addToFolder(Folder folder, List<String> assetIds);

    int removeFromFolder(Folder folder, List<String> assetIds);

    long update(String assetId, AssetUpdateBuilder builder);

    BulkAssetUpsertResult bulkUpsert(List<AssetBuilder> builders);

    void addToExport(Asset asset, Export export);

    void refresh();
}
