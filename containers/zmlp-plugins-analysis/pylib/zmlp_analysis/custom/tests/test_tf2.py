import os
import shutil
from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.custom.tf2 import TensorflowImageClassifier
from zmlpsdk.base import Frame
from zmlpsdk.storage import file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


class KerasModelImageClassifierTests(PluginUnitTestCase):
    model_id = "model-id-34567"
    base_dir = os.path.dirname(__file__)

    def setUp(self):
        try:
            shutil.rmtree("/tmp/model-cache/models_model-id-34567_foo_bar")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("zmlp_analysis.custom.tf2.get_proxy_level_path")
    def test_predict(self, proxy_patch, file_patch, model_patch):
        name = "disease_model"

        model_file = zorroa_test_path("training/{}.zip".format(name))
        file_patch.return_value = model_file
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TF2_IMAGE_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )

        args = {
            "model_id": self.model_id,
            "input_size": (321, 321)
        }

        flower_paths = [
            zorroa_test_path("training/test_dsy.jpg"),
            zorroa_test_path("training/test_rose.png")
        ]
        for paths in flower_paths:
            proxy_patch.return_value = paths
            frame = Frame(TestAsset(paths))

            processor = self.init_processor(
                TensorflowImageClassifier(), args
            )
            processor.process(frame)
            analysis = frame.asset.get_analysis(name)
            assert 'Cabbage Healthy' in get_prediction_labels(analysis)
            assert analysis['count'] >= 4
            assert 'labels' == analysis['type']