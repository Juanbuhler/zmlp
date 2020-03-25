from rest_framework import status
from rest_framework.response import Response

from apikeys.serializers import ApikeySerializer
from apikeys.utils import create_zmlp_api_key
from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class ApikeyViewSet(BaseProjectViewSet):
    serializer_class = ApikeySerializer
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/auth/v1/apikey/'
    zmlp_only = True

    def list(self, request, project_pk):
        def item_filter(request, item):
            if item['name'].startswith('Admin Console Generated Key'):
                return False
            return True
        return self._zmlp_list_from_search(request, item_filter=item_filter)

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk)

    def create(self, request, project_pk):
        serializer = self.get_serializer(data=request.data)
        if not serializer.is_valid():
            return Response(status=status.HTTP_400_BAD_REQUEST, data=serializer.errors)

        apikey = create_zmlp_api_key(request.client, serializer.validated_data['name'],
                                     serializer.validated_data['permissions'], encode_b64=False)

        return Response(status=status.HTTP_201_CREATED, data=apikey)

    def destroy(self, request, project_pk, pk):
        return self._zmlp_destroy(request, pk)
