from urllib.parse import urlparse

from zmlpsdk import Generator, Argument
from zmlpsdk.cloud import get_google_storage_client
from zmlp.asset import FileImport
from zmlp import app_from_env


class AssetSearchGenerator(Generator):
    """Generates frames based on an elastic search query.

    Args:
        search (dict): An ES query.
        max_assets (int): The maximum number of assets to process, 0 for unlimited.
        scroll (str): The scroll timeout, ex: 5m.  Set to false for no scrolling.

    """
    toolTips = {
        'search': 'Json formatted string representing a an elastic search query.',
        'max_assets': 'The maximum number of items to iterate.',
        'scroll': 'The scan/scroll timeout value.  Set to false to disable ElasticSearch scrolling.'
    }

    def __init__(self):
        super(AssetSearchGenerator, self).__init__()
        self.add_arg(Argument('search', 'dict', required=False, toolTip=self.toolTips['search']))
        self.add_arg(Argument('max_assets', 'int', required=False,
                              default=0, toolTip=self.toolTips['max_assets']))
        self.add_arg(Argument('scroll', 'string', required=False,
                              default="5m", toolTip=self.toolTips['scroll']))
        self.app = app_from_env()
        self.total_consumed = 0

    def generate(self, consumer):
        self.scroll_assets(consumer)

    def scroll_assets(self, consumer):
        max_assets = self.arg_value('max_assets')
        search = self.arg_value('search')
        timeout = self.arg_value('timeout')
        for asset in self.app.assets.scroll_search(search, timeout):
            consumer.accept(asset)
            self.total_consumed += 1
            if max_assets and self.total_consumed >= max_assets:
                self.logger.info("Max assets: {} reached".format(max_assets))
                break


class GcsBucketGenerator(Generator):
    """Simple generator that fetches all of the objects in a GCS bucket.

    To use this generator in production, google credentials must be mounted to the container.
    If no credentials are found it will attempt to use anonymous credentials.  To open
    a bucket to an Anonymous GCS user, give  "Storage Object Viewer" or
    "Storage Legacy Bucket Reader" to "allUsers".

    Args:
        uri (str): Address of a bucket in the form "gs://<BUCKET_NAME>".

    """

    def __init__(self):
        super(GcsBucketGenerator, self).__init__()
        self.add_arg(Argument('uri', 'str', required=True))

    def generate(self, consumer):
        uri = urlparse(self.arg_value('uri'))
        storage_client = get_google_storage_client()
        for blob in storage_client.list_blobs(uri.netloc, prefix=uri.path.lstrip("/")):
            if blob.name.endswith("/"):
                continue
            gsuri = "gs://{}/{}".format(uri.netloc, blob.name)
            consumer.accept(FileImport(gsuri))
