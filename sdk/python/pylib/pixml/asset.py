import logging

from .util import as_collection
from pixml.analysis.storage import get_lfs

__all__ = [
    "Asset",
    "AssetSpec"
]

logger = logging.getLogger(__name__)


class BaseAsset(object):

    def __init__(self):
        self.document = {}

    def set_attr(self, attr, value):
        """Set the value of an attribute.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            value (:obj:`object`): value: The value for the particular
                attribute. Can be any json serializable type.
        """
        self.__set_attr(attr, value)

    def del_attr(self, attr):
        """
        Delete the attribute from the document.  If the attribute does not exist
        or is protected by a manual field edit then return false.  Otherwise,
        delete the attribute and return true.

        Args:
            attr (str): The attribute name.

        Returns:
            bool: True if the attribute was deleted.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:-1]:
            if not isinstance(doc, dict) or k not in doc:
                return False
            doc = doc.get(k)

        attr_name = parts[-1]
        try:
            del doc[attr_name]
            return not self.attr_exists(attr)
        except KeyError:
            return False

    def get_attr(self, attr, default=None):
        """Get the given attribute to the specified value.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            default (:obj:`mixed`) The default value if no attr exists.

        Returns:
            mixed: The value of the attribute.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts:
            if not isinstance(doc, dict) or k not in doc:
                return default
            doc = doc.get(k)
        return doc

    def attr_exists(self, attr):
        """
        Return true if the given attribute exists.

        Args:
            attr (str): The name of the attribute to check.

        Returns:
            bool: true if the attr exists.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:len(parts)-1]:
            if k not in doc:
                return False
            doc = doc.get(k)
        return parts[-1] in doc

    def add_analysis(self, id, val):
        """Add an analysis structure to the document.

        Args:
            id (str): The name of the analysis
            val (mixed): the value/result of the analysis.

        """
        if not id or not val:
            raise ValueError("Analysis requires a unique ID and value")
        attr = "analysis.%s" % id
        self.set_attr(attr, val)

    def extend_list_attr(self, attr, items):
        """
        Adds the given items to the given attr. The attr must be a list or set.

        Args:
            attr (str): The name of the attribute
            items (:obj:`list` of :obj:`mixed`): A list of new elements.

        """
        items = as_collection(items)
        all_items = self.get_attr(attr)
        if all_items is None:
            all_items = set()
            self.set_attr(attr, all_items)
        try:
            all_items.update(items)
        except AttributeError:
            all_items.extend(items)

    def get_local_path(self):
        lfs = get_lfs()
        return lfs.localize_uri(self.get_attr("source.path"))

    def __set_attr(self, attr, value):
        """
        A private set_attr method that handles just the setting of the
        attribute without any field edit protection.

        This gets called from set_attr to avoid infinite looping.

        Args:
            attr (str): The attribute name in dot notation format.
                ex: 'foo.bar'
            value (:obj:`object`): value: The value for the particular
                attribute.  Can be any json serializable type.

        """
        doc = self.document
        parts = attr.split(".")
        for k in parts[0:len(parts)-1]:
            if k not in doc:
                doc[k] = {}
            doc = doc[k]
        if isinstance(value, dict):
            doc[parts[-1]] = value
        else:
            try:
                doc[parts[-1]] = value.for_json()
            except AttributeError:
                doc[parts[-1]] = value


class AssetSpec(BaseAsset):
    """

    """
    def __init__(self, uri):
        super(AssetSpec, self).__init__()
        self.uri = uri

    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "uri": self.uri,
            "document": self.document
        }


class Asset(BaseAsset):

    def __init__(self, data):
        super(Asset, self).__init__()
        """

        Args:
            id:
            document:
        """
        if not data:
            raise ValueError("Error creating Asset instance, Assets must have an id.")
        self.id = data.get("id")
        self.document = data.get("document")

    @property
    def uri(self):
        return self.get_attr("source.path")


    def for_json(self):
        """Returns a dictionary suitable for JSON encoding.

        The ZpsJsonEncoder will call this method automatically.

        Returns:
            :obj:`dict`: A JSON serializable version of this Document.

        """
        return {
            "id": self.id,
            "document": self.document
        }
