import tempfile
import os
import shutil

from unittest.mock import patch
from zmlp_core.util import media
from zmlp.analysis.testing import zorroa_test_data

IMAGE_EXR = zorroa_test_data('images/set06/SquaresSwirls.exr', False)
IMAGE_PSD = zorroa_test_data('images/set06/psd_123.psd', False)
IMAGE_JPG = zorroa_test_data('images/set02/beer_kettle_01.jpg', False)
VIDEO_WEBM = zorroa_test_data('video/dc.webm', False)
VIDEO_MP4 = zorroa_test_data('video/FatManOnABike1914.mp4', False)
VIDEO_MXF = zorroa_test_data('mxf/freeMXF-mxf1.mxf', False)
VIDEO_MOV = zorroa_test_data('video/1324_CAPS_23.0_030.00_15_MISC.mov', False)


def test_get_image_metadata():
    metadata = media.get_image_metadata(IMAGE_JPG)
    assert metadata["width"] == "3264"
    assert metadata["height"] == "2448"


def test_get_video_metadata():
    metadata = media.get_video_metadata(VIDEO_MP4)
    assert 25.0 == metadata['frameRate']
    assert 3611 == metadata['frames']
    assert 450 == metadata['width']
    assert 360 == metadata['height']
    assert 144.45 == metadata['length']


@patch('zmlp_core.util.media.check_output')
def test_get_image_metadata_invalid_chars(check_out_patch):

    xml = """
    <ImageSpec version="20">
    <attrib name="oiio:ColorSpace" type="string">sRGB</attrib>
    <attrib name="jpeg:subsampling" type="string">4:4:4</attrib>
    <attrib name="IPTC:Caption" type="string">&#05;4.2.7</attrib>
    <attrib name="ImageDescription" type="string">&#05;4.2.7</attrib>
    <attrib name="XResolution" type="float">75</attrib>
    <attrib name="YResolution" type="float">75</attrib>
    <attrib name="ResolutionUnit" type="string">in</attrib>
    </ImageSpec>
    """

    check_out_patch.return_value = xml
    metadata = media.get_image_metadata(IMAGE_JPG)
    assert metadata["IPTC"]["Caption"] == "4.2.7"
    assert metadata["ImageDescription"] == "4.2.7"


def test_media_size_video():
    size = media.media_size(VIDEO_WEBM)
    assert size[0] == 1920
    assert size[1] == 1080

    size = media.media_size(VIDEO_MP4)
    assert size[0] == 450
    assert size[1] == 360

    size = media.media_size(VIDEO_MXF)
    assert size[0] == 720
    assert size[1] == 576


def test_media_size_image():
    size = media.media_size(IMAGE_JPG)
    assert size[0] == 3264
    assert size[1] == 2448

    size = media.media_size(IMAGE_PSD)
    assert size[0] == 257
    assert size[1] == 126

    size = media.media_size(IMAGE_EXR)
    assert size[0] == 1000
    assert size[1] == 1000


def test_media_size_image_with_wildcard():
    tmp_file = tempfile.gettempdir() + "/lgts_bty.#.jpg"
    shutil.copy(IMAGE_JPG, tmp_file)
    size = media.media_size(tmp_file)
    assert size[0] == 3264
    assert size[1] == 2448


def test_get_output_dimension():
    # Test width being the longest edge.
    width, height = media.get_output_dimension(256, 512, 341)
    assert width == 256
    assert height == 170

    width, height = media.get_output_dimension(256, 341, 512)
    assert width == 170
    assert height == 256


def test_create_video_thumbnail():
    dst = tempfile.gettempdir() + "/something.jpg"
    media.create_video_thumbnail(VIDEO_MP4, tempfile.gettempdir() + "/something.jpg", 1)
    assert os.path.exists(dst)
    size = media.media_size(dst)
    assert size[0] == 450
    assert size[1] == 360


def test_create_single_frame_video_thumbnail():
    dst = tempfile.gettempdir() + "/something.jpg"
    media.create_video_thumbnail(VIDEO_MOV, tempfile.gettempdir() + "/something.jpg", 0)
    assert os.path.exists(dst)
    size = media.media_size(dst)
    assert size[0] == 1024
    assert size[1] == 767


def test_ffprobe_mp4():
    probe = media.ffprobe(VIDEO_MP4)
    assert len(probe["streams"]) == 2
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuv420p"
    assert probe["format"]["duration"] == "144.450000"
    assert probe["format"]["size"] == "13168719"


def test_ffprobe_mxf():
    probe = media.ffprobe(VIDEO_MXF)
    assert len(probe["streams"]) == 1
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuv420p"
    assert probe["format"]["duration"] == "10.720000"
    assert probe["format"]["size"] == "2815388"


def test_ffprobe_webm():
    probe = media.ffprobe(VIDEO_WEBM)
    assert len(probe["streams"]) == 1
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuv420p"
    assert probe["format"]["duration"] == "11.466000"
    assert probe["format"]["size"] == "8437109"


def test_ffprobe_mov():
    probe = media.ffprobe(VIDEO_MOV)
    assert len(probe["streams"]) == 1
    stream0 = probe["streams"][0]
    assert stream0["pix_fmt"] == "yuvj420p"
    assert probe["format"]["duration"] == "0.041667"
    assert probe["format"]["size"] == "73981"
