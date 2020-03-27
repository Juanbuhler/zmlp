import copy
import unittest
import unittest.mock as mock
from unittest.mock import patch

import pytest

from zmlp import Asset
from zmlp import ZmlpClient, app_from_env
from zmlp.asset import FileImport, FileUpload
from zmlp.exception import ZmlpException

from .util import get_test_file


class AssetAppTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

        self.mock_import_result = {
            'bulkResponse': {
                'took': 15,
                'errors': False,
                'items': [{
                    'create': {
                        '_index': 'yvqg1901zmu5bw9q',
                        '_type': '_doc',
                        '_id': 'dd0KZtqyec48n1q1fniqVMV5yllhRRGx',
                        '_version': 1,
                        'result': 'created',
                        'forced_refresh': True,
                        '_shards': {
                            'total': 1,
                            'successful': 1,
                            'failed': 0
                        },
                        '_seq_no': 0,
                        '_primary_term': 1,
                        'status': 201
                    }
                }]
            },
            'failed': [],
            'created': ['dd0KZtqyec48n1q1fniqVMV5yllhRRGx'],
            'jobId': 'ba310246-1f87-1ece-b67c-be3f79a80d11'
        }

        # A mock search result used for asset search tests
        self.mock_search_result = {
            'took': 4,
            'timed_out': False,
            'hits': {
                'total': {'value': 2},
                'max_score': 0.2876821,
                'hits': [
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                        '_score': 0.2876821,
                        '_source': {
                            'source': {
                                'path': 'https://i.imgur.com/SSN26nN.jpg'
                            }
                        }
                    },
                    {
                        '_index': 'litvqrkus86sna2w',
                        '_type': 'asset',
                        '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
                        '_score': 0.2876821,
                        '_source': {
                            'source': {
                                'path': 'https://i.imgur.com/foo.jpg'
                            }
                        }
                    }
                ]
            }
        }

    @patch.object(ZmlpClient, 'post')
    def test_import_files(self, post_patch):
        post_patch.return_value = self.mock_import_result
        assets = [FileImport('gs://zorroa-dev-data/image/pluto.png')]
        rsp = self.app.assets.batch_import_files(assets)
        assert rsp['created'][0] == 'dd0KZtqyec48n1q1fniqVMV5yllhRRGx'

    @patch.object(ZmlpClient, 'get')
    def test_get_asset(self, get_patch):
        get_patch.return_value = {
            'id': 'abc13',
            'document': {
                'source': {
                    'path': 'gs://zorroa-dev-data/image/pluto.png'
                }
            }
        }
        asset = self.app.assets.get_asset('abc123')
        assert type(asset) == Asset
        assert asset.uri is not None
        assert asset.id is not None
        assert asset.document is not None

    @patch.object(ZmlpClient, 'upload_files')
    def test_upload_assets(self, post_patch):
        post_patch.return_value = self.mock_import_result

        path = get_test_file('images/set01/toucan.jpg')
        assets = [FileUpload(path)]
        rsp = self.app.assets.batch_upload_files(assets)
        assert rsp['created'][0] == 'dd0KZtqyec48n1q1fniqVMV5yllhRRGx'

    @patch.object(ZmlpClient, 'post')
    def test_search_defaults(self, post_patch):
        post_patch.return_value = self.mock_search_result
        assets = self.app.assets.search().assets
        assert 'https://i.imgur.com/SSN26nN.jpg' == assets[0].get_attr('source.path')

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_scroll_search_raises_on_no_scroll_id(self, post_patch, del_patch):
        post_patch.return_value = self.mock_search_result
        del_patch.return_value = {}
        with pytest.raises(ZmlpException):
            for _ in self.app.assets.scroll_search():
                pass

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_scroll_search(self, post_patch, del_patch):
        scroll_result = copy.deepcopy(self.mock_search_result)
        scroll_result['_scroll_id'] = 'abc123'

        post_patch.side_effect = [scroll_result, {'hits': {'hits': []}}]
        del_patch.return_value = {}
        for asset in self.app.assets.scroll_search():
            print(asset)

    @patch.object(ZmlpClient, 'post')
    def test_search_raw_response(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        path = rsp.raw_response['hits']['hits'][0]['_source']['source']['path']
        assert path == 'https://i.imgur.com/SSN26nN.jpg'

    @patch.object(ZmlpClient, 'post')
    def test_search_iter(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        path = rsp[0].get_attr('source.path')
        assert path == 'https://i.imgur.com/SSN26nN.jpg'
        assert 2 == rsp.size

        # Iterate the result to test iteration.
        count = 0
        for _ in rsp:
            count += 1
        assert count == 2

    @patch.object(ZmlpClient, 'post')
    def test_reprocess_search(self, post_patch):
        post_patch.return_value = {
            'assetCount': 101,
            'job': {
                'id': 'abc',
                'name': 'reprocess'
            }
        }
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.reprocess_search(search, ['zmlp-labels'])
        assert 101 == rsp.asset_count
        assert 'abc' == rsp.job.id
        assert 'reprocess' == rsp.job.name

    @patch.object(ZmlpClient, 'put')
    def test_index(self, put_patch):
        asset = Asset({'id': '123'})
        asset.set_attr('foo.bar', 'bing')
        self.app.assets.index(asset)
        args = put_patch.call_args_list
        body = args[0][0][1]
        assert body['foo'] == {'bar': 'bing'}

    @patch.object(ZmlpClient, 'post')
    def test_batch_index(self, post_patch):
        asset = Asset({'id': '123'})
        asset.set_attr('foo.bar', 'bing')
        self.app.assets.batch_index([asset])
        args = post_patch.call_args_list
        body = args[0][0][1]
        assert body['123'] == {'foo': {'bar': 'bing'}}

    @patch.object(ZmlpClient, 'post')
    def test_update(self, post_patch):
        req = {
            'foo': 'bar'
        }

        self.app.assets.update('123', req)
        args = post_patch.call_args_list
        body = args[0][0][1]
        assert body['doc'] == {'foo': 'bar'}

    @patch.object(ZmlpClient, 'post')
    def test_batch_update(self, post_patch):
        req = {
            'abc123': {
                'doc': {
                    'foo': 'bar'
                }
            }
        }

        self.app.assets.batch_update(req)
        args = post_patch.call_args_list
        body = args[0][0][1]
        assert body['abc123'] == {'doc': {'foo': 'bar'}}

    @patch.object(ZmlpClient, 'delete')
    def test_delete_with_asset_object(self, del_patch):
        asset = Asset({'id': '123'})
        self.app.assets.delete(asset)
        args = del_patch.call_args_list
        uri = args[0][0][0]
        assert '/api/v3/assets/123' == uri

    @patch.object(ZmlpClient, 'delete')
    def test_delete_with_asset_id(self, del_patch):
        self.app.assets.delete('123')
        args = del_patch.call_args_list
        uri = args[0][0][0]
        assert '/api/v3/assets/123' == uri

    @patch.object(ZmlpClient, 'delete')
    def test_delete_by_query(self, del_patch):
        q = {'query': {'match_all': {}}}
        self.app.assets.delete_by_query(q)
        args = del_patch.call_args_list
        assert '/api/v3/assets/_delete_by_query' == args[0][0][0]
        assert q == args[0][0][1]

    @patch.object(ZmlpClient, 'get')
    def test_download_file(self, get_patch):
        data = b'some_data'
        mockresponse = mock.Mock()
        mockresponse.content = data
        get_patch.return_value = mockresponse

        b = self.app.assets.download_file('123', 'proxy/proxy123.jpg')
        assert 'some_data' == b.read().decode()

    @patch.object(ZmlpClient, 'upload_files')
    def test_et_sim_hashes(self, upload_patch):
        upload_patch.return_value = ['ABC']
        path = get_test_file('images/set01/toucan.jpg')
        b = self.app.assets.get_sim_hashes(path)
        assert b == ['ABC']

    @patch.object(ZmlpClient, 'upload_files')
    def test_get_sim_hashes_file_handle(self, upload_patch):
        upload_patch.return_value = ['ABC']
        path = open(get_test_file('images/set01/toucan.jpg'), 'rb')
        b = self.app.assets.get_sim_hashes(path)
        assert b == ['ABC']

    @patch.object(ZmlpClient, 'upload_files')
    def test_get_sim_query(self, upload_patch):
        upload_patch.return_value = ['ABC']
        path = open(get_test_file('images/set01/toucan.jpg'), 'rb')
        b = self.app.assets.get_sim_query(path)
        assert b.hashes == ['ABC']
