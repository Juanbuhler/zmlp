import logging
import os
import tempfile
import zipfile
from unittest import TestCase
from unittest.mock import patch

import pytest
from minio.api import Minio

import zmlp
from zmlp import StoredFile, ZmlpClient, PipelineMod, Job, Model
from zmlp.app import ModelApp
from zmlpsdk import storage, timeline
from zmlpsdk.testing import zorroa_test_data, TestAsset

logging.basicConfig(level=logging.DEBUG)


class FileCacheTests(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.lfc = storage.FileCache(zmlp.app_from_env())

    def tearDown(self):
        self.lfc.clear()

    def test_init_with_task_id(self):
        os.environ['ZMLP_TASK_ID'] = '1234abcd5678'
        try:
            cache = storage.FileCache(zmlp.app_from_env())
            path = cache.localize_uri('https://i.imgur.com/WkomVeG.jpg')
            assert os.environ['ZMLP_TASK_ID'] in path
        finally:
            del os.environ['ZMLP_TASK_ID']
            cache.close()

    def test_localize_uri_http(self):
        path = self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)
        assert os.path.getsize(path) == 267493

    @patch('zmlpsdk.storage.get_cached_google_storage_client')
    def test_localize_uri_gs(self, client_patch):
        client_patch.return_value = MockGcsClient()
        path = self.lfc.localize_uri('gs://zorroa-dev-data/image/pluto_2.1.jpg')
        assert os.path.exists(path)
        assert os.path.getsize(path) == 5

    def test_localize_uri_local_path(self):
        local_file = zorroa_test_data('images/set01/toucan.jpg', uri=False)
        path = self.lfc.localize_uri(local_file)
        assert os.path.exists(path)
        assert os.path.getsize(path) == 97221

    def test_get_path(self):
        path = self.lfc.get_path('spock', '.kirk')
        filename = '1a569625e9949f82ab1be5257ab2cab1f7524c6d.kirk'
        assert path.endswith(filename)

    def test_get_path_with_project_env(self):
        os.environ['ZMLP_PROJECT_ID'] = 'abc123'
        try:
            path = self.lfc.get_path('spock', '.kirk')
            filename = 'c85be874d0f9c380a790f583c2bec6633109386e.kirk'
            assert path.endswith(filename)
        finally:
            del os.environ['ZMLP_PROJECT_ID']

    def test_clear(self):
        path = self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)
        self.lfc.clear()
        assert not os.path.exists(path)

    def test_close(self):
        self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')
        self.lfc.close()

        with pytest.raises(FileNotFoundError):
            self.lfc.localize_uri('https://i.imgur.com/WkomVeG.jpg')

    @patch.object(ZmlpClient, 'stream')
    def test_precache_file(self, post_patch):
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg',
            'size': 100
        })
        post_patch.return_value = '/tmp/toucan.jpg'
        bird = zorroa_test_data('images/set01/toucan.jpg', uri=False)
        path = self.lfc.precache_file(pfile, bird)
        assert os.path.getsize(path) == os.path.getsize(bird)

    @patch.object(ZmlpClient, 'stream')
    def test_precache_file_zero_bytes(self, post_patch):
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg',
            'size': 0
        })
        post_patch.return_value = '/tmp/toucan.jpg'
        bird = zorroa_test_data('images/set01/toucan.jpg', uri=False)
        self.assertRaises(storage.ZmlpStorageException, self.lfc.precache_file, pfile, bird)


class FileStorageTests(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.fs = storage.FileStorage()

    @patch.object(ZmlpClient, 'stream')
    def test_localize_remote_file(self, post_patch):
        pfile = {
            'name': 'cat.jpg',
            'category': 'source',
            'attrs': {},
            'id': 'assets/123456/source/cat.jpg'
        }
        post_patch.return_value = '/tmp/cat.jpg'
        asset = TestAsset(id='123456')
        asset.set_attr('files', [pfile])
        path = self.fs.localize_file(asset)
        assert path.endswith('34d8c2639fcb5f54c50d6a211aaf63f04f679bfb.jpg')

    def test_localize_file_obj_with_uri(self):
        test_asset = TestAsset('https://i.imgur.com/WkomVeG.jpg')
        path = self.fs.localize_file(test_asset)
        assert os.path.exists(path)

    def test_localize_file_str(self):
        path = self.fs.localize_file('https://i.imgur.com/WkomVeG.jpg')
        assert os.path.exists(path)

    @patch.object(Minio, 'get_object')
    def test_localize_internal_uri(self, get_object_patch):
        class MockStream:
            def stream(self, _):
                return [b'ninja turles']

        get_object_patch.return_value = MockStream()
        path = self.fs.localize_file('zmlp://internal/officer/pdf/proxy.1.jpg')

        assert os.path.exists(path)
        assert os.path.getsize(path) == 12


class TestAssetStorage(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.fs = storage.FileStorage()
        self.pfile_dict = {
            'name': 'cat.jpg',
            'category': 'source',
            'attrs': {},
            'id': 'assets/123456/source/cat.jpg'
        }

    def tearDown(self):
        self.fs.cache.clear()

    @patch.object(ZmlpClient, 'put')
    @patch('requests.put')
    @patch.object(ZmlpClient, 'post')
    def test_store_timeline(self, post_patch, req_put_patch, put_patch):
        post_patch.return_value = {
            'uri': "http://localhost:9999/foo/bar/signed",
            'mediaType': "image/jpeg"
        }
        req_put_patch.return_value = MockResponse()
        put_patch.return_value = {
            'id': '12345',
            'name': 'foo-timeline.json.gz',
            'category': 'timeline'
        }

        asset = TestAsset(id='123456')
        tl = timeline.Timeline('cats')
        tl.add_track('tabby').add_clip(0, 1)
        result = self.fs.assets.store_timeline(asset, tl)
        assert 'foo-timeline.json.gz' == result.name
        assert 'timeline' == result.category

    @patch.object(ZmlpClient, 'put')
    @patch('requests.put')
    @patch.object(ZmlpClient, 'post')
    def test_store_file(self, post_patch, req_put_patch, put_patch):
        post_patch.return_value = {
            'uri': "http://localhost:9999/foo/bar/signed",
            'mediaType': "image/jpeg"
        }
        req_put_patch.return_value = MockResponse()
        put_patch.return_value = {
            'id': '12345',
            'name': 'cat.jpg',
            'category': 'proxy'
        }

        asset = TestAsset(id='123456')
        result = self.fs.assets.store_file(
            zorroa_test_data('images/set01/toucan.jpg', uri=False), asset, 'test')
        assert 'cat.jpg' == result.name
        assert 'proxy' == result.category

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_blob(self, upload_patch):
        upload_patch.return_value = {
            'name': 'vid-int-moderation.json',
            'category': 'google',
            'attrs': {},
            'id': 'assets/123456/google/vid-int-moderation.json',
        }
        asset = TestAsset(id='123456')
        result = self.fs.assets.store_blob(
            b'{"jo": "boo"}', asset, 'google', 'vid-int-moderation.json')
        assert 'google' == result.category
        assert 'vid-int-moderation.json' == result.name

    @patch.object(ZmlpClient, 'upload_file')
    def test_store_blob_no_ext(self, upload_patch):
        upload_patch.return_value = StoredFile({
            'name': 'vid-int-moderation.json',
            'category': 'google',
            'attrs': {},
            'id': 'assets/123456/google/vid-int-moderation.json',
        })
        asset = TestAsset(id='123456')
        with pytest.raises(ValueError):
            self.fs.assets.store_blob(
                asset, '{"jo": "boo"}', 'google', 'vid-int-moderation')

    @patch.object(ZmlpClient, 'get')
    def test_get_native_uri(self, get_patch):
        get_patch.return_value = {'uri': 'gs://hulk-hogan'}
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        uri = self.fs.assets.get_native_uri(pfile)
        assert 'gs://hulk-hogan' == uri


class TestProjectStorage(TestCase):

    def setUp(self):
        os.environ['ZMLP_STORAGE_PIPELINE_URL'] = 'http://localhost:9000'
        self.fs = storage.FileStorage()

    def tearDown(self):
        self.fs.cache.clear()

    @patch.object(ZmlpClient, 'put')
    @patch('requests.put')
    @patch.object(ZmlpClient, 'post')
    def test_store_file_with_rename(self, post_patch, req_put_patch, put_patch):
        post_patch.return_value = {
            'uri': "http://localhost:9999/foo/bar/signed",
            'mediaType': "image/jpeg"
        }
        req_put_patch.return_value = MockResponse()
        put_patch.return_value = {
            'id': 'datasets/12345/face_model/celebs.dat',
            'name': 'celebs.dat',
            'category': 'face_model',
            'entity': 'assets'
        }

        path = os.path.dirname(__file__) + '/fake_model.dat'
        ds = Model({"id": "12345"})
        result = self.fs.projects.store_file(
            path, ds, 'face_model', rename='celebs.dat')
        assert 'celebs.dat' == result.name
        assert 'face_model' == result.category

    @patch.object(ZmlpClient, 'put')
    @patch('requests.put')
    @patch.object(ZmlpClient, 'post')
    def test_store_file(self, post_patch, req_put_patch, put_patch):
        post_patch.return_value = {
            'uri': "http://localhost:9999/foo/bar/signed",
            'mediaType': "image/jpeg"
        }
        req_put_patch.return_value = MockResponse()
        put_patch.return_value = {
            'id': "asset/foo/fake/fake_model.dat",
            'name': 'fake_model.dat',
            'category': 'fake'
        }
        ds = Model({"id": "12345"})
        path = os.path.dirname(__file__) + '/fake_model.dat'
        result = self.fs.projects.store_file(path, ds, 'model', 'fake_model.dat')
        assert 'fake_model.dat' == result.name
        assert 'fake' == result.category

    @patch.object(ZmlpClient, 'put')
    @patch('requests.put')
    @patch.object(ZmlpClient, 'post')
    def test_store_file_by_id(self, post_patch, req_put_patch, put_patch):
        post_patch.return_value = {
            'uri': "http://localhost:9999/foo/bar/signed",
            'mediaType': "image/jpeg"
        }
        req_put_patch.return_value = MockResponse()
        put_patch.return_value = {
            'id': "asset/foo/fake/fake_model.dat",
            'name': 'fake_model.dat',
            'category': 'fake'
        }
        path = os.path.dirname(__file__) + '/fake_model.dat'
        fid = "dataset/12345/model/fake_model.dat"
        result = self.fs.projects.store_file_by_id(path, fid, attrs={'foo': 'bar'})
        assert 'fake_model.dat' == result.name
        assert 'fake' == result.category

    @patch.object(ZmlpClient, 'stream')
    def test_localize_file(self, post_patch):
        post_patch.return_value = '/tmp/toucan.jpg'
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        path = self.fs.localize_file(pfile)
        assert path.endswith('b9430537beae3fe8e6ba2e11667f0ccc9be82a28.jpg')

    @patch.object(ZmlpClient, 'stream')
    def test_localize_asset_file_with_asset_override(self, post_patch):
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        post_patch.return_value = '/tmp/cat.jpg'
        self.fs.projects.localize_file(pfile)
        assert 'assets/123456/proxy/cat.jpg' in post_patch.call_args_list[0][0][0]

    @patch.object(ZmlpClient, 'stream')
    def test_localize_file_id(self, post_patch):
        post_patch.return_value = '/tmp/cat.jpg'
        self.fs.projects.localize_file('asset/foo/fake/fake_model.dat')
        assert 'asset/foo/fake/fake_model.dat' in post_patch.call_args_list[0][0][0]

    @patch.object(ZmlpClient, 'get')
    def test_get_native_uri(self, get_patch):
        get_patch.return_value = {'uri': 'gs://hulk-hogan'}
        pfile = StoredFile({
            'name': 'cat.jpg',
            'category': 'proxy',
            'attrs': {},
            'id': 'assets/123456/proxy/cat.jpg'
        })
        uri = self.fs.projects.get_native_uri(pfile)
        assert 'gs://hulk-hogan' == uri


class ModelStorageTests(TestCase):

    def setUp(self):
        self.fs = storage.FileStorage()
        self.fs.models.root = tempfile.mkdtemp()

    def tearDown(self):
        self.fs.cache.clear()

    @patch.object(storage.ProjectStorage, 'localize_file')
    def test_install_model(self, post_patch):
        post_patch.return_value = os.path.dirname(__file__) + "/model.zip"
        path = self.fs.models.install_model('asset/foo/fake/fake_model.zip')
        assert os.path.exists(path + "/fake_model.dat")

        # Attempt to install twice
        path = self.fs.models.install_model('asset/foo/fake/fake_model.zip')
        assert os.path.exists(path + "/fake_model.dat")

    @patch.object(ZmlpClient, 'put')
    @patch('requests.put')
    @patch.object(ZmlpClient, 'post')
    @patch.object(storage.ModelStorage, 'publish_model')
    @patch.object(ModelApp, 'deploy_model')
    def test_save_model(self, deploy_patch, publish_patch, post_patch, req_put_patch, put_patch):
        post_patch.return_value = {
            'uri': "http://localhost:9999/foo/bar/signed",
            'mediaType': "image/jpeg"
        }
        req_put_patch.return_value = MockResponse()
        put_patch.return_value = {
            'id': "models/foo/fake/fake_model.dat",
            'name': 'fake_model.dat',
            'category': 'fake'
        }
        publish_patch.return_value = PipelineMod({
            'id': '12345'
        })
        deploy_patch.return_value = Job({"id": "abcde"})

        cur_dir = os.path.dirname(__file__) + '/extracted_model'
        mod = self.fs.models.save_model(cur_dir, "foo/bar/bing/model.zip", deploy=True)

        assert '12345' == mod.id


class ZipDirectoryTexts(TestCase):

    def test_zip_directory_no_base(self):
        output_zip = '/tmp/test-zip1.zip'
        cur_dir = os.path.dirname(__file__)
        storage.zip_directory(cur_dir, output_zip)

        with zipfile.ZipFile(output_zip) as zip:
            assert 'test_storage.py' in zip.namelist()
            assert 'model.zip' in zip.namelist()

    def test_zip_directory_with_base(self):
        output_zip = '/tmp/test-zip1.zip'
        cur_dir = os.path.dirname(__file__)
        storage.zip_directory(cur_dir, output_zip, 'base')

        with zipfile.ZipFile(output_zip) as zip:
            assert 'base/test_storage.py' in zip.namelist()
            assert 'base/model.zip' in zip.namelist()


class MockResponse:
    """
    A mock requests response.
    """

    def raise_for_status(self):
        pass


class MockGcsClient:
    """
    A Mock GCS Storage client
    """
    def get_bucket(self, *args, **kwars):
        return self

    def blob(self, *args, **kwargs):
        return self

    def download_to_filename(self, filename):
        with open(filename, 'w') as fp:
            fp.write("hello")
