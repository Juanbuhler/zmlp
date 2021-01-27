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
    """Choices for the tier field in the Subscription model."""
    ACCESS = 'access'
    BUILD = 'build'
    CUSTOM_ENTERPRISE = 'custom_enterprise'


class Organization(UUIDMixin, TimeStampMixin, ActiveMixin):
    """An organization is a collection of projects with an owner. Currently this is only
    used for billing purposes."""
    name = models.CharField(max_length=144, unique=True, default=random_organization_name)
    owner = models.ForeignKey(User, on_delete=models.DO_NOTHING, null=True, blank=True)
    plan = models.CharField(max_length=6, choices=Plan.choices, default=Plan.ACCESS)

    def __str__(self):
        return self.name

    def __repr__(self):
        return f"Organization(name='{self.name}', owner_id={self.owner_id})"

    def save(self, *args, **kwargs):
        with transaction.atomic():
            super(Organization, self).save(*args, **kwargs)
            if not self.isActive:
                for project in self.projects.all():
                    project.isActive = False
                    project.save()

    def get_ml_usage_last_hour(self, start_time=None, end_time=None):
        """The summed tiered usage for all Projects associated with this Org over last hour.

        If start_time and end_time are given, that time range is used rather than the
        last hour time period.

        Args:
            start_time (datetime): Start of time window to look at.
            end_time (datetime): End of time window to look at.

        Returns:
            (dict): A dictionary specifying the total image and video hour sums for
                all tiers on all projects for this Org.

        """
        metrics_path = os.path.join(settings.METRICS_API_URL, 'api/v1/apicalls/tiered_usage')

        # Get last hour start and end time if it wasn't already given
        if end_time is None:
            end_time = datetime.datetime.utcnow()
        if start_time is None:
            start_time = end_time - datetime.timedelta(hours=1)

        # Get all projects for this organization
        projects = self.projects.filter(isActive=True)

        # Request tiered usage for each project from metrics
        project_results = []
        for project in projects:
            response = requests.get(metrics_path, {'project': project.id,
                                                   'after': start_time,
                                                   'before': end_time})
            project_results.append(response.json())

        # Combine
        summed_tiers = {
            'tier_1_image_count': sum([r['tier_1_image_count'] for r in project_results]),
            'tier_1_video_hours': sum([r['tier_1_video_hours'] for r in project_results]),
            'tier_2_image_count': sum([r['tier_2_image_count'] for r in project_results]),
            'tier_2_video_hours': sum([r['tier_2_video_hours'] for r in project_results])
        }

        return summed_tiers
