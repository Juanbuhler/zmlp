import logging

from rest_framework.exceptions import ParseError

from wallet.exceptions import InvalidRequestError
from wallet.utils import convert_base64_to_json
from searches.schemas import (SimilarityAnalysisSchema, ContentAnalysisSchema,
                              LabelsAnalysisSchema, FIELD_TYPE_FILTER_MAPPING)
from searches.filters import (ExistsFilter, FacetFilter, RangeFilter, LabelConfidenceFilter,
                              TextContentFilter, SimilarityFilter, LabelFilter, DateFilter,
                              PredictionCountFilter, LimitFilter, SimpleSortFilter,
                              LabelsExistFilter)


ANALYSIS_SCHEMAS = [SimilarityAnalysisSchema, ContentAnalysisSchema, LabelsAnalysisSchema]
logger = logging.getLogger(__name__)


class FieldUtility(object):

    @property
    def utility_fields(self):
        return {'utility': {'Search Results Limit': ['limit']}}

    def get_filter_map(self, client=None):
        """Returns the list of fields and their valid filters."""
        field_types = self.get_field_type_map(client)
        fields = self._get_child_filters_from_field_types(field_types)
        return self._add_utility_fields(fields)

    def _get_child_filters_from_field_types(self, field_types):
        """Recursive helper to parse the list of fields and convert fieldTypes to filters."""
        fields = {}
        for field in field_types:
            if 'fieldType' in field_types[field]:
                try:
                    fields[field] = FIELD_TYPE_FILTER_MAPPING[field_types[field]['fieldType']]
                except KeyError:
                    fields[field] = FIELD_TYPE_FILTER_MAPPING['object']
            else:
                fields.update({field: self._get_child_filters_from_field_types(field_types[field])})
        return fields

    def get_field_type_map(self, client=None):
        """Converts an ES Indexes mappings response into a field:fieldType map."""
        fields = {}
        path = 'api/v3/fields/_mapping'
        content = client.get(path)
        indexes = list(content.keys())
        if len(indexes) != 1:
            raise ValueError('ZMLP did not return field mappings as expected.')

        index = indexes[0]
        properties = content[index]['mappings']['properties']
        for property in properties:
            if property == 'labels':
                fields.update(self._get_labels_field_types(property, properties[property],
                                                           client=client))
            else:
                fields.update(self._get_field_types_for_child_fields(property, properties[property],
                                                                     client=client))
        return fields

    def _get_field_types_for_child_fields(self, property_name, values, client=None):
        """Recursive helper to walk a field mapping and determine the field types."""
        fields = {property_name: {}}
        if 'type' in values:
            return {property_name: {'fieldType': values['type']}}

        if 'properties' in values:
            child_properties = values['properties']

            # Identify special Analysis Schemas
            if 'type' in child_properties:
                schema = self._get_analysis_schema(property_name, child_properties)
                if schema:
                    return schema.get_field_type_representation()

            for property in child_properties:
                fields[property_name].update(
                    self._get_field_types_for_child_fields(property, child_properties[property]))

        return fields

    def _get_labels_field_types(self, property_name, child_properties, client=None):
        """Adds labels to the field type map and sets the field type for each."""
        if not client:
            logger.warning('No Client provided. Unable to retrieve datasets.')
            return {'labels': {}}
        dataset_key_names = self._get_all_dataset_ids(client)
        dataset_ids = {}
        for dataset_id in dataset_key_names:
            dataset_ids[dataset_id] = {'fieldType': 'label'}
        return {'labels': dataset_ids}

    def _get_analysis_schema(self, property_name, child_properties):
        """Return the special schema for a ZMLP Analysis Schema"""
        for Klass in ANALYSIS_SCHEMAS:
            schema = Klass(property_name, child_properties)
            if schema.is_valid():
                return schema

        return None

    def _get_all_dataset_ids(self, client):
        path = '/api/v3/datasets/_search'
        response = client.post(path, {})
        names = []
        for dataset in response.get('list', []):
            names.append(dataset['id'])
        return names

    def get_attribute_field_type(self, attribute, client):
        """Given an attribute in dot path form, return it's field type.

        Returns:
            (str): The FieldType of the given field

        Raises:
            (ParseError): If the given attribute type can't be found or is not in the mapping.
        """
        field_type_map = self.get_field_type_map(client)
        current_level = field_type_map
        for level in attribute.split('.'):
            if level not in current_level:
                raise ParseError(detail=['Given attribute could not be found in field mapping.'])
            if 'fieldType' in current_level[level]:
                return current_level[level]['fieldType']
            else:
                current_level = current_level[level]

        raise ParseError(detail=['Attribute given is not a valid filterable or visualizable field.'])

    def _add_utility_fields(self, fields):
        """Adds any one-off utility fields to the field mapping so the UI can display them properly.

        Args:
            fields (dict): Current set of determined fields from the ES Field Mapping.

        Returns:
            (dict): All fields plus the additional utility fields.
        """
        for field in self.utility_fields:
            fields[field] = self.utility_fields[field]
        return fields


class FilterBuddy(object):

    filters = [ExistsFilter,
               FacetFilter,
               RangeFilter,
               LabelConfidenceFilter,
               TextContentFilter,
               SimilarityFilter,
               LabelFilter,
               DateFilter,
               PredictionCountFilter,
               LimitFilter,
               SimpleSortFilter,
               LabelsExistFilter]

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
            raise InvalidRequestError(detail={'detail': ['No `filter` query param included.']})

        try:
            decoded_filter = convert_base64_to_json(encoded_filter)
        except ValueError:
            raise ParseError(detail={'detail': ['Unable to decode `filter` query param.']})

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
            raise ParseError(detail={'detail': ['Unable to decode `query` query param.']})

        filters = []
        for raw_filter in converted_query:
            filters.append(self.get_filter_from_json(raw_filter, request))
        return filters

    def get_filter_from_json(self, raw_filter, request=None):
        """Converts a raw filter dict into native Wallet object.

        Args:
            raw_filter: The raw JSON data that represents the Filter
            request (Request): DRF Request object to pass to the instantiated filter.

        Returns:
            Filter: Wallet Filter representation of the raw data.

        Raises:
            ParseError: If the requested Filter type is unknown or missing.
        """

        try:
            filter_type = raw_filter['type']
        except KeyError:
            raise ParseError(detail={'detail': ['Filter description is missing a `type`.']})
        except TypeError:
            raise ParseError(detail={'detail': ['Filter format incorrect, did not receive a '
                                                'single JSON object for the Filter.']})

        Filter = None
        for _filter in self.filters:
            if _filter.type == filter_type:
                Filter = _filter
                continue

        if not Filter:
            raise ParseError(detail={'detail': [f'Unsupported filter `{filter_type}` given.']})

        return Filter(raw_filter, request)

    def reduce_filters_to_query(self, filters, request):
        """Takes a list of Filters and combines their separate queries into one."""
        query = {}
        for _filter in filters:
            query = _filter.add_to_query(query, request)
        return query

    def reduce_filters_to_clip_query(self, filters):
        """Takes a list of Filters and combines their clip queries into one."""
        query = {}
        for _filter in filters:
            query = _filter.add_to_clip_query(query)
        return query

    def validate_filters(self, filters):
        """Ensures every filter was valid, while also catching the Limit filter and
        setting the max_assets value on the overall request object."""
        for _filter in filters:
            _filter.is_valid(query=True, raise_exception=True)

    def finalize_query_from_filters_and_request(self, filters, request):
        """Converts a list of filters and request into a usable query with updated request.

        Validates given filters, builds an ES query from them, and updates the given request
        with additional attributes for certain filters that affect it.

        Args:
            filters (list): Set of Filter objects to convert to a query
            request (Request): Current request object to modify, if needed

        Raises:
            ValidationError: If any given filters are not properly configured

        Returns:
            (dict): The final ES query generated from the given filters
        """
        self.validate_filters(filters)
        query = self.reduce_filters_to_query(filters, request)

        # If there's no specific query at this point, we sort by the created date
        # to make the visual display in Visualizer more useful
        if not query:
            query['sort'] = {'system.timeCreated': {'order': 'desc'}}

        return query
