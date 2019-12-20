import operator
import re
from datetime import datetime
from functools import reduce

import dateutil.parser
from pathlib2 import Path

from pixml import Clip, FileImport
from pixml.analysis import AssetBuilder, Argument, ExpandFrame
from pixml.analysis.storage import file_cache
from ..util.media import get_image_metadata, set_resolution_attrs


class ImageImporter(AssetBuilder):
    default_fields = ['XResolution', 'YResolution', 'ResolutionUnit',
                      'Exif.ExposureBiasValue', 'Exif.ExposureProgram', 'ExposureTime',
                      'Exif.ExposureTime', 'Exif.Flash', 'FNumber', 'Exif.FNumber',
                      'Exif.FocalLength', 'Exif.FocalLength35', 'Exif.Make', 'Exif.Model',
                      'Orientation', 'Exif.Orientation', 'Exif.Software', 'IPTC.Keywords',
                      'XMP.FNumber', 'FocalLength', 'XMP.FocalLength', 'Make', 'XMP.Make',
                      'Model', 'XMP.Model', 'XMP.Rating', 'IPTC.Country', 'IPTC.State',
                      'Keywords', 'Copyright', 'Artist', 'ImageDescription',
                      'IPTC.Headline', 'IPTC.Creator', 'IPTC.ObjectName',
                      'IPTC.AuthorsPosition', 'Keywords', 'IPTC.Keywords',
                      'IPTC.Country', 'IPTC.State', 'ImageDescription', 'IPTC.Headline']
    date_fields = ['Exif.DateTimeOriginal', 'Exif.DateTimeDigitized',
                   'Exif.DateTime', 'IPTC.DateCreated', 'IPTC.TimeCreated', 'DateTime',
                   'File.FileModifiedDate', 'Date']

    file_types = ["bmp", "cin", "dds", "dicom", "dpx", "gif", "hdr", "ico", "iff",
                  "jpeg", "jpg", "jp2", "exr", "png", "pnm", "psd", "raw", "rla", "sgi",
                  "tiff", "tif"]

    def __init__(self):
        super(ImageImporter, self).__init__()
        self.add_arg(Argument('included_tags', 'list[str]', default=self.default_fields))
        self.add_arg(Argument('extract_pages', 'bool', default=False))
        self.add_arg(Argument('extract_extended_metadata', 'bool', default=False))

    def process(self, frame):
        asset = frame.asset
        path = Path(file_cache.localize_remote_file(asset))
        metadata = get_image_metadata(path)
        set_resolution_attrs(asset, int(metadata.get('full_width')),
                             int(metadata.get('full_height')))

        self.set_media_type(asset)
        self.set_location(asset, metadata)
        self.set_date(asset, metadata)
        self.set_metadata(asset, metadata)

        has_clip = asset.attr_exists('clip')
        if not has_clip:
            # Since there is no clip, then set a clip, as all pages
            # need to have a clip.
            asset.set_attr('clip', Clip.page(1))

            if self.arg_value('extract_pages') and metadata.get('subimages'):
                self.extract_pages(frame, metadata)

    def set_date(self, document, metadata):
        """Extracts the date from the metadata and sets it on the document.

        Args:
            document(Document): Document to add date created information.
            metadata(dict): Metadata to parse a date from.

        """
        for field in self.date_fields:
            try:
                date_str = reduce(operator.getitem, field.split('.'), metadata)
            except (ValueError, KeyError, TypeError):
                continue
            date_str = date_str.replace('"', '')
            try:
                _datetime = datetime.strptime(date_str, '%Y:%m:%d %H:%M:%S')
            except ValueError:
                try:
                    _datetime = dateutil.parser.parse(date_str)
                except ValueError:
                    continue
            document.set_attr('media.timeCreated', _datetime.isoformat())
            break

    def set_location(self, document, metadata):
        """Extracts the location metadata and sets it as the media.latitude and
        media.longitude on the document.

        Args:
            document(Document): Document to add GPS location info to.
            metadata(dict): Metadata to parse for GPS location info.

        """
        gps_data = metadata.get('GPS', {})
        required_keys = ['LatitudeRef', 'Latitude', 'LongitudeRef', 'Longitude']
        if all(key in gps_data for key in required_keys):
            latitude = self._get_degrees_from_coordinates(gps_data['Latitude'])
            longitude = self._get_degrees_from_coordinates(gps_data['Longitude'])
            if gps_data['LatitudeRef'].lower() == 's':
                latitude *= -1
            if gps_data['LongitudeRef'].lower() == 'w':
                longitude *= -1
            if latitude and longitude:
                document.set_attr('media.latitude', latitude)
                document.set_attr('media.longitude', longitude)

    def _get_degrees_from_coordinates(self, coordinates):
        """Takes a coordinates string in the form "degrees, minutes, seconds" and returns
        it's decimal degrees.

        Args:
            coordinates(str): Coordinates string parsed from metadata.

        Returns:
            float: Decimal degree representation of the given coordinates.

        """
        if not coordinates:
            return 0
        if not isinstance(coordinates, (list, tuple)):
            coordinates = coordinates.split(',')
        if not len(coordinates) == 3:
            return 0
        degrees = float(coordinates[0])
        minutes = float(coordinates[1])
        seconds = float(coordinates[2])
        return degrees + (minutes / 60) + (seconds / 3600)

    def extract_pages(self, frame, metadata):
        """Extract pages from multi-page images. Each page is added as a derived frame.

        Args:
            frame(Frame): Parent to add derived frames to.
            metadata(dict): Metadata describing the parent image.
        """
        subimages = int(metadata.get('subimages'))
        source_asset = "asset:{}".format(frame.asset.id)
        for i in range(2, subimages + 1):
            clip = Clip.page(i)
            expand = ExpandFrame(FileImport(source_asset, clip=clip))
            self.expand(frame, expand)

    def set_metadata(self, document, metadata, namespace=None):
        """Based on the arguments given to the processor the necessary metadata is
        extracted and added to the given document.

        Args:
            document(Document): The document to add the metadata to.
            metadata(dict): Blob of metadata to extract information from.
            namespace(str): The parent namespace this metadata belongs. This
             is used when constructing the complete field name for each matadatum to
             determine if it should be extracted and allows for recursion into nested
             metadata sets.

        """
        included_tags = self.arg_value('included_tags')
        non_alphanum_regex = re.compile(r'[\W_]+')
        for key, value in metadata.items():
            clean_key = non_alphanum_regex.sub('', key)
            if namespace:
                field = '%s.%s' % (namespace, clean_key)
            else:
                field = clean_key
            if field in included_tags and self.arg_value('extract_extended_metadata'):
                document.set_attr('media.attrs.%s' % clean_key, value)

            if isinstance(value, dict):
                self.set_metadata(document, value, namespace=field)
        # Set the length of the image
        subimages = int(metadata.get('subimages', 1))
        document.set_attr('media.length', subimages)

    def set_media_type(self, asset):
        """
        Set media.type to image which signals that the Asset was processed as an image.

        Args:
            asset (Asset): The asset to fix.
        """
        asset.set_attr("media.type", "image")

    def add_keywords(self, document, item):
        """Adds the item to the asset as keywords. Simple function that allows for
        recursion in the case that the item is a dictionary.

        Args:
            document(Document): Document to add keywords to.
            item(str, dict): Item to add as keywords.

        """
        if isinstance(item, dict):
            for value in list(item.values()):
                self.add_keywords(document, value)
        else:
            document.add_keywords('media', str(item).split())
