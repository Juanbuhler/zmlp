# flake8: noqa
import os
from unittest.mock import patch
import pytest

from zmlp_analysis.clarifai.video.colors import ClarifaiVideoColorDetectionProcessor
from zmlpsdk import Frame
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, get_prediction_labels


@pytest.mark.skip(reason='dont run automatically')
class ClarifaiColorDetectionPublicModelsProcessorIntegrationTests(PluginUnitTestCase):

    def setUp(self):
        cred_location = os.path.join(os.path.dirname(__file__), '..', 'clarifai-creds')
        with open(cred_location, 'rb') as f:
            key = f.read().decode()
        os.environ['CLARIFAI_KEY'] = key

    def tearDown(self):
        del os.environ['CLARIFAI_KEY']

    @patch("zmlp_analysis.clarifai.video.colors.video.save_timeline", return_value={})
    @patch('zmlp_analysis.clarifai.video.colors.proxy.get_video_proxy')
    def run_process(self, proxy_path_patch, _, video_path, detector, attr, assertions):
        frame = Frame(TestAsset(video_path))
        proxy_path_patch.return_value = video_path

        processor = self.init_processor(detector)
        asset = TestAsset(video_path)
        asset.set_attr('media.length', 15.0)
        frame = Frame(asset)

        processor.process(frame)

        analysis = frame.asset.get_analysis(attr)
        predictions = get_prediction_labels(analysis)
        for label in assertions['labels']:
            assert label in predictions
        assert 'labels' in analysis['type']
        assert assertions['count'] == analysis['count']

    def test_color_process(self):
        self.run_process(
            video_path=zorroa_test_path('images/set05/color_test.png'),
            detector=ClarifaiVideoColorDetectionProcessor(),
            attr='clarifai-video-color-model',
            assertions={'labels': ['Yellow', 'OrangeRed'], 'count': 4}
        )
