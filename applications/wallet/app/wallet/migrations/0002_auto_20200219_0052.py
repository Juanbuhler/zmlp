# Generated by Django 2.2.9 on 2020-02-19 00:52

import os
import json
import pathlib
import logging
import base64

import backoff
import requests
from django.db import migrations
from django.conf import settings
from django.contrib.auth import get_user_model

from projects.models import Project, Membership
from projects.util import sync_project_with_zmlp

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

    # Get the inception key from the env (production)
    inception_key = os.getenv('INCEPTION_KEY_B64')
    if not inception_key:
        # Get it from the file if it's not in the env (dev)
        current_dir = pathlib.Path(__file__).parent.absolute()
        key_file = os.path.join(current_dir, '../../../../../dev/config/keys/inception-key.json')
        if not os.path.exists(key_file):
            logger.error('Could not find inception key file or ENV var for migration.')
            raise OSError('Could not find inception key file.')
        with open(key_file, 'rb') as _file:
            key_contents = json.load(_file)
        inception_key = base64.b64encode(json.dumps(key_contents).encode('utf-8')).decode('utf-8')

    # Create the membership if it doesn't already exist
    user = User.objects.get(username='admin')
    membership = None
    try:
        membership = Membership.objects.get(user=user, project=project_zero)
    except Membership.DoesNotExist:
        pass

    if not membership:
        Membership.objects.create(user=user, project=project_zero, apikey=inception_key)
    else:
        logger.info('Project Zero membership already exists, not modifying.')

    # Sync Project Zero to Zmlp
    _zmlp_up_check()
    try:
        project_zero.sync_project_with_zmlp(user)
    except requests.exceptions.ConnectionError:
        logger.error('Unable to sync Project Zero to ZMLP, please check.')


@backoff.on_exception(backoff.expo, requests.exceptions.ConnectionError, max_time=120)
def _zmlp_up_check():
    requests.get(os.path.join(settings.ZMLP_API_URL, 'monitor/health'))


class Migration(migrations.Migration):

    dependencies = [
        ('wallet', '0001_initial'),
        ('projects', '0004_auto_20200129_2353')
    ]

    operations = [
        migrations.RunPython(create_project_zero),
    ]
