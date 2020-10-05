# flake8: noqa
from unittest.mock import patch

from zmlp_analysis.azure import *
from zmlpsdk.base import Frame
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_path, get_prediction_labels

patch_path = 'zmlp_analysis.azure.util.ComputerVisionClient'
cred_path = 'zmlp_analysis.azure.util.CognitiveServicesCredentials'

DOGBIKE = zorroa_test_path('images/detect/dogbike.jpg')
STREETSIGN = zorroa_test_path("images/set09/streetsign.jpg")
RYAN_GOSLING = zorroa_test_path('images/set08/meme.jpg')
EIFFEL_TOWER = zorroa_test_path('images/set11/eiffel_tower.jpg')
LOGOS = zorroa_test_path('images/set11/logos.jpg')
NSFW = zorroa_test_path('images/set10/nsfw1.jpg')


class MockCognitiveServicesCredentials:

    def __init__(self, subscription_key=None):
        pass


class MockACVClient:

    def __init__(self, endpoint=None, credentials=None):
        pass

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


class AzureObjectDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-object-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionObjectDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'dog' in get_prediction_labels(analysis)


class AzureLabelDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-label-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionLabelDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'bicycle' in get_prediction_labels(analysis)


class AzureImageDescriptionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-image-description-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self,  p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionImageDescription())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        description = 'a dog sitting in front of a mirror posing for the camera'
        assert description in get_prediction_labels(analysis)


class AzureTagDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-tag-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionImageTagsDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'bicycle' in get_prediction_labels(analysis)


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


class AzureLandmarkDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-landmark-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = EIFFEL_TOWER
        frame = Frame(TestAsset(EIFFEL_TOWER))

        processor = self.init_processor(ComputerVisionLandmarkDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Eiffel Tower' in get_prediction_labels(analysis)


class AzureLogoDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-logo-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = LOGOS
        frame = Frame(TestAsset(LOGOS))

        processor = self.init_processor(ComputerVisionLogoDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'Shell' in get_prediction_labels(analysis)


class AzureCategoryDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-category-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionCategoryDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'indoor_' in get_prediction_labels(analysis)


class AzureExplicitContentDetectionProcessorTests(PluginUnitTestCase):
    namespace = 'azure-explicit-detection'

    @patch("zmlp_analysis.azure.vision.get_proxy_level_path")
    @patch(cred_path, side_effect=MockCognitiveServicesCredentials)
    @patch(patch_path, side_effect=MockACVClient)
    def test_predict(self, p_path, c_path, proxy_patch):
        proxy_patch.return_value = DOGBIKE
        frame = Frame(TestAsset(DOGBIKE))

        processor = self.init_processor(ComputerVisionExplicitContentDetection())
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.namespace)
        assert 'racy' in get_prediction_labels(analysis)


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

    @property
    def rectangle(self):
        return MockBoundingBox()


class MockImageAnalysis:

    @property
    def tags(self):
        return [MockTags()]

    @property
    def brands(self):
        return [MockBrands()]

    @property
    def categories(self):
        return [MockCategories()]

    @property
    def adult(self):
        return MockExplicit()

    @property
    def result(self):
        return {
            'celebrities': [{
                'name': 'Ryan Gosling',
                'confidence': 0.995,
                'faceRectangle': {
                    'left': 118,
                    'top': 159,
                    'width': 94,
                    'height': 94
                }
            }],
            'landmarks': [{
                'name': 'Eiffel Tower',
                'confidence': 0.998
            }]
        }


class MockTags:

    @property
    def name(self):
        return 'bicycle'

    @property
    def confidence(self):
        return 0.776


class MockBrands:

    @property
    def name(self):
        return 'Shell'

    @property
    def confidence(self):
        return 0.935

    @property
    def rectangle(self):
        return MockBoundingBox()


class MockBoundingBox:

    @property
    def x(self):
        return 0

    @property
    def y(self):
        return 0

    @property
    def w(self):
        return 1

    @property
    def h(self):
        return 1


class MockCategories:

    @property
    def name(self):
        return 'indoor_'

    @property
    def score(self):
        return 0.935


class MockExplicit:

    @property
    def adult_score(self):
        return 0.935

    @property
    def racy_score(self):
        return 0.935

    @property
    def gore_score(self):
        return 0.935

    @property
    def is_racy_content(self):
        return True if self.racy_score() >= 0.50 else False
