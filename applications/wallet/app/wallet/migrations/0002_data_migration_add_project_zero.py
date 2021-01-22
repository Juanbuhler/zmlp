# Generated by Django 2.2.12 on 2020-05-20 18:17

import logging

import backoff
import requests
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db import migrations

from projects.models import Project, Membership
from wallet.utils import get_zmlp_superuser_client

User = get_user_model()
logger = logging.getLogger(__name__)


def create_project_zero(apps, schema_editor):
    # Regardless of Platform, create project zero (or get it for deployments where it may
    # already exist
    project_zero, created = Project.objects.get_or_create(id='00000000-0000-0000-0000-000000000000',
                                                          name='Project Zero')
    project_zero.save()

    # if this is zvi, stop
    if settings.PLATFORM == 'zvi':
        if created:
            logger.info('Created Project Zero for ZVI platform.')
        else:
            logger.info('Project Zero already exists for ZVI platform.')
        return

    # Create the membership if it doesn't already exist
    user = User.objects.get(username=settings.SUPERUSER_EMAIL)
    membership = Membership.objects.get_or_create(user=user, project=project_zero,
                                                  roles=[r['name'] for r in settings.ROLES])[0]

    # Sync Project Zero to Zmlp
    sync_project(project_zero, membership)


@backoff.on_exception(backoff.expo, requests.exceptions.ConnectionError, max_time=300)
def sync_project(project_zero, membership):
    try:
        project_zero.sync_with_zmlp()
        membership.sync_with_zmlp(project_zero.get_zmlp_super_client())
    except Exception:
        raise requests.exceptions.ConnectionError()


def fake_reverse(apps, schema_editor):
    pass


class Migration(migrations.Migration):

    dependencies = [
        ('wallet', '0001_data_migration_add_superuser'),
        ('projects', '0004_auto_20200810_2320'),
        ('subscriptions', '0002_auto_20200528_2041'),
    ]

    operations = [
        migrations.RunPython(create_project_zero, fake_reverse),
    ]

