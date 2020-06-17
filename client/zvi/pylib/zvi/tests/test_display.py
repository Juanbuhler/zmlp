import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, app_from_env
from zvi.display import show_thumbnails, show_asset


class DisplayTests(unittest.TestCase):

    def setUp(self):
        self.app = app_from_env()

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
                            },
                            "analysis": {"zvi-image-similarity": {
                                "simhash": "AAAAAAAA"}
                            },
                            "labels": [
                                {
                                    "dataSetId": "ds-id-12345",
                                    "label": "Glion",
                                }
                            ],
                            'system': {
                                'state': 'Analyzed'
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
                            },
                            'analysis': {"zvi-image-similarity": {
                                "simhash": "BBBBBBBB"}
                            },
                            "labels": [
                                {
                                    "dataSetId": "ds-id-12345",
                                    "label": "Gandalf",
                                }
                            ],
                            'system': {
                                'state': 'Analyzed'
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
                                'path': 'https://i.imgur.com/bar.jpg'
                            },
                            'analysis': {"zvi-image-similarity": {
                                "simhash": "CCCCCCCC"}
                            },
                            "labels": [
                                {
                                    "dataSetId": "ds-id-12345",
                                    "label": "Gandalf",
                                }
                            ],
                            'system': {
                                'state': 'Analyzed'
                            }
                        }
                    }
                ]
            }
        }

    @patch.object(ZmlpClient, 'post')
    def test_show_thumbnails(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        rsp = self.app.assets.search(search=search)
        paths = show_thumbnails(search=rsp)

        assert len(paths) == 3
        assert all(path.endswith('jpg') for path in paths)

    @patch.object(ZmlpClient, 'post')
    def test_show_asset(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        assets = self.app.assets.search(search=search)

        for asset in assets:
            document = show_asset(asset=asset)

            assert type(document) == dict
            assert list(document.keys()) == ['source', 'analysis', 'labels', 'system']
