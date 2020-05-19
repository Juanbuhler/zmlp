import os
import pickle
import tempfile
import zipfile

import numpy as np

from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.storage import file_storage


class KnnFaceRecognitionClassifier(AssetProcessor):
    def __init__(self):
        super(KnnFaceRecognitionClassifier, self).__init__()

        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))
        self.add_arg(Argument("sensitivity", "int", default=1000,
                              toolTip="How sensitive the model is to differences."))

        self.app_model = None
        self.face_classifier = None
        self.labels = None

    def init(self):
        self.app_model = self.app.models.get_model(self.arg_value('model_id'))
        self.face_classifier = self.load_model()

    def process(self, frame):
        asset = frame.asset

        faces = asset.get_attr('analysis.zvi-face-detection.predictions')
        if not faces:
            return

        x = self.hashes_as_nparray(faces)
        predictions = self.face_classifier.predict(x)
        dist, ind = self.face_classifier.kneighbors(x, n_neighbors=1, return_distance=True)

        min_distance = self.arg_value('sensitivity')
        for i, face in enumerate(faces):
            if dist[i][0] < min_distance:
                faces[i]['label'] = predictions[i]
                faces[i]['score'] = 1 - max(0, min(1, (dist[i][0] - 800) / (1100 - 800)))
            else:
                faces[i]['label'] = 'Unrecognized'

    def load_model(self):
        """
        Load the model.

        Returns:
            KNeighborsClassifier: The model.
        """
        model_zip = file_storage.projects.localize_file(self.app_model.file_id)
        with zipfile.ZipFile(model_zip) as zfp:
            zfp.extractall(path=tempfile.tempdir)
        with open(os.path.join(tempfile.tempdir, 'model', 'face_classifier.pickle'), 'rb') as fp:
            face_classifier = pickle.load(fp)
        return face_classifier

    @staticmethod
    def hashes_as_nparray(detections):
        """
        Convert the face hashes into a NP array so they can be compared
        to the ones in the model.

        Args:
            detections (list): List of face detection.

        Returns:
            nparray: Array of simhashes as a NP array.
        """
        data = []
        i = 0
        for f in detections:
            num_hash = []
            hash = f['simhash']
            for char in hash:
                num_hash.append(ord(char))
            data.append(num_hash)
            i += 1

        return np.asarray(data, dtype=np.float64)
