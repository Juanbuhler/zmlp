import os

from rest_framework import status
from rest_framework.decorators import action
from rest_framework.response import Response

from projects.views import BaseProjectViewSet
from wallet.paginators import ZMLPFromSizePagination


class JobViewSet(BaseProjectViewSet):
    """CRUD operations for ZMLP or ZVI processing jobs."""
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v1/jobs/'

    def list(self, request, project_pk):
        def item_modifier(request, job):
            job['actions'] = self._get_action_links(request, job['url'], detail=True)

        return self._zmlp_list_from_search(request, item_modifier=item_modifier)

    def retrieve(self, request, project_pk, pk):
        response = request.client.get(f'{self.zmlp_root_api_path}{pk}')
        content = self._get_content(response)
        content['actions'] = self._get_action_links(request)
        return Response(content)

    def _get_action_links(self, request, current_url=None, detail=None):
        """
        Determines the appropriate hyperlinks for all the available actions on a specific
        detailed job view.

        The `current_url` argument is useful when generating the urls for a list of IDs.

        Args:
            request (Request): Incoming request
            current_url (str): Optional URL to use as the base for actions
            detail (bool): Whether to include detail actions or or list actions

        Returns:
            (dict): Hyperlinks to the available actions to include in the Response

        """
        if current_url is not None:
            item_url = current_url
        else:
            item_url = request.build_absolute_uri(request.path)
        actions = self.get_extra_actions()
        action_map = {}
        is_detail = detail if detail is not None else self.detail
        for _action in actions:
            if _action.detail == is_detail:
                action_map[_action.url_name] = f'{item_url}{_action.url_path}/'
        return action_map

    @action(detail=True, methods=['get'])
    def errors(self, request, project_pk, pk):
        """
        Retrieves all the errors that the tasks of the given job may have triggered.

        """
        payload = {'jobIds': [pk],
                   'page': {'from': request.GET.get('from', 0),
                            'size': request.GET.get('size',
                                                    self.pagination_class.default_limit)}}
        response = request.client.post(f'/api/v1/taskerrors/_search', payload)
        content = self._get_content(response)
        paginator = self.pagination_class()
        paginator.prep_pagination_for_api_response(content, request)
        return paginator.get_paginated_response(content['list'])

    @action(detail=True, methods=['put'])
    def pause(self, request, project_pk, pk):
        """
        Pauses the running job.

        The endpoint expects a `PUT` request with an empty body.

        """
        new_values = {'paused': True}
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def resume(self, request, project_pk, pk):
        """
        Resumes the paused job.

        The endpoint expects a `PUT` request with an empty body.

        """
        new_values = {'paused': False}
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def cancel(self, request, project_pk, pk):
        """
        Cancels the given job.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_cancel', {})
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def restart(self, request, project_pk, pk):
        """
        Restarts the cancelled job.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_restart', {})
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'])
    def priority(self, request, project_pk, pk):
        """
        Sets the priority order of the given job in order to control which jobs
        run first.

        The endpoint expects a `PUT` request with a JSON body in the form of:

        `{"priority": 100}`

        With the value of the "priority" key being an integer.

        """
        priority = request.data.get('priority', None)
        if priority is None:
            msg = 'Unable to find a valid `priority` value to use.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        try:
            priority = int(priority)
        except ValueError:
            msg = 'Invalid `priority` value provided. Expected an integer.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        new_values = {'priority': priority}
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'], name='Max Running Tasks')
    def max_running_tasks(self, request, project_pk, pk):
        """
        Sets the maximum number of running tasks for the given job.

        The endpoint expects a `PUT` request with a JSON body in the form of:

        `{"max_running_tasks": 2}`

        With the value of the "max_running_tasks" key being an integer.

        """
        max_running_tasks = request.data.get('max_running_tasks', None)
        if max_running_tasks is None:
            msg = 'Unable to find a valid `max_running_tasks` value to use.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        try:
            max_running_tasks = int(max_running_tasks)
        except ValueError:
            msg = 'Invalid `max_running_tasks` value provided. Expected an integer.'
            return Response({'msg': msg}, status.HTTP_400_BAD_REQUEST)
        new_values = {'maxRunningTasks': max_running_tasks}
        request_body = self._get_updated_info(request.client, pk, new_values)
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}', request_body)
        return Response(self._get_content(response))

    @action(detail=True, methods=['put'], name='Retry All Failures')
    def retry_all_failures(self, request, project_pk, pk):
        """
        Finds every failed task in the given job and retries them.

        The endpoint expects a `PUT` request with an empty body.

        """
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_retryAllFailures', {})
        return Response(self._get_content(response))

    def _get_updated_info(self, client, pk, new_values):
        """
        Pulls the job info for the specified pk, and then updates the job spec
        values with those specified. Used for the various PUT requests to modify jobs.

        Args:
             client: Client to use for the HTTP calls, specific to the platform
             pk (str): The UUID for the job
             new_values (dict): The new values to use

        Returns:
            (dict): Full job spec with updated values
        """
        response = client.get(f'{self.zmlp_root_api_path}{pk}')
        body = self._get_content(response)
        job_spec = {
            'name': body['name'],
            'priority': body['priority'],
            'paused': body['paused'],
            'timePauseExpired': body['timePauseExpired']
        }
        job_spec.update(new_values)
        return job_spec


class TaskViewSet(BaseProjectViewSet):
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v1/tasks/'

    def list(self, request, project_pk):
        def item_modifier(request, task):
            item_url = request.build_absolute_uri(request.path)
            task['actions'] = {'retry': f'{item_url}{task["id"]}/retry/'}

        return self._zmlp_list_from_search(request, item_modifier=item_modifier)

    def retrieve(self, request, project_pk, pk):
        return self._zmlp_retrieve(request, pk)

    @action(detail=True, methods=['put'])
    def retry(self, request, project_pk, pk):
        """Retries a task that has failed. Expects a `PUT` with an empty body."""
        response = request.client.put(f'{self.zmlp_root_api_path}{pk}/_retry', {})
        if response.get('success'):
            return Response({'detail': f'Task {pk} has been successfully retried.'})
        else:
            message = f'Task {pk} failed to be retried. Message from ZMLP: {response}'
            return Response({'detail': message}, status=500)


class TaskErrorViewSet(BaseProjectViewSet):
    pagination_class = ZMLPFromSizePagination
    zmlp_root_api_path = '/api/v1/taskerrors/'

    def list(self, request, project_pk):
        def item_modifier(request, error):
            self._add_job_name(request.client, error)
        return self._zmlp_list_from_search(request, item_modifier=item_modifier)

    def retrieve(self, request, project_pk, pk):
        url = os.path.join(self.zmlp_root_api_path, '_findOne')
        error = request.client.post(url, {'ids': [pk]})
        self._add_job_name(request.client, error)
        return Response(error)

    def _add_job_name(self, client, error):
        error['jobName'] = client.get(f'/api/v1/jobs/{error["jobId"]}')['name']
