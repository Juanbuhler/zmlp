from unittest.mock import patch

import pytest

from zmlp_core.core.importers import FileImportProcessor
from zmlp_core.office.importers import OfficerClient, OfficeImporter
from zmlpsdk import Frame, ZmlpFatalProcessorException
from zmlpsdk.testing import PluginUnitTestCase, TestAsset, zorroa_test_data


class FileImportProcessorTests(PluginUnitTestCase):

    def test_process_image(self):
        frame = Frame(TestAsset(zorroa_test_data("images/set01/toucan.jpg")))
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

    def test_process_video(self):
        frame = Frame(TestAsset(zorroa_test_data('video/sample_ipad.m4v')))
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

    def test_process_gps_point(self):
        frame = Frame(TestAsset(zorroa_test_data('video/sample_ipad.m4v')))
        frame.asset.set_attr("location.point.lat", -37.81)
        frame.asset.set_attr("location.point.lon", 144.96)
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

        assert -37.81 == frame.asset.get_attr('location.point.lat')
        assert 144.96 == frame.asset.get_attr('location.point.lon')
        assert 'Melbourne' == frame.asset.get_attr('location.city')
        assert 'AU' == frame.asset.get_attr('location.code')
        assert 'Australia' == frame.asset.get_attr('location.country')

    @patch.object(OfficeImporter, 'get_metadata',
                  return_value={'author': 'Zach', 'content': 'temp'})
    @patch.object(OfficerClient, 'render', return_value='/fake')
    @patch.object(OfficerClient, 'get_cache_location', return_value=None)
    def test_process_document(self, _, __, ___):
        frame = Frame(TestAsset(zorroa_test_data("office/minimal.pdf")))
        ih = self.init_processor(FileImportProcessor())
        ih.process(frame)

    def test_process_unsupported(self):
        frame = Frame(TestAsset(zorroa_test_data("offie/minimal.rar")))
        ih = self.init_processor(FileImportProcessor())
        with pytest.raises(ZmlpFatalProcessorException):
            ih.process(frame)
