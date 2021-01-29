import logging
import os
import shutil
from unittest.mock import patch

from zmlp.app import ModelApp
from zmlp.entity import Model
from zmlp_analysis.custom.video import VideoPytorchTransferLearningClassifier
from zmlpsdk.base import Frame
from zmlpsdk.storage import file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels

logging.basicConfig()


class VideoTensorflowTransferLearningClassifierTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    base_dir = os.path.dirname(__file__)
    name = "pytorch_ants_and_bees"

    def setUp(self):
        try:
            shutil.rmtree("/tmp/model-cache/models_model-id-34568_foo_bar")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

        self.video_path = zorroa_test_path('video/flower.mp4')
        asset = TestAsset(self.video_path)
        asset.set_attr('media.length', 15.0)
        self.frame = Frame(asset)

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("zmlp_analysis.custom.video.pytorch.video.save_timeline", return_value={})
    @patch('zmlp_analysis.custom.video.pytorch.proxy.get_video_proxy')
    def test_predict(self, proxy_path_patch, _, file_patch, model_patch):
        proxy_path_patch.return_value = self.video_path
        model_file = zorroa_test_path("training/{}.zip".format(self.name))
        file_patch.return_value = model_file
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "PYTORCH_IMAGE_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )

        args = {
            "model_id": self.model_id,
            "input_size": (321, 321)
        }

        processor = self.init_processor(
            VideoPytorchTransferLearningClassifier(), args
        )
        processor.process(self.frame)

        analysis = self.frame.asset.get_analysis(self.name)
        predictions = get_prediction_labels(analysis)
        assert 'labels' in analysis['type']
        assert 1 == analysis['count']
        assert 'bees' in predictions
