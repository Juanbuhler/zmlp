import { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'
import SectionTitle from '../SectionTitle'
import Value, { VARIANTS } from '../Value'

import JobErrorType from './Type'
import JobErrorTaskMenu from './TaskMenu'

const JobErrorContent = () => {
  const {
    query: { projectId, errorId },
  } = useRouter()

  const { data: jobError, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/taskerrors/${errorId}`,
  )

  if (typeof jobError === 'undefined') return <Loading />

  const { jobName, fatal, message, taskId } = jobError

  return (
    <>
      <SectionTitle>Job: {jobName}</SectionTitle>

      <JobErrorType fatal={fatal} />

      <Value legend="Error Message" variant={VARIANTS.SECONDARY}>
        {message}
      </Value>

      <JobErrorTaskMenu
        projectId={projectId}
        taskId={taskId}
        revalidate={revalidate}
      />
    </>
  )
}

export default JobErrorContent
