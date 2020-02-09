import subprocess
import sys
import tempfile
import logging
import collections

from PIL import Image
from pathlib2 import Path

from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.storage import file_storage
from zmlpsdk.proxy import store_asset_proxy, get_proxy_level
from zmlp_core.util.media import get_output_dimension, media_size


logger = logging.getLogger(__file__)


class ImageProxyProcessor(AssetProcessor):
    toolTips = {
        'sizes': 'Sizes of the proxies to create.',
        'file_type': 'File type of the proxies to create.',
        'resize_filter': 'Filter to use.',
        'output_args': 'Extra arguments for oiio.'
    }

    VALID_FILE_TYPES = {'jpg': 'image/jpeg', 'png': 'image/png'}

    def __init__(self):
        super(ImageProxyProcessor, self).__init__()
        self.created_proxy_count = 0
        self.add_arg(Argument('sizes', 'list[int]', default=[1024, 512, 320],
                              toolTip=self.toolTips['sizes']))
        self.add_arg(Argument('file_type', 'str', default='jpg',
                              toolTip=self.toolTips['file_type']))
        self.add_arg(Argument('resize_filter', 'str', default='lanczos3',
                              toolTip=self.toolTips['resize_filter']))
        self.add_arg(Argument('output_args', 'list[str]', default=[],
                              toolTip=self.toolTips['output_args']))

    def init(self):
        file_type = self.arg_value('file_type')
        if file_type not in self.VALID_FILE_TYPES:
            raise ValueError('"%s" is not a valid type (%s)' %
                             (file_type, self.VALID_FILE_TYPES.keys()))

    def process(self, frame):
        # Inherits parent docstring.
        asset = frame.asset
        source_path = self._get_source_path(asset)

        # If we can't make a proxy just log it and move on.  Proxies
        # can be made with ExistingProxyIngestor as well.
        # We want to avoid generating error by trying to use oiio
        # on word docs and stuff like that.
        # In the future the server side will detect missing proxy images.
        if not source_path:
            self.logger.info('No suitable proxy path found for asset "{}"'
                             .format(asset.get_attr('source.path')))
            return

        self.logger.info('Creating %s proxies for %s.' % (self.arg_value('file_type'),
                                                          source_path))
        proxy_paths = self._create_proxy_images(asset)
        for proxy in proxy_paths:
            store_asset_proxy(asset, proxy[2], (proxy[0], proxy[1]))
        set_tiny_proxy_colors(asset)

    def _create_proxy_images(self, asset):
        """
        Creates the proxy images.

        Args:
            asset(Document): Document that is being processed.

        Returns:
            list<str>: Paths to the proxies that were created.

        """
        proxy_descriptors = self._get_proxy_descriptors(asset)
        oiiotool_command = self._get_oiio_command_line(asset, proxy_descriptors)
        # Create the parent directory of the tmp output location of each proxy.
        for (_, _, output_path) in proxy_descriptors:
            if not output_path.parent.exists():
                output_path.parent.mkdir(parents=True)

        # If we had outputs then we need to actually shell out to oiiotool.
        if proxy_descriptors:
            self.logger.info('oiiotool command to create proxies: %s' % oiiotool_command)
            subprocess.check_call(oiiotool_command, stdout=sys.stdout, stderr=sys.stdout)
            self.created_proxy_count += len(proxy_descriptors)
        else:
            self.logger.info('All proxies already exist. No proxies will be created.')
        return proxy_descriptors

    def _get_oiio_command_line(self, asset, proxy_descriptors):
        """
        Compose the OIIO tool command line for generating all the necessary proxy files.

        Args:
            asset: The asset currently being processed.
            proxy_descriptors: A list of (width, height, source_path) tuples describing the
                proxies that need to be computed.

        Returns:
            A list with all the command line parameters necessary to launch the OIIO tool
            to generate proxy files.
        """
        source_path = self._get_source_path(asset)

        # Crete the base of the oiiotool shell command.
        oiiotool_command = ['oiiotool', '-native', '-wildcardoff', source_path,
                            '--threads', '--cache 100', '--clear-keywords',
                            '--nosoftwareattrib', '--eraseattrib', 'thumbnail_image',
                            '--eraseattrib', 'Exif:.*', '--eraseattrib', 'IPTC:.*']
        if asset.get_attr('media.clip.type') == 'image':
            start = asset.get_attr('media.clip.start')
            if start:
                page = start - 1
                oiiotool_command.extend(['--subimage', str(int(page))])
        for (width, height, output_path) in proxy_descriptors:
            # We need to create a proxy so add an output to the oiiotool command.
            oiiotool_command.extend([
                '--resize:filter=%s' % self.arg_value('resize_filter'),
                '%sx%s' % (width, height),
                '--autocc', '--quality', '100'
            ])
            oiiotool_command.extend(self.arg_value('output_args'))
            oiiotool_command.extend(['-o', str(output_path)])

        return oiiotool_command

    def _get_proxy_descriptors(self, asset):
        """
        Make a list of (width, height, path) tuples of the proxies to be computed.

        The algorithm examines the asset for existing proxies and does not put descriptors
        in the list if a matching proxy already exists.

        Args:
            asset: The asset for which the proxy sizes are to be determined.

        Returns:
            A list of (width, height) pairs with the proxy sizes.
        """
        self.logger.info("Existing proxies: %s" % asset.get_files())
        source_width, source_height = self._get_source_dimensions(asset)
        tmp_dir = Path(tempfile.gettempdir())
        # Determine list of (width, height) for proxies to be made.
        proxy_sizes = []
        for size in self._get_valid_sizes(source_width, source_height):
            width, height = get_output_dimension(size, source_width, source_height)

            mime_type = self.VALID_FILE_TYPES[self.arg_value('file_type')]
            # If the proxy already exists and we aren't forcing creation then move on.
            output_path = tmp_dir.joinpath('%s_%sx%s.%s' %
                                           (asset.id, width, height, self.arg_value('file_type')))
            proxy_sizes.append((width, height, output_path))

        return proxy_sizes

    def _get_source_dimensions(self, asset):
        """
        Return the source dimensions for the given asset.  If the asset
        is an image and has a width/height, then return those values.  Otherwise
        try to detect and use the image rendering of the asset.

        Args:
            asset (Asset): The asset to check

        Returns (list): A list of two integers, width and height

        """
        # If the source is an image then oiio has already setup the settings we need.
        if asset.get_attr("media.type") == "image" \
                and asset.attr_exists("media.width") \
                and asset.attr_exists("media.height"):
            return [asset.get_attr("media.width"), asset.get_attr("media.height")]
        else:
            # If the source is not an image, try to figure it out by looking at
            # the tmp image representation of the asset.
            return media_size(self._get_source_path(asset))

    def _get_source_path(self, asset):

        proxy_source_file = asset.get_attr("tmp.proxy_source_image")
        if proxy_source_file:
            return file_storage.localize_uri(proxy_source_file)

        # Handles pulling the actual source.path or a files source.
        # If the source file type is not an image, this processor
        # has no chance of making a proxy, so we're going to skip
        # generating an error.
        if asset.get_attr("media.type") == "image":
            return file_storage.localize_remote_file(asset)
        return None

    def _get_valid_sizes(self, width, height):
        """Based on the sizes provided to the processor the valid sizes to create proxies
        of are returned.

        Args:
            width(int): The width of the image to use for determining valid proxy sizes.
            height(int): The height of the image to use for determining valid proxy sizes.
        Returns:
            list<int>: List of valid proxy sizes to create.

        """
        valid_sizes = []
        longest_edge = max(width, height)
        for size in self.arg_value('sizes'):
            # Can't use a set here, maintaining order
            _size = min(size, longest_edge)
            if _size not in valid_sizes:
                valid_sizes.append(_size)

        if not valid_sizes:
            valid_sizes.append(longest_edge)
        return valid_sizes


def set_tiny_proxy_colors(asset):
    """Select the smallest available image proxy and create a tiny proxy.

    Args:
        asset (Asset): Asset to set the tiny proxy colors on.

    """
    if not asset.get_attr('tmp.proxies.tinyProxyGenerated'):
        smallest_proxy = get_proxy_level(asset, 0)
        if smallest_proxy:
            logger.info('Creating tiny proxy colors for %s.' % smallest_proxy)
            asset.set_attr('analysis.zmlp.tinyProxy',
                           get_tiny_proxy_colors(smallest_proxy) or None)

            # Mark that the tiny proxy was generated so we don't do this multiple times
            # if the customer has multiple proxy importers.
            asset.set_attr('tmp.proxies.tinyProxyGenerated', True)


def get_tiny_proxy_colors(image_path):
    """Takes a sampling of 9 evenly spaced pixels from a proxy image to represent it's
    general colors. To get these pixel values the images is downres'd to an 11x11
    image. The outer row of pixels is ignored and the remaining pixels are divided
    into 3x3 squares. The hex pixel color of the center of each 3x3 square is
    returned.

    Args:
        image_path: Path to an image to extract a tiny proxy from.

    Returns:
        list(str): List of 9 hex color values that represent the image.

    """
    colors = []
    image = Image.open(image_path).resize((11, 11)).convert('RGB')
    coordinates = [(3, 3), (6, 3), (9, 3), (3, 6), (6, 6), (9, 6), (3, 9), (6, 9), (9, 9)]
    for coordinate in coordinates:
        rgb = image.getpixel(coordinate)
        color = '#%02x%02x%02x' % rgb
        colors.append(color)
    return colors


ProxySelection = collections.namedtuple('name', '')
