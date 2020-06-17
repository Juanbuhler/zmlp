from rest_framework.exceptions import ParseError

from wallet.exceptions import InvalidRequestError
from wallet.utils import convert_base64_to_json
from searches.schemas import (SimilarityAnalysisSchema, ContentAnalysisSchema,
                              LabelsAnalysisSchema, TYPE_FIELD_MAPPING)
from searches.filters import (ExistsFilter, FacetFilter, RangeFilter, LabelConfidenceFilter,
                              TextContentFilter, SimilarityFilter)

ANALYSIS_SCHEMAS = [SimilarityAnalysisSchema, ContentAnalysisSchema, LabelsAnalysisSchema]


class FieldUtility(object):

    def get_fields_from_mappings(self, mappings):
        """Converts an ES Indexes mappings response into a field:filter map."""
        fields = {}
        properties = mappings['properties']
        for property in properties:
            fields.update(self.get_filters_for_child_fields(property, properties[property]))
        return fields

    def get_filters_for_child_fields(self, property_name, values):
        """Recursively build the dict structure for an attribute and retrieve it's filters.

        Returns:
            <dict>: A dict of dicts where each key is part of the attriburte dot path,
            and the final value is the list of allowed filters.
        """
        fields = {property_name: {}}
        if 'type' in values:
            # May need to add an override for type on similarity hash fields
            return {property_name: TYPE_FIELD_MAPPING[values['type']]}

        if 'properties' in values:
            child_properties = values['properties']

            # Identify special Analysis Schemas
            if 'type' in child_properties:
                schema = self.get_analysis_schema(property_name, child_properties)
                if schema:
                    return schema

            for property in child_properties:
                fields[property_name].update(
                    self.get_filters_for_child_fields(property, child_properties[property]))

        return fields

    def get_analysis_schema(self, property_name, child_properties):
        """Return the special schema for a ZMLP Analysis Schema"""
        for Klass in ANALYSIS_SCHEMAS:
            schema = Klass(property_name, child_properties)
            if schema.is_valid():
                return schema.get_representation()

        return None


class FilterBoy(object):

    filters = [ExistsFilter,
               FacetFilter,
               RangeFilter,
               LabelConfidenceFilter,
               TextContentFilter,
               SimilarityFilter]

    def get_filter_from_request(self, request):
        """Gets Filter object from a requests querystring.

        Pulls the `filter` querystring value, decodes it, and returns the native
        Wallet object to represent that filter.

        Args:
            request: The initial request object

        Returns:
            Filter: Wallet Filter Representation of the querystring data.

        Raises:
            ParseError: If the querystring is undecodeable
            InvalidRequestError: If no `filter` argument is included in the querystring.
        """
        try:
            encoded_filter = request.query_params['filter']
        except KeyError:
            raise InvalidRequestError(detail='No `filter` querystring included.')

        try:
            decoded_filter = convert_base64_to_json(encoded_filter)
        except ValueError:
            raise ParseError(detail='Unable to decode `filter` querystring.')

        return self.get_filter_from_json(decoded_filter, request.app)

    def get_filters_from_request(self, request):
        """Gets the list of Filters from a request querystring.

        Pulls the `query` querystring value, decodes it, and returns a list of the
        native Wallet objects that represent that query.

        Args:
            request: The initial request object

        Returns:
            list<Filter>: List of Wallet Filters from the querystring data.

        Raises:
            ParseError: If the querystring is undecodeable
        """
        try:
            encoded_query = request.query_params['query']
        except KeyError:
            return []

        if not encoded_query:
            # Catches a blank `query=` querystring
            return []

        try:
            converted_query = convert_base64_to_json(encoded_query)
        except ValueError:
            raise ParseError(detail='Unable to decode `query` querystring.')

        filters = []
        for raw_filter in converted_query:
            filters.append(self.get_filter_from_json(raw_filter, request.app))
        return filters

    def get_filter_from_json(self, raw_filter, zmlp_app):
        """Converts a raw filter dict into native Wallet object.

        Args:
            raw_filter: The raw JSON data that represents the Filter
            zmlp_app(ZmlpApp): ZMLP App object to pass to the instantiated filter.

        Returns:
            Filter: Wallet Filter representation of the raw data.

        Raises:
            ParseError: If the requested Filter type is unknown or missing.
        """

        try:
            filter_type = raw_filter['type']
        except KeyError:
            raise ParseError(detail='Filter description is missing a `type`.')
        except TypeError:
            raise ParseError(detail='Filter format incorrect, did not receive a single '
                                    'JSON object for the Filter.')

        Filter = None
        for _filter in self.filters:
            if _filter.type == filter_type:
                Filter = _filter
                continue

        if not Filter:
            raise ParseError(detail=f'Unsupported filter `{filter_type}` given.')

        return Filter(raw_filter, zmlp_app)

    def reduce_filters_to_query(self, filters):
        """Takes a list of Filters and combines their separate queries into one."""
        query = {}
        for _filter in filters:
            query = _filter.add_to_query(query)
        return query
