# flake8: noqa
import os
import uuid
from unittest.mock import patch

import pytest

from zmlp import ZmlpClient, StoredFile
from zmlp_analysis.google.cloud_vision import *
from zmlp_analysis.google.cloud_vision import file_storage
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.testing import PluginUnitTestCase, zorroa_test_path, TestAsset, \
    get_prediction_labels, get_mock_stored_file


@pytest.mark.skip(reason='dont run automaticallly')
class CloudVisionProcessorTestCase(PluginUnitTestCase):

    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'

    def tearDown(self):
        del os.environ['GOOGLE_APPLICATION_CREDENTIALS']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_image_text_processor(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set01/visa.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectImageText())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-image-text-detection')
        assert 'content' == analysis['type']
        assert 'Giants Franchise History' in analysis['content']
        assert 41 == analysis['count']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_document_text_processor(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectDocumentText())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-doc-text-detection')
        assert 'HEY GIRL' in analysis['content']
        assert 'content' in analysis['type']
        assert 12 == analysis['count']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_landmark_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/eiffel_tower.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectLandmarks())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-landmark-detection')
        assert 'landmarks' == analysis['type']
        assert 'Eiffel Tower' in get_prediction_labels(analysis)
        assert 'Champ de Mars' in get_prediction_labels(analysis)
        assert 2 == analysis['count']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_explicit_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()

        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectExplicit())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-content-moderation')
        assert 'moderation' == analysis['type']
        assert False is analysis['safe']
        assert 'spoof' in get_prediction_labels(analysis)
        assert 'violence' in get_prediction_labels(analysis)
        assert 3 == analysis['count']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_face_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        asset = TestAsset(path)
        frame = Frame(asset)
        processor = self.init_processor(CloudVisionDetectFaces())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-face-detection')
        assert 1 == analysis['count']
        assert 'faces' == analysis['type']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_logo_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set01/visa.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectLogos())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-logo-detection')
        assert 'Visa Inc.' in get_prediction_labels(analysis)
        assert 7 == analysis['count']
        assert 'objects' == analysis['type']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_label_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/set08/meme.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectLabels())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-label-detection')
        assert 'Hair' in get_prediction_labels(analysis)
        assert 10 == analysis['count']
        assert 'labels' == analysis['type']

    @patch('zmlp_analysis.google.cloud_vision.get_proxy_level')
    @patch.object(file_storage, 'localize_file')
    @patch.object(file_storage.assets, 'get_native_uri')
    def test_object_detection(self, native_patch, localize_patch, proxy_patch):
        path = zorroa_test_path('images/detect/dogbike.jpg')
        native_patch.return_value = path
        localize_patch.return_value = path
        proxy_patch.return_value = get_mock_stored_file()
        frame = Frame(TestAsset(path))
        processor = self.init_processor(CloudVisionDetectObjects())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.gcp-vision-object-detection')
        assert 'Dog' in get_prediction_labels(analysis)
        assert 5 == analysis['count']
        assert 'objects' == analysis['type']
