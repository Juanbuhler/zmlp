package examples.assets;

import com.zorroa.zmlp.client.app.AssetApp;
import com.zorroa.zmlp.client.domain.asset.Asset;

public class GetAssetById extends AssetBase {

    public static void main(String[] args) {

        AssetApp assetApp = createAssetApp();

        // Get Asset by it's Id
        Asset asset = assetApp.getById("dd0KZtqyec48n1q1ffogVMV5yzthRRGx");

    }
}
