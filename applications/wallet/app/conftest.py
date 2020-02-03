from base64 import b64encode

import pytest
from rest_framework.test import APIClient, APIRequestFactory


from projects.models import Project, Membership


@pytest.fixture(scope='session')
def api_client():
    return APIClient()


@pytest.fixture(scope='session')
def api_factory():
    return APIRequestFactory()


@pytest.fixture
def user(django_user_model, api_client):
    user = django_user_model.objects.create_user('user', 'user@fake.com', 'letmein')
    return user


@pytest.fixture
def superuser(django_user_model, api_client):
    user = django_user_model.objects.create_superuser('superuser', 'superuser@fake.com', 'letmein')
    return user


@pytest.fixture
def project():
    return Project.objects.create(id='6abc33f0-4acf-4196-95ff-4cbb7f640a06',
                                  name='Test Project')


@pytest.fixture
def project2():
    return Project.objects.create(id='e93cbadb-e5ae-4598-8395-4cf5b30c0e94',
                                  name='Test Project 2')


@pytest.fixture
def zmlp_apikey():
    return b"""{
    "name": "admin-key",
    "projectId": "97271bd2-5b51-427a-8fbf-93b4cdb2ba85",
    "id": "4338a83f-a920-hedb-a251-a123b17df1ba",
    "accessKey": "Nn6RoLw9TRe2vTUXmy74CZWPDkVwVbcp",
    "secretKey": "beecda19ed4120b8172309e47242ea88bf35d86aca19bdefb189fe468641b48c8e17241ec955b6a6653b5f1b96ed6e88ccb5f251a04efe70d0e2ef93b60bf9b3",
    "permissions": ["SuperAdmin", "ProjectAdmin", "AssetsRead", "AssetsImport"]
}""" # noqa


@pytest.fixture
def zmlp_project_membership(project, user, zmlp_apikey):
    apikey = b64encode(zmlp_apikey).decode('utf-8')
    return Membership.objects.create(user=user, project=project, apikey=apikey)


@pytest.fixture
def zmlp_project_user(user, zmlp_project_membership):
    return user


@pytest.fixture
def zvi_project_membership(project, user):
    apikey = b"""{"userId": "00000000-7b0b-480e-8c36-f06f04aed2f1",
    "user": "admin",
    "key": "65950f84a6f97c111be559f54666308c719210468c3476e9bae813484bc703ce",
    "server": "https://dev.zorroa.com/"}"""
    apikey = b64encode(apikey).decode('utf-8')
    return Membership.objects.create(user=user, project=project, apikey=apikey)


@pytest.fixture
def zvi_project_user(user, zvi_project_membership):
    return user
