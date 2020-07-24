from unittest.mock import patch

from pytest import approx

from zmlp_analysis.aws import RekognitionLabelDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, \
    zorroa_test_path, get_prediction_labels


class RekognitionLabelDetectionProcessorTests(PluginUnitTestCase):
    model_id = "model-id-12345"

    @patch("zmlp_analysis.aws.labels.get_proxy_level_path")
    @patch('zmlp_analysis.aws.labels.get_zvi_rekognition_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        flower_paths = zorroa_test_path("training/test_dsy.jpg")
        proxy_patch.return_value = flower_paths
        frame = Frame(TestAsset(flower_paths))

        args = expected_results[0][0]

        processor = self.init_processor(RekognitionLabelDetection(), args)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-label-detection')
        assert 'Plant' in get_prediction_labels(analysis)
        assert 'Daisy' in get_prediction_labels(analysis)
        assert analysis['count'] == 2


expected_results = [
    (
        {"model_id": "model-id-12345"},
        [
            ('Plant', approx(99.90, 0.01)),
            ('Daisy', approx(99.59, 0.01))
        ]
    )
]


class MockAWSClient:

    def detect_labels(self, Image=None, MaxLabels=3):
        return {
            'Labels': [
                {
                    'Name': 'Plant',
                    'Confidence': 99.90
                },
                {
                    'Name': 'Daisy',
                    'Confidence': 99.59
                }
            ]
        }
