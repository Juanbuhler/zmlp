from boonsdk.entity import Project, ProjectTier
from boonsdk.util import as_id, is_valid_uuid, as_id_collection, as_collection
from ..entity import IndexSize, Index

__all__ = [
    'ProjectApp'
]


class ProjectApp(object):

    def __init__(self, bz, app):
        self.bz = bz
        self.app = app

    def create_project(self, name, tier=ProjectTier.ESSENTIALS, size=IndexSize.SMALL, pid=None):
        """
        Create a project.

        Args:
            name (str): The name of the project.
            tier: (ProjectTier): The project tier.
            size (IndexSize): Used to determine the default index settings.
            pid: (str): An optional project UUID, otherwise its randomly generated.

        Returns:
            Project: The newly created project.
        """
        body = {
            'name': name,
            'tier': tier.name,
            'size': size.name,
            'id': pid
        }
        return Project(self.app.client.post('/api/v1/projects', body))

    def get_project(self, pid):
        """
        Get a project by it's unique Id or name.

        Args:
            pid (str): The project Id or nam,e

        Returns:
            Project
        """
        _id = as_id(pid)
        if is_valid_uuid(_id):
            return Project(self.app.client.get(f'/api/v1/projects/{_id}'))
        else:
            return self.find_one_project(name=pid)

    def find_one_project(self, id=None, name=None):
        """
        Find a single project.

        Args:
            id (str): A project Id or list of Ids.
            name (str): A project name or list of names.
        Returns:
            Project: The matching project.

        """
        body = {
            "ids": as_id_collection(id),
            "names": as_collection(name)
        }
        return Project(self.app.client.post('/api/v1/projects/_findOne', body))

    def find_projects(self, limit=0):
        """
        Returns a generator which can produce a list of all projects.

        Args:
            limit (int) : Limit the number of projects to the given amount.

        Returns:
            Generator: A generator of projects.
        """
        body = {}
        return self.app.client.iter_paged_results('/api/v1/projects/_search',
                                                  body, limit, Project)

    def get_project_index(self, project):
        """
        Get a project index by it's unique Id.

        Args:
            project (Project): The Project or its unique Id.

        Returns:
            Index
        """
        project_id = as_id(project)
        return Index(self.app.client.get(f'/api/v1/projects/{project_id}/_index'))

    def set_project_index(self, project, index):
        """
        Set a project index by it's unique Id.

        Args:
            project (Project): The Project or its unique Id.
            index (Index): The Index or its unique /Id
        Returns:
            dict: A status message
        """
        project_id = as_id(project)
        index_id = as_id(index)
        return self.app.client.put(f'/api/v1/projects/{project_id}/_index/{index_id}')

    def delete_project(self, pid):
        """
        Hard Delete every project information

        Args:
            pid: Project Id

        Returns:

        """
        return self.app.client.delete(f"/api/v1/projects/{pid}")
