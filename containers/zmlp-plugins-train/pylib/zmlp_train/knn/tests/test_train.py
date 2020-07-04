import logging
from unittest.mock import patch

from zmlp.app import ModelApp, AssetApp
from zmlp.entity import Model, StoredFile, PipelineMod, Asset
from zmlp_train.knn import KnnLabelDetectionTrainer
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()

model_id = "model-id-12345"


class KnnLabelDetectionTrainerTests(PluginUnitTestCase):

    @patch.object(file_storage.models, 'publish_model')
    @patch.object(ModelApp, 'get_model')
    @patch.object(AssetApp, 'scroll_search')
    @patch.object(file_storage.projects, "store_file_by_id")
    def test_process(self, upload_patch, search_patch, model_patch, pub_patch):

        name = 'custom-knn-labels-detect'
        pub_patch.return_value = PipelineMod({
            'id': "12345",
            'name': name
        })
        model_patch.return_value = Model({
            'id': model_id,
            'type': "ZVI_KNN_CLASSIFIER",
            'fileId': 'models/{}/foo/bar'.format(model_id),
            'name': name
        })
        search_patch.return_value = mock_search_result
        upload_patch.return_value = StoredFile({"id": "12345"})

        args = {
            'model_id': model_id
        }

        processor = self.init_processor(KnnLabelDetectionTrainer(), args)
        processor.process(Frame(TestAsset()))


mock_search_result = [
    Asset.from_hit({
        '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
        '_score': 0.2876821,
        '_source': {
            'source': {
                'path': 'https://i.imgur.com/SSN26nN.jpg'
            },
            "analysis": {"zvi-image-similarity": {"simhash": "AAAAAAAA"}},
            "labels": [
                {
                    "modelId": model_id,
                    "label": "Gandalf",
                }
            ]
        }
    }),
    Asset.from_hit({
        '_index': 'litvqrkus86sna2w',
        '_type': 'asset',
        '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
        '_score': 0.2876821,
        '_source': {
            'source': {
                'path': 'https://i.imgur.com/foo.jpg'
            },
            "analysis": {"zvi-image-similarity": {"simhash": "BBBBBBBB"}},
            "labels": [
                {
                    "modelId": model_id,
                    "label": "Glion",
                }
            ]
        }
    })
]
