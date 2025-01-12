import os
import logging

from azure.cognitiveservices.vision.computervision import ComputerVisionClient
from msrest.authentication import CognitiveServicesCredentials

logger = logging.getLogger('azure')


def get_zvi_azure_cv_client():
    """
    Return an Azure Computer Vision client configured with ZVI credentials.

    Returns:
       ComputerVisionClient: an Azure ComputerVisionClient
    """
    key = os.environ.get('BOONAI_AZURE_VISION_KEY')
    if not key:
        raise RuntimeError('Azure support is not setup for this environment.')
    credentials = CognitiveServicesCredentials(key)

    region = os.environ.get('BOONAI_AZURE_VISION_REGION', 'centralus')
    endpoint = os.environ.get('BOONAI_AZURE_VISION_ENDPOINT',
                              f'https://{region}.api.cognitive.microsoft.com/')

    return ComputerVisionClient(
        endpoint=endpoint,
        credentials=credentials
    )


def log_backoff_exception(details):
    """
    Log an exception from the backoff library.

    Args:
        details (dict): The details of the backoff call.

    """
    logger.warning(
        'Waiting on quota {wait:0.1f} seconds afters {tries} tries'.format(**details))
