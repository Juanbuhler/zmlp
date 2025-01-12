from unittest.mock import patch

import boonsdk
from boonai_analysis.boonai import TimelineAnalysisProcessor, ClipAnalysisProcessor, \
    MultipleTimelineAnalysisProcessor
from boonai_analysis.boonai.clips import analyze_timelines
from boonai_analysis.utils import simengine
from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow import Frame
from boonflow import file_storage
from boonflow.testing import PluginUnitTestCase, test_path, TestAsset
from boonsdk import VideoClip
from boonsdk.app import AssetApp, VideoClipApp
from boonsdk.client import BoonClient


class TestClipAnalysisProcessors(PluginUnitTestCase):

    @patch.object(BoonClient, 'put')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    @patch.object(VideoClipApp, 'get_clip')
    def test_single_clip(self, get_clip_patch, get_asset_patch, local_patch,
                         store_file_patch, store_clips_patch, put_patch):

        get_clip_patch.return_value = VideoClip(
            {'id': '123456', 'start': 1.5, 'assetId': 'abc1234'})
        asset = TestAsset('12345')
        SimilarityEngine.default_model_path = test_path('models/resnet-152')
        get_asset_patch.return_value = TestAsset('12345')
        local_patch.return_value = test_path('video/ted_talk.mp4')
        store_file_patch.return_value = {'id': 'jonsnow'}
        put_patch.return_value = {}
        processor = self.init_processor(ClipAnalysisProcessor(), {})
        processor.process(Frame(asset))

        args, kwargs = put_patch.call_args
        assert args[1]['simhash'].startswith('OKOPPPNPNPPJPPPPPPI')

    @patch('boonai_analysis.boonai.clips.submit_clip_batch')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    def test_single_timeline(self, get_asset_patch, local_patch,
                             search_patch, store_file_patch, store_clips_patch):
        asset = TestAsset('12345')
        clip = VideoClip({'id': '56789', 'start': 1.5})
        SimilarityEngine.default_model_path = test_path('models/resnet-152')
        get_asset_patch.return_value = TestAsset('12345')
        local_patch.return_value = test_path('video/ted_talk.mp4')
        search_patch.return_value = [clip]
        store_file_patch.return_value = {'id': 'jonsnow'}

        processor = self.init_processor(TimelineAnalysisProcessor(), {})
        processor.process(Frame(asset))

        args, kwargs = store_clips_patch.call_args
        assert args[2]['56789']['files'][0]['id'] == 'jonsnow'
        assert args[2]['56789']['simhash'].startswith('OKOPPPN')

    @patch('boonai_analysis.boonai.clips.submit_clip_batch')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    def test_multiple_timeline(self, get_asset_patch, local_patch,
                               search_patch, store_file_patch, store_clips_patch):
        asset = TestAsset('12345')
        clip = VideoClip({'id': '56789', 'start': 1.5})
        SimilarityEngine.default_model_path = test_path('models/resnet-152')
        get_asset_patch.return_value = TestAsset('12345')
        local_patch.return_value = test_path('video/ted_talk.mp4')
        search_patch.return_value = [clip]
        store_file_patch.return_value = {'id': 'jonsnow'}

        processor = self.init_processor(MultipleTimelineAnalysisProcessor(), {
            'timelines': {'12345': ['test-timeline']}
        })
        processor.process(Frame(asset))

        args, kwargs = store_clips_patch.call_args
        assert args[2]['56789']['files'][0]['id'] == 'jonsnow'
        assert args[2]['56789']['simhash'].startswith('OKOPPPN')

    @patch('boonai_analysis.boonai.clips.submit_clip_batch')
    @patch.object(file_storage.projects, 'store_file')
    @patch.object(VideoClipApp, 'scroll_search')
    @patch.object(file_storage, 'localize_file')
    @patch.object(AssetApp, 'get_asset')
    def test_analyze_timelines_with_bbox(self, get_asset_patch, local_patch,
                                         search_patch, store_file_patch, store_clips_patch):
        asset = TestAsset('12345')
        clip = VideoClip({'id': '56789', 'start': 1.5, 'bbox': [0.1, 0.1, 0.5, 0.5]})
        SimilarityEngine.default_model_path = test_path('models/resnet-152')
        get_asset_patch.return_value = TestAsset('12345')
        local_patch.return_value = test_path('video/ted_talk.mp4')
        search_patch.return_value = [clip]
        store_file_patch.return_value = {'id': 'jonsnow'}

        analyze_timelines(boonsdk.app_from_env(), simengine.SimilarityEngine(), asset.id, ["foo"])

        args, kwargs = store_clips_patch.call_args
        assert args[2]['56789']['files'][0]['id'] == 'jonsnow'
        assert args[2]['56789']['simhash'].startswith('OKOPPPN')

        args, kwargs = store_file_patch.call_args
        assert kwargs['attrs']['bbox'] == [76, 43, 384, 215]
