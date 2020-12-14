# Generated by Django 3.0.11 on 2020-12-09 19:24

import logging

from django.db import migrations

from projects.models import Project
from subscriptions.models import Subscription, Tier

logger = logging.getLogger(__name__)

def upgrade_subscription(apps, schema_editor):
    # Create a default Premier Subscription for Project Zero
    project_zero = Project.objects.get(id='00000000-0000-0000-0000-000000000000')
    try:
        project_zero.subscription
    except Project.subscription.RelatedObjectDoesNotExist:
        Subscription.objects.create(project=project_zero,
                                    tier=Tier.PREMIER)
        project_zero.sync_with_zmlp()
        logger.debug('Created Project Zero Premier Subscripton.')
    else:
        logger.debug('Project Zero already has a subscription. No action taken.')


class Migration(migrations.Migration):

    dependencies = [
        ('wallet', '0004_remove_system_permissions_from_users'),
    ]

    operations = [
        migrations.RunPython(upgrade_subscription)
    ]