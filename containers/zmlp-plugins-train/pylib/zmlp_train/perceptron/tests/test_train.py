import logging
import random
import string
from unittest.mock import patch

from zmlp.app import ModelApp, AssetApp
from zmlp.entity import Model, StoredFile, PipelineMod, Asset
from zmlp_train.perceptron import LabelDetectionPerceptronTrainer
from zmlpsdk import Frame, file_storage
from zmlpsdk.testing import PluginUnitTestCase, TestAsset

logging.basicConfig()

model_id = "model-id-12345"
ds_id = "ds-id-12345"


class LabelDetectionPerceptronTrainerTests(PluginUnitTestCase):
    class_names = ["Gandalf", "Glion"]

    def setUp(self):
        self.mock_search_result = [
            Asset.from_hit({
                '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/SSN26nN.jpg'
                    },
                    "analysis": {
                        "zvi-image-similarity": {
                            "simhash": "".join(random.choice(
                                string.ascii_uppercase) for _ in range(2048)),
                        }
                    },
                    "labels": [
                        {
                            "dataSetId": ds_id,
                            "label": self.class_names[random.randint(0, 1)],
                        }
                    ]
                }
            }) for _ in range(100)
        ]

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
            'dataSetId': ds_id,
            'type': "LABEL_DETECTION_PERCEPTRON",
            'fileId': 'models/{}/foo/bar'.format(model_id),
            'name': name
        })
        search_patch.return_value = self.mock_search_result
        upload_patch.return_value = StoredFile({"id": "12345"})

        args = {
            'model_id': model_id
        }

        processor = self.init_processor(LabelDetectionPerceptronTrainer(), args)
        processor.process(Frame(TestAsset()))
