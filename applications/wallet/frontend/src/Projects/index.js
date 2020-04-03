import { useEffect } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'

const NO_PROJECT_ID_ROUTES = ['/icons', '/account', '/account/password']

const Projects = ({ projectId, mutate, children }) => {
  const { query: { projectId: routerProjectId } = {}, pathname } = useRouter()

  const {
    data: { results: projects },
  } = useSWR('/api/v1/projects/')

  useEffect(() => {
    if (!routerProjectId) return

    if (projectId === routerProjectId) return

    mutate((user) => ({ ...user, projectId: routerProjectId }), false)
  }, [projectId, routerProjectId, mutate])

  // Reset user projectId if not part of current projects
  if (projectId && !projects.find(({ id }) => projectId === id)) {
    mutate((user) => ({ ...user, projectId: '' }), false)
    return null
  }

  // Render "Account Overview"
  if (pathname === '/') {
    return children
  }

  // Render other pages without projectId
  if (!routerProjectId && NO_PROJECT_ID_ROUTES.includes(pathname)) {
    return children
  }

  // Redirect pages that require a projectId to "No Projects" page if there are no projects
  if (projects.length === 0 && !NO_PROJECT_ID_ROUTES.includes(pathname)) {
    Router.push('/')
    return null
  }

  // Remove url projectId if not part of current projects
  if (!projects.find(({ id }) => routerProjectId === id)) {
    Router.push('/[projectId]/jobs', `/${projectId || projects[0].id}/jobs`)
    return null
  }

  return children
}

Projects.propTypes = {
  projectId: PropTypes.string.isRequired,
  mutate: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Projects
