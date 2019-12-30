from pathlib2 import Path

from zmlp_core.video.importers import VideoImporter
from zmlp.analysis import Frame, ZmlpProcessorException
from zmlp.analysis.testing import TestAsset, PluginUnitTestCase, zorroa_test_data


class VideoImporterUnitTestCase(PluginUnitTestCase):

    def setUp(self):
        self.movie_path = zorroa_test_data('video/sample_ipad.m4v')
        self.frame = Frame(TestAsset(self.movie_path))
        self.processor = self.init_processor(VideoImporter(), {})

    def test_set_media_metadata(self):
        asset = self.frame.asset
        self.processor._set_media_metadata(asset)
        expected_media = {'description': u'A short description of luke sledding in winter.',
                          'title': u'Luke crashes sled', 'length': 15.048367,
                          'height': 360, 'width': 640,
                          'orientation': 'landscape',
                          'aspect': 1.78}
        assert asset.get_attr('media') == expected_media

    def test_create_proxy_source_image(self):
        asset = self.frame.asset
        asset.set_attr('clip', {'start': 0, 'stop': 10})
        self.processor._create_proxy_source_image(asset)
        assert Path(asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'

    def test_create_proxy_source_failure(self):
        path = zorroa_test_data('office/simple.pdf')
        asset = Frame(TestAsset(path)).asset
        self.assertRaises(ZmlpProcessorException, self.processor._create_proxy_source_image, asset)

    def test_process(self):
        self.processor.process(self.frame)
        print(self.frame.asset.document)
        # Verify proxy source is created.
        assert Path(self.frame.asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'

    def test_single_frame_movie(self):
        movie_path = zorroa_test_data('video/1324_CAPS_23.0_030.00_15_MISC.mov')
        frame = Frame(TestAsset(movie_path))
        self.processor.process(frame)

        path = Path(frame.asset.get_attr('tmp.proxy_source_image'))
        assert path.exists()
        assert path.suffix == '.jpg'

    def test_process_mxf(self):
        movie_path = zorroa_test_data('mxf/freeMXF-mxf1.mxf')
        asset = TestAsset(movie_path)
        frame = Frame(asset)
        self.processor.process(frame)

        assert 'video' == asset.get_attr('media.type')
        assert 'mxf' == asset.get_attr('source.extension')
        assert 720 == asset.get_attr('media.width')
        assert 576 == asset.get_attr('media.height')
        assert 10.72 == asset.get_attr('media.length')
        assert 1.25 == asset.get_attr('media.aspect')

        # Verify proxy source is created.
        assert Path(asset.get_attr('tmp.proxy_source_image')).suffix == '.jpg'
