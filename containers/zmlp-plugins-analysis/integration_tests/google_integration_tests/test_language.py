import os
import pytest

from zmlp_analysis.google_cloud.cloud_lang import CloudNaturalLanguageProcessor

from zmlpsdk.testing import TestAsset, PluginUnitTestCase
from zmlpsdk.base import Frame


# NOTE: These test require you have a service account key located at
# ~/zorroa/keys/gcloud-integration-test.json.
@pytest.mark.skip(reason='dont run automaticallly')
class CloudNaturalLanguageProcessorTestCase(PluginUnitTestCase):
    def setUp(self):
        os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = os.path.dirname(__file__) + '/gcp-creds.json'
        self.processor = self.init_processor(CloudNaturalLanguageProcessor())

    def tearDown(self):
        del os.environ["GOOGLE_APPLICATION_CREDENTIALS"]

    def test_video_labels(self):
        asset = TestAsset()
        asset.set_attr('media.dialog', "Big Boi cooler than a polar bear's toenails")
        frame = Frame(asset)
        self.processor.process(frame)
        assert 'Big Boi' in asset.get_attr('analysis.google.languageEntities.keywords')
