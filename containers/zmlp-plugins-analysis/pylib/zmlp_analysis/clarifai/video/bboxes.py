# flake8: noqa
from zmlpsdk import AssetProcessor, Argument, FileTypes, file_storage, proxy, clips, video
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlp_analysis.clarifai.images.bboxes import *

models = [
    'face-detection-model',
    'logo-model'
]

MAX_LENGTH_SEC = 120


class AbstractClarifaiVideoProcessor(AssetProcessor):
    """
    This base class is used for all Microsoft Computer Vision features.  Subclasses
    only have to implement the "predict(asset, image) method.
    """

    file_types = FileTypes.images | FileTypes.documents

    namespace = 'clarifai-video'
    model_name = 'general-model'

    def __init__(self, model_name, reactor=None):
        super(AbstractClarifaiVideoProcessor, self).__init__()
        self.add_arg(Argument('debug', 'bool', default=False))
        self.reactor = reactor
        self.image_client = None
        self.model_name = model_name

    def process(self, frame):
        asset = frame.asset
        asset_id = asset.id
        final_time = asset.get_attr('media.length')

        if final_time > MAX_LENGTH_SEC:
            self.logger.warning(
                'Skipping, video is longer than {} seconds.'.format(self.max_length_sec))
            return

        video_proxy = proxy.get_video_proxy(asset)
        if not video_proxy:
            self.logger.warning(f'No video could be found for {asset_id}')
            return

        local_path = file_storage.localize_file(video_proxy)
        extractor = video.ShotBasedFrameExtractor(local_path)
        clip_tracker = clips.ClipTracker(asset, self.namespace)
        model = getattr(self.image_client.clarifai.public_models, self.model_name.replace("-", "_"))

        analysis, clip_tracker = self.set_analysis(extractor, clip_tracker, model)
        asset.add_analysis("-".join([self.namespace, self.model_name]), analysis)
        timeline = clip_tracker.build_timeline(final_time)
        video.save_timeline(timeline)

    def set_analysis(self, extractor, clip_tracker, model):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker
            model: Clarifai.PublicModel

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)

        for time_ms, path in extractor:
            response = model.predict_by_filename(path)
            try:
                concepts = response['outputs'][0]['data'].get('regions')
            except KeyError:
                continue
            if not concepts:
                continue
            for concept in concepts:
                c = concept['data'].get('concepts')[0]
                labels = [c['name']]
                clip_tracker.append(time_ms, labels)
                analysis.add_label_and_score(c['name'], c['value'])

        return analysis, clip_tracker

    def emit_status(self, msg):
        """
        Emit a status back to the Archivist.

        Args:
            msg (str): The message to emit.

        """
        if not self.reactor:
            return
        self.reactor.emit_status(msg)


class ClarifaiVideoFaceDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai face detection"""

    def __init__(self):
        super(ClarifaiVideoFaceDetectionProcessor, self).__init__('face-detection-model')

    def init(self):
        self.image_client = ClarifaiFaceDetectionProcessor()
        self.image_client.init()


class ClarifaiVideoLogoDetectionProcessor(AbstractClarifaiVideoProcessor):
    """ Clarifai logo detection"""

    def __init__(self):
        super(ClarifaiVideoLogoDetectionProcessor, self).__init__('logo-model')

    def init(self):
        self.image_client = ClarifaiLogoDetectionProcessor()
        self.image_client.init()
