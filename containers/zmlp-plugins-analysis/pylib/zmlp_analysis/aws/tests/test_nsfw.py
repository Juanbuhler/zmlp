from unittest.mock import patch

from pytest import approx

from zmlp_analysis.aws import RekognitionUnsafeDetection
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


class RekognitionUnsafeDetectionProcessorTests(PluginUnitTestCase):

    @patch("zmlp_analysis.aws.nsfw.get_proxy_level_path")
    @patch('zmlp_analysis.aws.nsfw.get_zvi_rekognition_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockAWSClient()

        img_path = zorroa_test_path("images/detect/dogbike.jpg")
        proxy_patch.return_value = img_path
        frame = Frame(TestAsset(img_path))

        args = expected_results[0][0]

        processor = self.init_processor(RekognitionUnsafeDetection(), args)
        processor.process(frame)

        analysis = frame.asset.get_analysis('aws-unsafe-detection')
        assert 'Suggestive' in get_prediction_labels(analysis)
        assert 'Male Swimwear Or Underwear' in get_prediction_labels(analysis)
        assert analysis['count'] == 2


expected_results = [
    (
        {"model_id": "model-id-12345"},
        [
            ('Suggestive', approx(0.6514, 0.0001)),
            ('Male Swimwear Or Underwear', approx(0.6514, 0.0001))
        ]
    )
]


class MockAWSClient:

    def detect_moderation_labels(self, Image=None, MinConfidence=60):
        return {
            'ModerationLabels': [
                {
                    'Name': 'Suggestive',
                    'Confidence': 65.14
                },
                {
                    'Name': 'Male Swimwear Or Underwear',
                    'Confidence': 65.14
                }
            ]
        }
