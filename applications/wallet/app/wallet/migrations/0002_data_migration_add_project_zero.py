# Generated by Django 2.2.12 on 2020-05-20 18:17

import logging

import backoff
import requests
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db import migrations
from wallet.utils import sync_project_with_zmlp, sync_membership_with_zmlp


User = get_user_model()
logger = logging.getLogger(__name__)


def create_project_zero(apps, schema_editor):
    # Create project zero (or get it for deployments where it may already exist).
    Project = apps.get_model('projects', 'Project')
    project_zero, created = Project.all_objects.get_or_create(id='00000000-0000-0000-0000-000000000000',
                                                          name='Project Zero')
    project_zero.save()

    # Create the membership if it doesn't already exist
    User = apps.get_model(settings.AUTH_USER_MODEL)
    user = User.objects.get(username=settings.SUPERUSER_EMAIL)
    Membership = apps.get_model('projects', 'Membership')
    membership = Membership.objects.get_or_create(user=user, project=project_zero,
                                                  roles=[r['name'] for r in settings.ROLES])[0]

    # Sync Project Zero to Zmlp
    sync_project_with_zmlp(project_zero)
    sync_membership_with_zmlp(membership)


# @backoff.on_exception(backoff.expo, requests.exceptions.ConnectionError, max_time=300)
# def sync_project(project_zero, membership):
#     try:
#         import pdb; pdb.set_trace()
#         project_zero.sync_with_zmlp()
#         membership.sync_with_zmlp(project_zero.get_zmlp_super_client())
#     except Exception:
#         raise requests.exceptions.ConnectionError()


class Migration(migrations.Migration):

    dependencies = [
        ('wallet', '0001_data_migration_add_superuser'),
        ('projects', '0004_auto_20200810_2320'),
    ]

    operations = [
        migrations.RunPython(create_project_zero),
    ]

