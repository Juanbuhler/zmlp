import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'

import JobTasks from '../JobTasks'
import TaskErrors from '../TaskErrors'

import JobDetails from './Details'

const Job = () => {
  const {
    pathname,
    query: { projectId, jobId, refreshParam },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Job Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Job Details', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <JobDetails key={pathname} />

        <Tabs
          tabs={[
            { title: 'Tasks', href: '/[projectId]/jobs/[jobId]' },
            { title: 'Errors', href: '/[projectId]/jobs/[jobId]/errors' },
          ]}
        />

        {pathname === '/[projectId]/jobs/[jobId]' && (
          <JobTasks key={refreshParam} />
        )}

        {pathname === '/[projectId]/jobs/[jobId]/errors' && (
          <TaskErrors
            key={refreshParam}
            parentUrl={`/api/v1/projects/${projectId}/jobs/${jobId}/`}
          />
        )}
      </SuspenseBoundary>
    </>
  )
}

export default Job
