import os
import tempfile
import unittest

import zmlpsdk.video as video
from zmlpsdk.testing import zorroa_test_path

VIDEO_M4V = zorroa_test_path('video/sample_ipad.m4v')
VIDEO_MOV = zorroa_test_path('video/1324_CAPS_23.0_030.00_15_MISC.mov')


def test_extract_thumbnail_from_video():
    dst = tempfile.gettempdir() + '/something.jpg'
    video.extract_thumbnail_from_video(VIDEO_M4V, tempfile.gettempdir() + '/something.jpg', 1)
    assert os.path.exists(dst)


def test_extract_thumbnail_from_video_single():
    dst = tempfile.gettempdir() + "/something.jpg"
    video.extract_thumbnail_from_video(VIDEO_MOV, tempfile.gettempdir() + "/something.jpg", 0)
    assert os.path.exists(dst)


def test_webvtt_builder():
    with video.WebvttBuilder() as vtt:
        vtt.append(0, 10, "hello, you bastard")
    data = open(vtt.path, "r").read()
    assert "00:00:00.000 --> 00:00:10.000" in data
    assert "hello, you bastard" in data


class FrameExtractors(unittest.TestCase):
    """
    Test the different frame extractors
    """
    def test_time_based_iterate(self):
        iter = video.TimeBasedFrameExtractor(VIDEO_M4V)
        frames = list(iter)
        assert 17 == len(frames)
        assert os.path.exists(frames[0][1])

    def test_shot_based_iterate(self):
        iter = video.ShotBasedFrameExtractor(VIDEO_M4V)
        frames = list(iter)
        assert 4 >= len(frames)
        assert os.path.exists(frames[0][1])
