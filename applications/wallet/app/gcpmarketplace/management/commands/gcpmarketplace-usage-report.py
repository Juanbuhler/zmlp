import datetime
import pprint
import uuid
from time import sleep

import sentry_sdk
from django.conf import settings
from django.core.management import BaseCommand

from gcpmarketplace.utils import get_service_control_api, get_procurement_api

sentry_sdk.init(
    dsn='https://5c1ab0d8be954c35b92283c1290e9924@o280392.ingest.sentry.io/5218609')


class UsageReporter():
    # Handles reporting project usage to Googel Marketplace.

    def report(self):
        """Loops over all active entitlements and sends usage information for each of them."""
        entitlements = self._get_active_entitlements()
        if not entitlements:
            print('No active entitlements. No usage reports to send.')
            return
        for entitlement in entitlements:
            self._report_usage(entitlement)

    def _get_active_entitlements(self):
        """Returns a list of all active marketplace entitlements."""
        request = get_procurement_api().providers().entitlements().list(
            parent=f'providers/DEMO-{settings.MARKETPLACE_PROJECT_ID}',  #TODO: Remove DEMO- when this goes live.
            filter='state=active')
        return request.execute().get('entitlements')

    def _get_usage(self, entitlement):
        """Returns usage info for the project linked to the entitlement given."""

        # TODO: Removed once the Zmlp API is updated.
        return {'end_time': 1588701600,
                'video_hours': 1,
                'image_count': 2}

        project_id = entitlement['name'].split('/')[-1]
        usage = django_command('gethourlyusage', project_id)
        return json.loads(usage)

    def _report_usage(self, entitlement):
        """Sends usage information to marketplace for the given entitlement."""
        ServiceControlApi = get_service_control_api()
        time_format = '%Y-%m-%dT%H:%M:%SZ'
        usage = self._get_usage(entitlement)
        end_time = datetime.datetime.fromtimestamp(usage['end_time'],
                                                   datetime.timezone.utc)
        start_time = end_time - datetime.timedelta(hours=1)
        operation = {
            'operationId': str(uuid.uuid4()),
            'operationName': 'Codelab Usage Report',
            'consumerId': entitlement['usageReportingId'],
            'startTime': start_time.strftime(time_format),
            'endTime': end_time.strftime(time_format),
            'metricValueSets': [{
                'metricName': f'{settings.MARKETPLACE_SERVICE_NAME}/{entitlement["plan"]}_requests',
                # TODO: Get real service name.
                'metricValues': [{
                    'int64Value': usage['image_count'],  # TODO: Get from usage.
                }],
            }],
        }
        check = ServiceControlApi.services().check(
            serviceName=settings.MARKETPLACE_SERVICE_NAME, body={
                'operation': operation
            }).execute()

        if 'checkErrors' in check:
            print('Errors for user %s with product %s:' % (entitlement['account'],
                                                           entitlement['product']))
            print(check['checkErrors'])
            ### TODO: Temporarily turn off service for the user. ###
            return
        print(f'Sending report:\n{pprint.pformat(operation)}')
        ServiceControlApi.services().report(
            serviceName=settings.MARKETPLACE_SERVICE_NAME, body={
                'operations': [operation]
            }).execute()


class Command(BaseCommand):
    help = 'Starts service that sends usage reports to gcp marketplace every hour.'

    def handle(self, *args, **options):
        usage_reporter = UsageReporter()
        try:
            while True:
                print('Sending usage report to marketplace.')
                usage_reporter.report()
                print('Usage reporting successful. Sleeping for 1 hour.')
                sleep(60 * 60)
        except KeyboardInterrupt:
            print('Program terminated by user. Goodbye.')
            return
