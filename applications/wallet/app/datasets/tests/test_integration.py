from django.urls import reverse
from boonsdk.client import BoonClient, BoonSdkNotFoundException
from rest_framework import status

from wallet.tests.utils import check_response


class TestDatasetsViewsets:

    def test_list(self, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'list': [{'id': 'ed756d9e-0106-1fb2-adab-0242ac12000e', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Testingerer', 'type': 'Classification', 'description': 'My second testing dataset.', 'modelCount': 0, 'timeCreated': 1622668587793, 'timeModified': 1622668587793, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}, {'id': 'ed756d9d-0106-1fb2-adab-0242ac12000e', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Testing', 'type': 'Classification', 'description': 'My first testing dataset.', 'modelCount': 0, 'timeCreated': 1622668561506, 'timeModified': 1622668561506, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}], 'page': {'from': 0, 'size': 20, 'disabled': False, 'totalCount': 2}}  # noqa

        monkeypatch.setattr(BoonClient, 'post', mock_response)
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        response = api_client.get(path)
        content = check_response(response)
        assert content['count'] == 2
        item = content['results'][0]
        assert item['name'] == 'Testingerer'
        assert item['type'] == 'Classification'

    def test_detail(self, login, api_client, project, monkeypatch):
        dataset_id = 'ed756d9e-0106-1fb2-adab-0242ac12000e'

        def mock_response(*args, **kwargs):
            return {'id': dataset_id, 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Testingerer', 'type': 'Classification', 'description': 'My second testing dataset.', 'modelCount': 0, 'timeCreated': 1622668587793, 'timeModified': 1622668587793, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}  # noqa

        monkeypatch.setattr(BoonClient, 'get', mock_response)
        path = reverse('dataset-detail', kwargs={'project_pk': project.id,
                                                 'pk': 'ed756d9e-0106-1fb2-adab-0242ac12000e'})
        response = api_client.get(path)
        content = check_response(response)
        assert content['id'] == dataset_id
        assert content['name'] == 'Testingerer'

    def test_create(self, login, api_client, project, monkeypatch):

        def mock_response(*args, **kwargs):
            return {'id': '274179cc-2122-167c-9a8a-0242ac12000c', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'My New Dataset', 'type': 'Detection', 'description': 'My detection dataset.', 'modelCount': 0, 'timeCreated': 1622748735068, 'timeModified': 1622748735068, 'actorCreated': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000', 'actorModified': 'd9140c2b-64ed-48b7-a7ee-d164821b4c50/Admin Console Generated Key - a97c8d61-b839-4600-8135-25051b9da0bc - software@zorroa.com_00000000-0000-0000-0000-000000000000'}  # noqa

        monkeypatch.setattr(BoonClient, 'post', mock_response)
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'projectId': project.id,
                'name': 'My New Dataset',
                'type': 'Detection',
                'description': 'My detection dataset.'}
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_201_CREATED)
        assert content['id'] == '274179cc-2122-167c-9a8a-0242ac12000c'
        assert content['name'] == 'My New Dataset'
        assert content['type'] == 'Detection'

    def test_create_wrong_project_id(self, login, api_client, project, monkeypatch):
        wrong_id = '00000000-0000-0000-0000-000000000011'

        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'projectId': wrong_id,
                'name': 'My New Dataset',
                'type': 'Detection',
                'description': 'My detection dataset.'}
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['detail'] == ['Invalid request. You can only create datasets '
                                     'for the current project context.']

    def test_create_missing_args(self, login, api_client, project, monkeypatch):
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'projectId': project.id,
                'type': 'Detection',
                'description': 'My detection dataset.'}
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_400_BAD_REQUEST)
        assert content['name'] == ['This field is required.']

    def test_create_too_many_args(self, login, api_client, project, monkeypatch):
        path = reverse('dataset-list', kwargs={'project_pk': project.id})
        body = {'projectId': project.id,
                'name': 'My New Dataset',
                'type': 'Detection',
                'description': 'My detection dataset.',
                'modelCount': 10}  # Additional field
        response = api_client.post(path, body)
        content = check_response(response, status=status.HTTP_403_FORBIDDEN)
        assert content['detail'] == 'You do not have permission to perform this action.'

    def test_delete(self, login, api_client, project, monkeypatch):
        dataset_id = 'ed756d9d-0106-1fb2-adab-0242ac12000e'

        def mock_response(*args, **kwargs):
            return {'type': 'dataSet', 'id': 'ed756d9d-0106-1fb2-adab-0242ac12000e', 'op': 'delete', 'success': True}  # noqa

        path = reverse('dataset-detail', kwargs={'project_pk': project.id,
                                                 'pk': dataset_id})
        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        response = api_client.delete(path)
        content = check_response(response)
        assert content['detail'] == ['Successfully deleted resource.']

    def test_delete_different_project(self, login, api_client, project, project2, zmlp_project_membership,
                                      zmlp_project2_membership, monkeypatch):
        dataset_id = 'ed756d9d-0106-1fb2-adab-0242ac12000e'
        wrong_project_id = project2.id

        def mock_response(*args, **kwargs):
            raise BoonSdkNotFoundException({'message': f'The Dataset {dataset_id} does not exist'})

        path = reverse('dataset-detail', kwargs={'project_pk': wrong_project_id,
                                                 'pk': dataset_id})
        monkeypatch.setattr(BoonClient, 'delete', mock_response)
        response = api_client.delete(path)
        content = check_response(response, status=status.HTTP_404_NOT_FOUND)
        assert content['detail'] == ['Not found.']

    # TODO: Put back in once updating Datasets is supported
    # def test_update(self, login, api_client, project, monkeypatch):
    #     pass
    #
    # def test_partial_update(self, login, api_client, project, monkeypatch):
    #     pass
    #
    # def test_update_type_not_allowed(self, login, api_client, project, monkeypatch):
    #     pass
