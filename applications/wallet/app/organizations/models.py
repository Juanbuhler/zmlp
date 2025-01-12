import os
import datetime
import requests
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db import models, transaction

from organizations.utils import random_organization_name
from wallet.mixins import TimeStampMixin, UUIDMixin, ActiveMixin

User = get_user_model()


class Plan(models.TextChoices):
    """Choices for the plan field in the Organization model."""
    ACCESS = 'access'
    BUILD = 'build'
    CUSTOM_ENTERPRISE = 'custom_enterprise'


class Organization(UUIDMixin, TimeStampMixin, ActiveMixin):
    """An organization is a collection of projects with owners that have full access."""
    name = models.CharField(max_length=144, unique=True, default=random_organization_name)
    owners = models.ManyToManyField(User, related_name='organizations')
    plan = models.CharField(max_length=24, choices=Plan.choices, default=Plan.ACCESS)

    def __str__(self):
        return self.name

    def __repr__(self):
        return f"Organization(name='{self.name}')"

    def save(self, *args, **kwargs):
        with transaction.atomic():
            super(Organization, self).save(*args, **kwargs)
            if not self.isActive:
                for project in self.projects.all():
                    project.isActive = False
                    project.save()

    def get_ml_usage_for_time_period(self, start_time=None, end_time=None):
        """The summed tiered usage for all Projects associated with this Org over a time period.

        If no start_time and end_time are given, a time period of 1 hour is used by default.

        Args:
            start_time (datetime): Start of time window to look at.
            end_time (datetime): End of time window to look at.

        Returns:
            (dict): A dictionary with summed totals and usage per-project.

        """
        metrics_path = os.path.join(settings.METRICS_API_URL, 'api/v1/apicalls/tiered_usage')

        # Set the 1 hour default if no time period is given
        if end_time is None:
            end_time = datetime.datetime.utcnow()
        if start_time is None:
            start_time = end_time - datetime.timedelta(hours=1)

        # Get all active projects for this organization
        projects = self.projects.filter(isActive=True)

        # Request tiered usage for each project from metrics
        project_usage = {}
        for project in projects:
            response = requests.get(metrics_path, {'project': project.id,
                                                   'after': start_time,
                                                   'before': end_time})
            response.raise_for_status()
            results = response.json()
            project_usage[str(project.id)] = {
                'tier_1_image_count': results['tier_1']['image_count'],
                'tier_1_video_hours': int(results['tier_1']['video_minutes'] / 60),
                'tier_2_image_count': results['tier_2']['image_count'],
                'tier_2_video_hours': int(results['tier_2']['video_minutes'] / 60)
            }
        return project_usage
