from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_clarifai_app

models = [
    'apparel-model',
    'food-model',
    'general-model',
    'travel-model',
    'wedding-model'
]


class ClarifaiLabelDetectionProcessor(AssetProcessor):
    namespace = 'clarifai-'

    def __init_(self):
        super(ClarifaiLabelDetectionProcessor, self).__init__()
        for model in models:
            self.add_arg(Argument(model, "boolean", required=False,
                                  toolTip="Enable the {} model".format(model)))
        self.clarifai = None

    def init(self):
        self.clarifai = get_clarifai_app()

    def process(self, frame):
        import pprint
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)

        for model_name in models:
            if self.arg_value(model_name):
                model = getattr(self.clarifai.public_models, model_name.replace("-", "_"))
                response = model.predict_by_filename(p_path)
                labels = response['outputs'][0]['data'].get('concepts')
                if not labels:
                    continue

                result = [
                    {'label': label['name'],
                     'score': round(label['value'], 3)} for label in labels]

                asset.add_analysis(self.namespace + model_name, {
                    'predictions': result,
                    'count': len(result),
                    'type': 'labels'
                })
