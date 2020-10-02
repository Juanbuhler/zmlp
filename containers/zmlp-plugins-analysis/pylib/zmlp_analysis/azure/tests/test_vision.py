import pytest
from unittest.mock import patch

from zmlp_analysis.azure.vision import (
    ComputerVisionObjectDetection,
    ComputerVisionLabelDetection,
    ComputerVisionImageDescription,
    ComputerVisionImageTagsDetection,
    ComputerVisionCelebrityDetection,
    ComputerVisionLandmarkDetection,
)
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels


DOGBIKE = zorroa_test_path('images/detect/dogbike.jpg')
STREETSIGN = zorroa_test_path("images/set09/streetsign.jpg")
RYAN_GOSLING = zorroa_test_path('images/set08/meme.jpg')
EIFFEL_TOWER = zorroa_test_path('images/set08/eiffel_tower.jpg')


@pytest.mark.skip(reason='dont run automatically')
class AzureObjectDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-object-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionObjectDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'dog' in get_prediction_labels(analysis)


@pytest.mark.skip(reason='dont run automatically')
class AzureLabelDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-label-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionLabelDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'bicycle' in get_prediction_labels(analysis)


@pytest.mark.skip(reason='dont run automatically')
class AzureImageDescriptionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-image-description-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionImageDescription())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        description = 'a dog sitting in front of a mirror posing for the camera'
        assert description in get_prediction_labels(analysis)


@pytest.mark.skip(reason='dont run automatically')
class AzureTagDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-tag-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionImageTagsDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'bicycle' in get_prediction_labels(analysis)


@pytest.mark.skip(reason='dont run automatically')
class AzureCelebrityDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-celebrity-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = RYAN_GOSLING
        frame = Frame(TestAsset(RYAN_GOSLING))

        processor = self.init_processor(ComputerVisionCelebrityDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Ryan Gosling' in get_prediction_labels(analysis)


@pytest.mark.skip(reason='dont run automatically')
class AzureLandmarkDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-landmark-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch('zmlp_analysis.azure.vision.get_zvi_azure_cv_client')
    def test_predict(self, client_patch, proxy_patch):
        client_patch.return_value = MockACVClient()
        proxy_patch.return_value = EIFFEL_TOWER
        frame = Frame(TestAsset(EIFFEL_TOWER))

        processor = self.init_processor(ComputerVisionLandmarkDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Eiffel Tower' in get_prediction_labels(analysis)


class MockACVClient:

    def detect_objects_in_stream(self, image=None):
        return MockDetectResult()

    def analyze_image_in_stream(self, image=None, visual_features=None):
        return MockImageAnalysis()

    def describe_image_in_stream(self, image=None):
        return MockDetectResult()

    def tag_image_in_stream(self, image=None):
        return MockImageAnalysis()

    def analyze_image_by_domain_in_stream(self, model=None, image=None):
        return MockImageAnalysis()


class MockDetectResult:

    @property
    def objects(self):
        return [MockDetectedObjects()]

    @property
    def captions(self):
        return [MockDetectedObjects()]


class MockDetectedObjects:

    @property
    def object_property(self):
        return 'dog'

    @property
    def confidence(self):
        return '0.873'

    @property
    def text(self):
        return 'a dog sitting in front of a mirror posing for the camera'


class MockImageAnalysis:

    @property
    def tags(self):
        return [MockTags()]

    @property
    def result(self):
        return {
            'celebrities': [{
                'name': 'Ryan Gosling',
                'confidence': '0.995'
            }],
            'landmarks': [{
                'name': 'Eiffel Tower',
                'confidence': '0.998'
            }]
        }


class MockTags:

    @property
    def name(self):
        return 'bicycle'

    @property
    def confidence(self):
        return '0.776'
