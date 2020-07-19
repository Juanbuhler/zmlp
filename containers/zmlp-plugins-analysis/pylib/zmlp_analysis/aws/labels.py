import boto3

from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path


class RekognitionLabelClassifier(AssetProcessor):
    """Get labels for an image using AWS Rekognition """

    def __init__(self):
        super(RekognitionLabelClassifier, self).__init__()

        self.add_arg(
            Argument("model_id", "str", required=True, toolTip="The model Id")
        )
        self.add_arg(
            Argument("max_labels", "int", default=3, toolTip="Total number of labels")
        )

        self.app_model = None
        self.client = None
        self.label_and_score = None
        self.max_labels = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))

        # AWS client
        self.client = boto3.client('rekognition')

        self.max_labels = self.arg_value("max_labels")

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        self.predict(proxy_path)

        analysis = LabelDetectionAnalysis(min_score=0.01)
        for ls in self.label_and_score:
            analysis.add_label_and_score(ls[0], ls[1])

        asset.add_analysis(self.app_model.module_name, analysis)

    def predict(self, path):
        """ Make a prediction for an image path.
        self.label_and_score (List[tuple]): result is list of tuples in format [(label, score),
            (label, score)]

        Args:
            path (str): image path

        Returns:
            None
        """
        with open(path, 'rb') as f:
            source_bytes = f.read()

        # get predictions
        img_json = {'Bytes': source_bytes}
        response = self.client.detect_labels(Image=img_json, MaxLabels=self.max_labels)

        # get list of labels
        self.label_and_score = [(r['Name'], r['Confidence']) for r in response['Labels']]
