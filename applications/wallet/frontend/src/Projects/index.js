import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'

const Projects = ({ children }) => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: { results: projects } = {} } = useSWR('/api/v1/projects/')

  if (!Array.isArray(projects)) return <Loading />

  if (projects.length === 0) {
    if (projectId) {
      Router.push('/')

      return null
    }

    return children
  }

  if (!projectId || !projects.find(({ id }) => projectId === id)) {
    Router.push('/[projectId]/jobs', `/${projects[0].id}/jobs`)

    return null
  }

  return children
}

Projects.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Projects
