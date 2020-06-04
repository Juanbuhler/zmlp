import unittest
from unittest.mock import patch

from zmlp import ZmlpClient, app_from_env
from zvi.pandas import search_to_df


class PandasTests(unittest.TestCase):

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
                            ]
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
                            ]
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
                        }
                    }
                ]
            }
        }

    @patch.object(ZmlpClient, 'post')
    def test_search_to_df(self, post_patch):
        post_patch.return_value = self.mock_search_result
        search = {
            'query': {'match_all': {}}
        }
        attrs = [
            'source.path',
            'analysis.zvi-image-similarity.simhash',
            'labels'
        ]
        rsp = self.app.assets.search(search=search)
        df = search_to_df(
            search=rsp,
            attrs=attrs[1:],  # all but source.path
            descriptor='source.path'
        )

        assert df.shape == (3, 3)
        assert list(df.columns) == attrs
        assert df.iloc[1][attrs[1]] == 'BBBBBBBB'  # simhash
        assert not df.iloc[2][attrs[2]]  # no `labels` for last asset
