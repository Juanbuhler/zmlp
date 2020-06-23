from rest_framework.exceptions import ParseError

from wallet.exceptions import InvalidRequestError
from wallet.utils import convert_base64_to_json
from .visualizations import RangeVisualization, FacetVisualization


class VizBuddy(object):

    visualizations = [RangeVisualization,
                      FacetVisualization]

    def __init__(self, filter_query):
        self.filter_query = filter_query

    def get_visualizations_from_request(self, request):
        try:
            encoded_visualizations = request.query_params['visuals']
        except KeyError:
            raise InvalidRequestError(detail='No `visuals` query param included.')

        try:
            converted_visualizations = convert_base64_to_json(encoded_visualizations)
        except ValueError:
            raise ParseError(detail='Unable to decode `visuals` query param.')

        visualizations = []
        for raw_visual in converted_visualizations:
            visualizations.append(self.get_visualization_from_json(raw_visual, request.app))
        return visualizations

    def get_visualization_from_json(self, raw_visual, zmlp_app=None):
        try:
            visualization_type = raw_visual['type']
        except KeyError:
            raise ParseError(detail='Visualization description is missing a `type`.')
        except TypeError:
            raise ParseError(detail='Visualization format incorrect, did not receive a '
                                    'single JSON object for the Visualization.')

        Visualization = None
        for klass in self.visualizations:
            if klass.type == visualization_type:
                Visualization = klass
                continue

        if not Visualization:
            raise ParseError(detail=f'Unsupported Visualization type `{visualization_type}` given.')

        return Visualization(raw_visual, zmlp_app)

    def reduce_visualizations_to_query(self, visualizations):
        # TODO: Figure out how to combine these darn things
        queries = []
        for visualization in visualizations:
            queries.append(visualization.get_query())
        return queries