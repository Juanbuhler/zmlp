import { useRouter } from 'next/router'
import useSWR from 'swr'

import SectionTitle from '../SectionTitle'
import Value, { VARIANTS } from '../Value'
import Tabs from '../Tabs'
import TaskErrorStackTrace from '../TaskErrorStackTrace'
import TaskErrorAsset from '../TaskErrorAsset'
import SuspenseBoundary from '../SuspenseBoundary'

import TaskErrorType from './Type'
import TaskErrorTaskMenu from './TaskMenu'
import TaskErrorDetails from './Details'

const TaskErrorContent = () => {
  const {
    pathname,
    query: { projectId, errorId },
  } = useRouter()

  const { data: taskError, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/task_errors/${errorId}/`,
  )

  const { jobName, fatal, message, taskId, assetId } = taskError

  return (
    <>
      <SectionTitle>Job: {jobName}</SectionTitle>

      <TaskErrorType fatal={fatal} />

      <Value legend="Error Message" variant={VARIANTS.SECONDARY}>
        {message}
      </Value>

      <TaskErrorTaskMenu
        projectId={projectId}
        taskId={taskId}
        revalidate={revalidate}
      />

      <TaskErrorDetails taskError={taskError} />

      <Tabs
        tabs={[
          {
            title: 'Stack Trace',
            href: '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]',
          },
          {
            title: 'Asset',
            href:
              '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]/asset',
          },
        ]}
      />

      {pathname ===
        '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]' && (
        <TaskErrorStackTrace taskError={taskError} />
      )}

      {pathname ===
        '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]/asset' && (
        <SuspenseBoundary>
          <TaskErrorAsset assetId={assetId} />
        </SuspenseBoundary>
      )}
    </>
  )
}

export default TaskErrorContent
