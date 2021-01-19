import cv2
import mxnet
import numpy as np
from collections import namedtuple

Batch = namedtuple('Batch', ['data'])


class SimilarityEngine:
    """
    Calculates similarity hashes using MXNET Resnet152.
    """
    default_model_path = "/models/resnet-152"

    def __init__(self, model_path=None):
        if not model_path:
            model_path = self.default_model_path
        self.mod = self._load_model(model_path)

    def calculate_hash(self, path):
        """
        Calculate a similarity hash using the given file path.

        Args:
            path (str): Path to the file.

        Returns:
            str: The hash itself.
        """
        self.mod.forward(Batch([self._load_image(path)]))
        features = self.mod.get_outputs()[0].asnumpy()
        features = np.squeeze(features)

        mxh = np.clip((features*16).astype(int), 0, 15) + 65
        return "".join([chr(item) for item in mxh])

    def _load_image(self, path):
        """
        Load an image into a numpy array and prep it for mxnet.

        Args:
            path (str):

        Returns:
            NpArray: A numpy array.

        """
        img = cv2.imread(path)
        img = cv2.resize(img, (224, 224))
        if img.shape == (224, 224):
            img = cv2.cvtColor(img, cv2.CV_GRAY2RGB)
        img = np.swapaxes(img, 0, 2)
        img = np.swapaxes(img, 1, 2)
        img = img[np.newaxis, :]
        return mxnet.nd.array(img)

    def _load_model(self, path):
        """
        Load the model.

        Returns:
            mxnet.mod.Module: The mxnet model.
        """
        mp = f"{path}/resnet-152"
        sym, arg_params, aux_params = mxnet.model.load_checkpoint(mp, 0)

        all_layers = sym.get_internals()
        fe_sym = all_layers['flatten0_output']
        fe_mod = mxnet.mod.Module(symbol=fe_sym, context=mxnet.cpu(), label_names=None)
        fe_mod.bind(for_training=False, data_shapes=[('data', (1, 3, 224, 224))])
        fe_mod.set_params(arg_params, aux_params)
        return fe_mod