import base64
import copy
from uuid import uuid4

import pytest
from django.core.exceptions import ValidationError
from django.http import JsonResponse, HttpResponseForbidden, Http404
from django.test import RequestFactory, override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.response import Response
from zmlp import ZmlpClient
from zmlp.client import (ZmlpDuplicateException, ZmlpInvalidRequestException,
                         ZmlpNotFoundException)

from projects.models import Project, Membership
from projects.serializers import ProjectSerializer
from projects.utils import random_project_name
from projects.views import BaseProjectViewSet
from subscriptions.models import Tier
from wallet.utils import convert_base64_to_json, convert_json_to_base64

pytestmark = pytest.mark.django_db


def mock_put_disable_project(*args, **kwargs):
    return {'type': 'project', 'id': '00000000-0000-0000-0000-000000000000', 'op': 'disable', 'success': True}  # noqa


def mock_put_enable_project(*args, **kwargs):
    return {'type': 'project', 'id': '00000000-0000-0000-0000-000000000000', 'op': 'enable', 'success': True}  # noqa


def test_random_name():
    assert random_project_name()


def test_project_with_random_name():
    project = Project.objects.create()
    assert project.name


def test_project_view_user_does_not_belong_to_project(user, project):
    class FakeViewSet(BaseProjectViewSet):
        def get(self, request, project):
            return JsonResponse({'success': True})
    request = RequestFactory().get('/fake-path')
    request.user = user
    view = FakeViewSet()
    view.request = request
    view.args = []
    view.kwargs = {'project_pk': project.id}
    response = view.dispatch(view.request, *view.args, **view.kwargs)
    assert type(response) == HttpResponseForbidden
    assert response.content == (b'user is not a member of the project '
                                b'6abc33f0-4acf-4196-95ff-4cbb7f640a06')


@override_settings(PLATFORM='zvi')
def test_zmlp_only_flag(user, project):

    class FakeViewSet(BaseProjectViewSet):
        zmlp_only = True

        def get(self, request, project):
            return JsonResponse({'success': True})

    request = RequestFactory().get('/fake-path')
    request.user = user
    view = FakeViewSet()
    view.request = request
    view.args = []
    view.kwargs = {'project_pk': project.id}
    with pytest.raises(Http404):
        view.dispatch(view.request, *view.args, **view.kwargs)


def test_projects_view_no_projects(project, user, api_client):
    api_client.force_authenticate(user)
    response = api_client.get(reverse('project-list')).json()
    assert response['count'] == 0


def test_projects_view_with_projects(project, zmlp_project_user, api_client):
    api_client.force_authenticate(zmlp_project_user)
    response = api_client.get(reverse('project-list')).json()
    assert response['count'] == 1
    assert response['results'][0]['name'] == project.name


def test_projects_view_inactive_projects(project, zmlp_project_user, api_client):
    api_client.force_authenticate(zmlp_project_user)
    project.is_active = False
    project.save()
    response = api_client.get(reverse('project-list')).json()
    assert response['count'] == 0


def test_project_serializer_detail(project):
    serializer = ProjectSerializer(project, context={'request': None})
    data = serializer.data
    expected_fields = ['id', 'name', 'url', 'jobs', 'apikeys', 'assets', 'users', 'roles',
                       'permissions', 'tasks', 'datasources', 'taskerrors', 'subscriptions',
                       'modules', 'providers', 'searches', 'export', 'faces', 'visualizations',
                       'models']
    assert set(expected_fields) == set(data.keys())
    assert data['id'] == project.id
    assert data['name'] == project.name
    assert data['url'] == f'/api/v1/projects/{project.id}/'
    assert data['jobs'] == f'/api/v1/projects/{project.id}/jobs/'
    assert data['users'] == f'/api/v1/projects/{project.id}/users/'
    assert data['roles'] == f'/api/v1/projects/{project.id}/roles/'
    assert data['assets'] == f'/api/v1/projects/{project.id}/assets/'
    assert data['datasources'] == f'/api/v1/projects/{project.id}/data_sources/'
    assert data['apikeys'] == f'/api/v1/projects/{project.id}/api_keys/'
    assert data['permissions'] == f'/api/v1/projects/{project.id}/permissions/'
    assert data['tasks'] == f'/api/v1/projects/{project.id}/tasks/'
    assert data['taskerrors'] == f'/api/v1/projects/{project.id}/task_errors/'
    assert data['subscriptions'] == f'/api/v1/projects/{project.id}/subscriptions/'
    assert data['modules'] == f'/api/v1/projects/{project.id}/modules/'
    assert data['providers'] == f'/api/v1/projects/{project.id}/providers/'
    assert data['searches'] == f'/api/v1/projects/{project.id}/searches/'
    assert data['export'] == f'/api/v1/projects/{project.id}/searches/export/'
    assert data['faces'] == f'/api/v1/projects/{project.id}/faces/'
    assert data['visualizations'] == f'/api/v1/projects/{project.id}/visualizations/'
    assert data['models'] == f'/api/v1/projects/{project.id}/models/'


def test_project_serializer_list(project, project2):
    queryset = Project.objects.all()
    serializer = ProjectSerializer(queryset, many=True, context={'request': None})
    data = serializer.data
    assert isinstance(data, list)
    assert len(data) == 2
    assert [entry['id'] for entry in data] == [project.id, project2.id]


def test_project_sync_with_zmlp(monkeypatch, project_zero_user):
    def mock_get_project(*args, **kwargs):
        return {'id': '00000000-0000-0000-0000-000000000000', 'name': 'test', 'timeCreated': 1590092156428, 'timeModified': 1593626053685, 'actorCreated': 'f3bd2541-428d-442b-8a17-e401e5e76d06/admin-key', 'actorModified': 'f3bd2541-428d-442b-8a17-e401e5e76d06/admin-key', 'enabled': True, 'tier': 'ESSENTIALS'}  # noqa

    def mock_get_project_exists(*args, **kwargs):
        raise ZmlpNotFoundException({})

    def mock_post_true(*args, **kwargs):
        return True

    def mock_post_duplicate(*args, **kwargs):
        raise ZmlpDuplicateException({})

    def mock_post_exception(*args, **kwargs):
        raise KeyError('')

    def mock_put_failed_enable(*args, **kwargs):
        return {'type': 'project', 'id': '00000000-0000-0000-0000-000000000000', 'op': 'enable',
                'success': False}

    # Test a successful sync.
    monkeypatch.setattr(ZmlpClient, 'get', mock_get_project)
    monkeypatch.setattr(ZmlpClient, 'post', mock_post_true)
    monkeypatch.setattr(ZmlpClient, 'put', mock_put_enable_project)
    project = Project.objects.create(name='test', id=uuid4())
    project.sync_with_zmlp()

    # Test a disabled project.
    project.is_active = False
    project.save()
    monkeypatch.setattr(ZmlpClient, 'put', mock_put_enable_project)
    project.sync_with_zmlp()

    # Test a sync when the project already exists in zmlp.
    monkeypatch.setattr(ZmlpClient, 'get', mock_get_project_exists)
    monkeypatch.setattr(ZmlpClient, 'post', mock_get_project)
    project.sync_with_zmlp()

    # Test failed status sync.
    monkeypatch.setattr(ZmlpClient, 'put', mock_put_failed_enable)
    with pytest.raises(IOError):
        project.sync_with_zmlp()


def test_project_sync_with_zmlp_with_subscription(monkeypatch, project_zero_user,
                                                  project_zero_subscription, project_zero):
    def mock_get_project(*args, **kwargs):
        return {'id': '00000000-0000-0000-0000-000000000000', 'name': 'test', 'timeCreated': 1590092156428, 'timeModified': 1593626053685, 'actorCreated': 'f3bd2541-428d-442b-8a17-e401e5e76d06/admin-key', 'actorModified': 'f3bd2541-428d-442b-8a17-e401e5e76d06/admin-key', 'enabled': True, 'tier': 'ESSENTIALS'}  # noqa

    def mock_post_true(*args, **kwargs):
        return True

    def mock_put_tier(*args, **kwargs):
        return {'id': '00000000-0000-0000-0000-000000000000', 'name': 'test', 'timeCreated': 1590092156428, 'timeModified': 1593626053685, 'actorCreated': 'f3bd2541-428d-442b-8a17-e401e5e76d06/admin-key', 'actorModified': 'f3bd2541-428d-442b-8a17-e401e5e76d06/admin-key', 'enabled': True, 'tier': 'PREMIER'}  # noqa

    # Test a successful sync.
    monkeypatch.setattr(ZmlpClient, 'get', mock_get_project)
    monkeypatch.setattr(ZmlpClient, 'post', mock_post_true)
    monkeypatch.setattr(ZmlpClient, 'put', mock_put_tier)
    project_zero_subscription.tier = Tier.PREMIER
    project_zero_subscription.save()
    monkeypatch.setattr(ZmlpClient, 'put', mock_put_enable_project)
    project_zero.sync_with_zmlp()


def test_project_managers(project):
    assert Project.objects.all().count() == 1
    assert str(project.id) == str(Project.objects.first().id)
    project.is_active = False
    project.save()
    assert Project.objects.all().count() == 0
    assert Project.all_objects.all().count() == 1


@pytest.fixture
def data(project):
    return {
        'id': 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f',
        'name': 'tester@fake.com',
        'projectId': project.id,
        'accessKey': 'P1klR1U1RgT3YfdLYN4-AHPlnOhXZHeD',
        'secretKey': '6Ti7kZZ7IcmWnR1bfdvCMUataoMh9Mbq9Kqvs3xctOM7y1OwbefdFiLewuEDAGBof_lV5y_JKuFtY11bmRjFEg',  # noqa
        'permissions': ['AssetsRead']
    }


@pytest.fixture
def inception_key(project):
    return {
        'id': 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba333',
        'name': 'admin-key',
        'projectId': '00000000-0000-0000-0000-000000000000',
        'accessKey': 'P1klR1U1RgT3YfdLYN4-AHPlnOhXZHeD',
        'secretKey': '6Ti7kZZ7IcmWnR1bfdvCMUataoMh9Mbq9Kqvs3xctOM7y1OwbefdFiLewuEDAGBof_lV5y_JKuFtY11bmRjFEg',  # noqa
        'permissions': ['AssetsRead']
    }


@pytest.fixture
def api_key():
    return {'accessKey': 'P1klR1U1RgT3YfdLYN4-AHPlnOhXZHeD',
            'secretKey': '6Ti7kZZ7IcmWnR1bfdvCMUataoMh9Mbq9Kqvs'}


def make_users_for_project(project, count, user_model, apikey):
    users = []
    for index in range(0, count):
        username = f'user_{index}'
        user = user_model.objects.create_user(username, f'{username}@fake.com', 'letmein')  # noqa
        Membership.objects.create(user=user, project=project,
                                  apikey=base64.b64encode(apikey).decode('utf-8'))
        users.append(user)
    return users


class TestProjectUserGet:

    @override_settings(PLATFORM='zmlp')
    def test_list(self, zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_membership.user)
        api_client.force_login(zmlp_project_membership.user)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('projectuser-list', kwargs={'project_pk': project_pk}))
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['results'][0]['id'] == zmlp_project_membership.user.id
        assert content['results'][0]['roles'] == ['ML_Tools', 'User_Admin']

    def test_list_no_permissions(self, zmlp_project_membership, api_client):
        zmlp_project_membership.roles = []
        zmlp_project_membership.save()
        api_client.force_authenticate(zmlp_project_membership.user)
        api_client.force_login(zmlp_project_membership.user)
        project_pk = zmlp_project_membership.project_id
        response = api_client.get(reverse('projectuser-list', kwargs={'project_pk': project_pk}))
        assert response.status_code == status.HTTP_403_FORBIDDEN
        assert response.json() == {'detail': 'You do not have permission to manage users.'}

    @override_settings(PLATFORM='zmlp')
    def test_paginated_list(self, project, zmlp_project_user, zmlp_project_membership,
                            api_client, django_user_model, zmlp_apikey):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        make_users_for_project(project, 5, django_user_model, zmlp_apikey)
        uri = reverse('projectuser-list', kwargs={'project_pk': project.id})
        response = api_client.get(f'{uri}?from=0&size=2')
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['count'] == 6
        assert len(content['results']) == 2
        assert 'next' in content
        assert content['next'] is not None
        assert 'previous' in content

    @override_settings(PLATFORM='zmlp')
    def test_list_bad_project(self, project, zmlp_project_user, zmlp_project_membership, api_client):  # noqa
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        new_project = Project.objects.create(id='0820a307-c3dd-460e-a9c4-0e5f582e09c3',
                                             name='Test Project')
        response = api_client.get(reverse('projectuser-list', kwargs={'project_pk': new_project.id}))  # noqa
        assert response.status_code == status.HTTP_403_FORBIDDEN

    @override_settings(PLATFORM='zmlp')
    def test_retrieve(self, project, zmlp_project_user, zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': zmlp_project_user.id}))
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['id'] == zmlp_project_user.id
        assert content['username'] == zmlp_project_user.username
        assert content['permissions'] == ['SuperAdmin', 'ProjectAdmin',
                                          'AssetsRead', 'AssetsImport']
        assert content['roles'] == ['ML_Tools', 'User_Admin']

    @override_settings(PLATFORM='zmlp')
    def test_with_bad_apikey(self, project, zmlp_project_user, zmlp_project_membership,
                             api_client, monkeypatch):
        monkeypatch.setattr(ZmlpClient, '_ZmlpClient__load_apikey', lambda x, y: {})
        zmlp_project_membership.apikey = 'no good'
        zmlp_project_membership.save()
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': zmlp_project_user.id}))
        assert response.status_code == status.HTTP_200_OK
        content = response.json()
        assert content['permissions'] == []

    @override_settings(PLATFORM='zmlp')
    def test_retrieve_bad_user_pk(self, project, zmlp_project_user, zmlp_project_membership,
                                  api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': 9999}))
        assert response.status_code == status.HTTP_404_NOT_FOUND
        content = response.json()
        assert content['detail'] == 'Not found.'

    @override_settings(PLATFORM='zmlp')
    def test_retrieve_non_member_user(self, project, zmlp_project_user, zmlp_project_membership,
                                      api_client, django_user_model):
        user = django_user_model.objects.create_user('newGuy', 'newGuy@fake.com', 'letmein')
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.get(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': user.id}))
        assert response.status_code == status.HTTP_404_NOT_FOUND
        content = response.json()
        assert content['detail'] == 'Not found.'


class TestProjectUserDelete:

    @override_settings(PLATFORM='zmlp')
    def test_destroy(self, project, zmlp_project_user, zmlp_project_membership, api_client,
                     monkeypatch, django_user_model, zmlp_apikey):

        def mock_return(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        monkeypatch.setattr(ZmlpClient, 'delete', mock_return)
        user = make_users_for_project(project, 1, django_user_model, zmlp_apikey)[0]
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_200_OK
        with pytest.raises(Membership.DoesNotExist):
            user.memberships.get(project=project.id)

    @override_settings(PLATFORM='zmlp')
    def test_non_member_user(self, project, zmlp_project_user, zmlp_project_membership,
                             api_client, django_user_model):
        user = django_user_model.objects.create_user('newGuy', 'newGuy@fake.com', 'letmein')
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_404_NOT_FOUND
        content = response.json()
        assert content['detail'] == 'Not found.'

    @override_settings(PLATFORM='zmlp')
    def test_bad_apikey(self, project, zmlp_project_user, zmlp_project_membership,
                        api_client, django_user_model):
        user = make_users_for_project(project, 1, django_user_model,
                                      'oh hey there'.encode('utf-8'))[0]
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_200_OK

    @override_settings(PLATFORM='zmlp')
    def test_incomplete_apikey(self, project, zmlp_project_user, zmlp_project_membership,
                               api_client, django_user_model):
        user = make_users_for_project(project, 1, django_user_model,
                                      '{"hi": "there"}'.encode('utf-8'))[0]
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_200_OK

    @override_settings(PLATFORM='zmlp')
    def test_failed_zmlp_delete(self, project, zmlp_project_user, django_user_model,
                                zmlp_project_membership, api_client, monkeypatch,
                                zmlp_apikey):

        def mock_return(*args, **kwargs):
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        user = make_users_for_project(project, 1, django_user_model, zmlp_apikey)[0]
        monkeypatch.setattr(ZmlpClient, 'delete', mock_return)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': user.id}))
        assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
        content = response.json()
        assert content['detail'] == 'Error deleting apikey.'


class TestProjectUserPost:

    @override_settings(PLATFORM='zmlp')
    def test_stop_deleting_yourself(self, project, zmlp_project_user,
                                    zmlp_project_membership, api_client, monkeypatch):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        response = api_client.delete(reverse('projectuser-detail',
                                             kwargs={'project_pk': project.id,
                                                     'pk': zmlp_project_user.id}))
        assert response.status_code == status.HTTP_403_FORBIDDEN
        content = response.json()
        assert content['detail'] == 'Cannot remove yourself from a project.'

    @override_settings(PLATFORM='zmlp')
    def test_create(self, project, zmlp_project_user, zmlp_project_membership,
                    api_client, monkeypatch, django_user_model, data, api_key):

        def mock_post_response(*args, **kwargs):
            return data

        def mock_get_response(*args, **kwargs):
            return api_key

        new_user = django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com', 'letmein')  # noqa
        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com', 'roles': ['ML_Tools']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_201_CREATED
        membership = Membership.objects.get(user=new_user, project=project)
        decoded_apikey = convert_base64_to_json(membership.apikey)
        assert decoded_apikey['secretKey'] == api_key['secretKey']

    @override_settings(PLATFORM='zmlp')
    def test_create_already_exists(self, project, zmlp_project_user,
                                   zmlp_project_membership,
                                   api_client, monkeypatch, django_user_model, data,
                                   api_key):
        def mock_post_response(*args, **kwargs):
            return data

        def mock_get_response(*args, **kwargs):
            return api_key

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': zmlp_project_membership.user.username,
                'roles': zmlp_project_membership.roles}
        response = api_client.post(
            reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_200_OK

    @override_settings(PLATFORM='zmlp')
    def test_create_already_exists_batch(self, project, zmlp_project_user,
                                         zmlp_project_membership,
                                         api_client, monkeypatch, django_user_model, data,
                                         api_key):
        def mock_post_response(*args, **kwargs):
            return data

        def mock_get_response(*args, **kwargs):
            return api_key

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'batch': [{'email': zmlp_project_membership.user.username,
                           'roles': zmlp_project_membership.roles}]}
        response = api_client.post(
            reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        results = response.json()['results']
        assert response.status_code == 207
        assert len(results['succeeded']) == 1
        assert not results['failed']

    @override_settings(PLATFORM='zmlp')
    def test_create_already_exists_different_roles(self, project, zmlp_project_user,
                                                   zmlp_project_membership,
                                                   api_client, monkeypatch,
                                                   django_user_model, data,
                                                   api_key):
        def mock_post_response(*args, **kwargs):
            return data

        def mock_get_response(*args, **kwargs):
            return api_key

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': zmlp_project_membership.user.username,
                'roles': []}
        response = api_client.post(
            reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == 409

    @override_settings(PLATFORM='zmlp')
    def test_create_batch(self, project, zmlp_project_user, zmlp_project_membership,
                          api_client, monkeypatch, django_user_model, data, api_key):

        def mock_post_response(*args, **kwargs):
            return data

        def mock_get_response(*args, **kwargs):
            return api_key

        tester1 = django_user_model.objects.create_user('tester1@fake.com', 'tester1@fake.com', 'letmein')  # noqa
        django_user_model.objects.create_user('tester2@fake.com', 'tester2@fake.com', 'letmein')
        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'batch': [
            {'email': 'tester1@fake.com', 'roles': ['ML_Tools']},
            {'email': 'tester2@fake.com', 'roles': ['ML_Tools']},
            {'email': 'tester3@fake.com', 'roles': ['ML_Tools']}
        ]}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_207_MULTI_STATUS
        # Verify at least one membership was created
        membership1 = Membership.objects.get(user=tester1, project=project)
        decoded_apikey = convert_base64_to_json(membership1.apikey)
        assert decoded_apikey['accessKey'] == 'P1klR1U1RgT3YfdLYN4-AHPlnOhXZHeD'
        assert decoded_apikey['secretKey'] == '6Ti7kZZ7IcmWnR1bfdvCMUataoMh9Mbq9Kqvs'
        # Verify Individual response objects
        content = response.json()['results']
        assert len(content['succeeded']) == 2
        assert len(content['failed']) == 1
        assert content['failed'][0]['statusCode'] == status.HTTP_404_NOT_FOUND
        assert content['failed'][0]['email'] == 'tester3@fake.com'
        assert content['failed'][0]['roles'] == ['ML_Tools']
        assert content['failed'][0]['body']['detail'] == 'No user with the given email.'

    @override_settings(PLATFORM='zmlp')
    def test_create_mixed_args(self, project, zmlp_project_user, zmlp_project_membership,
                               api_client, monkeypatch, django_user_model, data):

        def mock_api_response(*args, **kwargs):
            return data

        django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com', 'letmein')
        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com',
                'roles': ['ML_Tools'],
                'batch': [{'email': 'fake'}]}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Batch argument provided with single creation arguments.'

    @override_settings(PLATFORM='zmlp')
    def test_missing_email(self, project, zmlp_project_user, zmlp_project_membership, api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'roles': ['ML_Tools']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Email and Roles are required.'

    @override_settings(PLATFORM='zmlp')
    def test_missing_permissions(self, project, zmlp_project_user, zmlp_project_membership,
                                 api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com'}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Email and Roles are required.'

    @override_settings(PLATFORM='zmlp')
    def test_nonexistent_user(self, project, zmlp_project_user, zmlp_project_membership,
                              api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com', 'roles': ['ML_Tools']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}),
                                   body)  # noqa
        assert response.status_code == status.HTTP_404_NOT_FOUND
        content = response.json()
        assert content['detail'] == 'No user with the given email.'

    @override_settings(PLATFORM='zmlp')
    def test_bad_zmlp_response(self, project, zmlp_project_user, monkeypatch, data,
                               zmlp_project_membership, api_client, django_user_model):

        def mock_api_response(*args, **kwargs):
            raise ZmlpInvalidRequestException({'msg': 'bad'})

        django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com', 'letmein')
        monkeypatch.setattr(ZmlpClient, 'post', mock_api_response)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com', 'roles': ['ML_Tools']}
        response = api_client.post(reverse('projectuser-list', kwargs={'project_pk': project.id}), body)  # noqa
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Invalid request.'


class TestProjectUserPut:

    @override_settings(PLATFORM='zmlp')
    def test_edit_perms(self, project, zmlp_project_user, monkeypatch, data,
                        zmlp_project_membership, api_client, django_user_model):

        def mock_post_response(*args, **kwargs):
            return data

        def mock_delete_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        def mock_get_response(*args, **kwargs):
            return {'accessKey': 'access',
                    'secretKey': 'secret'}

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'delete', mock_delete_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)

        new_user = django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com', 'letmein')  # noqa
        old_data = copy.deepcopy(data)
        old_data['permissions'] = ['AssetsWrite']
        apikey = convert_json_to_base64(data).decode('utf-8')
        Membership.objects.create(user=new_user, project=project, apikey=apikey)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': new_user.email, 'roles': ['User_Admin']}
        response = api_client.put(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': new_user.id}), body)
        assert response.status_code == status.HTTP_200_OK
        membership = Membership.objects.get(user=new_user, project=project)
        decoded_apikey = convert_base64_to_json(membership.apikey)
        assert decoded_apikey['accessKey'] == 'access'
        assert decoded_apikey['secretKey'] == 'secret'
        assert membership.roles == ['User_Admin']

    @override_settings(PLATFORM='zmlp')
    def test_no_permissions(self, project, zmlp_project_user, data, zmlp_project_membership,
                            api_client):
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': 'tester@fake.com'}
        response = api_client.put(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': 1}), body)
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Roles must be supplied.'

    @override_settings(PLATFORM='zmlp')
    def test_no_new_key(self, project, zmlp_project_user, monkeypatch, data,
                        zmlp_project_membership, api_client, django_user_model):

        def mock_post_response(*args, **kwargs):
            raise ZmlpInvalidRequestException({'msg': 'bad'})

        def mock_delete_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        def mock_get_response(*args, **kwargs):
            return {'permissions': ['AssetsWrite']}

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'delete', mock_delete_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)

        new_user = django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com', 'letmein')  # noqa
        old_data = copy.deepcopy(data)
        old_data['permissions'] = ['AssetsWrite']
        apikey = convert_json_to_base64(data).decode('utf-8')
        Membership.objects.create(user=new_user, project=project, apikey=apikey)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': new_user.email, 'roles': ['User_Admin']}
        response = api_client.put(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': new_user.id}), body)
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Invalid request.'

    @override_settings(PLATFORM='zmlp')
    def test_cannot_delete(self, project, zmlp_project_user, monkeypatch, data,
                           zmlp_project_membership, api_client, django_user_model):

        def mock_post_response(*args, **kwargs):
            return data

        def mock_delete_response(*args, **kwargs):
            raise ZmlpInvalidRequestException({'msg': 'bad'})

        def mock_get_response(*args, **kwargs):
            return {'accessKey': 'access',
                    'secretKey': 'secret'}

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'delete', mock_delete_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)

        new_user = django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com', 'letmein')  # noqa
        old_data = copy.deepcopy(data)
        old_data['permissions'] = ['AssetsWrite']
        apikey = convert_json_to_base64(data).decode('utf-8')
        Membership.objects.create(user=new_user, project=project, apikey=apikey)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': new_user.email, 'roles': ['User_Admin']}
        response = api_client.put(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': new_user.id}), body)
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Invalid request.'

    @override_settings(PLATFORM='zmlp')
    def test_server_error(self, project, zmlp_project_user, monkeypatch, data,
                          zmlp_project_membership, api_client, django_user_model):

        def mock_post_response(*args, **kwargs):
            return data

        def mock_delete_response(*args, **kwargs):
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        def mock_get_response(*args, **kwargs):
            return {'accessKey': 'access',
                    'secretKey': 'secret'}

        monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
        monkeypatch.setattr(ZmlpClient, 'delete', mock_delete_response)
        monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)


        new_user = django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com', 'letmein')  # noqa
        old_data = copy.deepcopy(data)
        old_data['permissions'] = ['AssetsWrite']
        apikey = convert_json_to_base64(data).decode('utf-8')
        Membership.objects.create(user=new_user, project=project, apikey=apikey)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': new_user.email, 'roles': ['User_Admin']}
        response = api_client.put(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': new_user.id}), body)
        assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
        content = response.json()
        assert content['detail'] == 'Error deleting apikey.'

    @override_settings(PLATFORM='zmlp')
    def test_inception_key(self, project, zmlp_project_user, monkeypatch, inception_key,
                           zmlp_project_membership, api_client, django_user_model):

        def get_mock_response(*args, **kwargs):
            return {}

        monkeypatch.setattr(ZmlpClient, 'get', get_mock_response)

        new_user = django_user_model.objects.create_user('tester@fake.com', 'tester@fake.com',
                                                         'letmein')  # noqa
        apikey = convert_json_to_base64(inception_key).decode('utf-8')
        Membership.objects.create(user=new_user, project=project, apikey=apikey)
        api_client.force_authenticate(zmlp_project_user)
        api_client.force_login(zmlp_project_user)
        body = {'email': new_user.email, 'roles': ['User_Admin']}
        response = api_client.put(reverse('projectuser-detail',
                                          kwargs={'project_pk': project.id,
                                                  'pk': new_user.id}), body)
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        content = response.json()
        assert content['detail'] == 'Unable to modify the admin key.'


class TestMembershipModel:

    @pytest.fixture
    def apikey_data(self, data):
        data['permissions'] = ['AssetsRead', 'AssetsImport', 'AssetsDelete', 'DataSourceManage',
                               'DataQueueManage']
        return data

    @pytest.fixture
    def clean_membership(self, zmlp_project_user, project):
        original = Membership.objects.get(user=zmlp_project_user, project=project)
        original.delete()

    def test_save_membership_no_roles(self, zmlp_project_user, project, clean_membership):
        membership = Membership(user=zmlp_project_user, project=project)
        membership.full_clean()
        membership.save()
        from_db = Membership.objects.get(id=membership.id)
        assert from_db.user == zmlp_project_user
        assert str(from_db.project.id) == str(project.id)
        assert from_db.roles == []

    def test_save_with_roles(self, zmlp_project_user, project, clean_membership):
        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools', 'User_Admin'])
        membership.full_clean()
        membership.save()
        from_db = Membership.objects.get(id=membership.id)
        assert from_db.user == zmlp_project_user
        assert str(from_db.project.id) == str(project.id)
        assert from_db.roles == ['ML_Tools', 'User_Admin']

    def test_save_with_bad_role(self, zmlp_project_user, project, clean_membership):
        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['Oh Hi'])
        with pytest.raises(ValidationError) as excinfo:
            membership.full_clean()
        assert 'is not a valid choice.' in str(excinfo)

    def test_sync_project_no_apikey(self, zmlp_project_user, project, data, clean_membership,
                                    monkeypatch):

        def post_mock_response(*args, **kwargs):
            return data

        def post_get_response(*args, **kwargs):
            return data

        monkeypatch.setattr(ZmlpClient, 'post', post_mock_response)
        monkeypatch.setattr(ZmlpClient, 'get', post_get_response)

        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools'])
        membership.full_clean()
        membership.save()
        assert membership.apikey == ''
        membership.sync_with_zmlp()
        assert membership.apikey == convert_json_to_base64(data).decode('utf-8')

    def test_sync_project_apikey_all_match(self, zmlp_project_user, project, apikey_data,
                                           clean_membership, monkeypatch):

        def post_mock_response(*args, **kwargs):
            return apikey_data

        def post_get_response(*args, **kwargs):
            return apikey_data

        monkeypatch.setattr(ZmlpClient, 'post', post_mock_response)
        monkeypatch.setattr(ZmlpClient, 'get', post_get_response)

        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools'])
        membership.apikey = convert_json_to_base64(apikey_data).decode('utf-8')
        membership.full_clean()
        membership.save()
        membership.sync_with_zmlp()
        assert membership.apikey == convert_json_to_base64(apikey_data).decode('utf-8')

    def test_sync_project_apikey_no_id(self, zmlp_project_user, project, apikey_data,
                                       clean_membership, monkeypatch):
        data = copy.deepcopy(apikey_data)
        del(apikey_data['id'])

        def post_mock_response(*args, **kwargs):
            return data

        def post_get_response(*args, **kwargs):
            return apikey_data

        monkeypatch.setattr(ZmlpClient, 'post', post_mock_response)
        monkeypatch.setattr(ZmlpClient, 'get', post_get_response)

        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools'])
        membership.apikey = convert_json_to_base64(apikey_data).decode('utf-8')
        membership.full_clean()
        membership.save()
        membership.sync_with_zmlp()
        assert membership.apikey == convert_json_to_base64(data).decode('utf-8')

    def test_sync_internally_inconsistent(self, zmlp_project_user, project, apikey_data,
                                          clean_membership, monkeypatch):

        data = copy.deepcopy(apikey_data)
        data['permissions'] = ['AssetsRead']

        def post_mock_response(*args, **kwargs):
            return apikey_data

        def get_mock_response(*args, **kwargs):
            return apikey_data

        def delete_mock_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        monkeypatch.setattr(ZmlpClient, 'post', post_mock_response)
        monkeypatch.setattr(ZmlpClient, 'get', get_mock_response)
        monkeypatch.setattr(ZmlpClient, 'delete', delete_mock_response)

        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools'])
        membership.apikey = convert_json_to_base64(data).decode('utf-8')
        membership.full_clean()
        membership.save()
        membership.sync_with_zmlp()
        assert membership.apikey == convert_json_to_base64(apikey_data).decode('utf-8')

    def test_sync_externally_inconsistent(self, zmlp_project_user, project, apikey_data,
                                          clean_membership, monkeypatch):

        data = copy.deepcopy(apikey_data)
        data['permissions'] = ['AssetsRead']

        def post_mock_response(*args, **kwargs):
            return apikey_data

        def get_mock_response(*args, **kwargs):
            if args[-1].endswith('_download'):
                return apikey_data
            else:
                return data

        def delete_mock_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        monkeypatch.setattr(ZmlpClient, 'post', post_mock_response)
        monkeypatch.setattr(ZmlpClient, 'get', get_mock_response)
        monkeypatch.setattr(ZmlpClient, 'delete', delete_mock_response)

        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools'])
        membership.apikey = convert_json_to_base64(apikey_data).decode('utf-8')
        membership.full_clean()
        membership.save()
        membership.sync_with_zmlp()
        assert membership.apikey == convert_json_to_base64(apikey_data).decode('utf-8')

    def test_sync_consistent_with_force(self, zmlp_project_user, project, apikey_data,
                                        clean_membership, monkeypatch):

        def post_mock_response(*args, **kwargs):
            return apikey_data

        def post_get_response(*args, **kwargs):
            return apikey_data

        def delete_mock_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        monkeypatch.setattr(ZmlpClient, 'post', post_mock_response)
        monkeypatch.setattr(ZmlpClient, 'get', post_get_response)
        monkeypatch.setattr(ZmlpClient, 'delete', delete_mock_response)

        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools'])
        membership.apikey = convert_json_to_base64(apikey_data).decode('utf-8')
        membership.full_clean()
        membership.save()
        membership.sync_with_zmlp(force=True)
        assert membership.apikey == convert_json_to_base64(apikey_data).decode('utf-8')

    def test_sync_not_in_zmlp(self, zmlp_project_user, project, apikey_data, clean_membership,
                              monkeypatch):

        def post_mock_response(*args, **kwargs):
            return apikey_data

        def post_get_response(*args, **kwargs):
            if args[-1].endswith('_download'):
                return apikey_data
            else:
                raise ZmlpNotFoundException({})

        def delete_mock_response(*args, **kwargs):
            return Response(status=status.HTTP_200_OK)

        monkeypatch.setattr(ZmlpClient, 'post', post_mock_response)
        monkeypatch.setattr(ZmlpClient, 'get', post_get_response)
        monkeypatch.setattr(ZmlpClient, 'delete', delete_mock_response)

        membership = Membership(user=zmlp_project_user, project=project,
                                roles=['ML_Tools'])
        membership.apikey = convert_json_to_base64(apikey_data).decode('utf-8')
        membership.full_clean()
        membership.save()
        membership.sync_with_zmlp(force=True)
        assert membership.apikey == convert_json_to_base64(apikey_data).decode('utf-8')
