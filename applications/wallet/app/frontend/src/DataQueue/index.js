import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing, typography } from '../Styles'

import Table from '../Table'
import Pagination from '../Pagination'

export const noop = () => () => {}

const COLUMNS = [
  'Status',
  'Job Name',
  'Created By',
  'Priority',
  'Created',
  'Failed',
  'Errors',
  '# Assets',
  'Progress',
]

const DataQueue = ({ selectedProject }) => {
  const { data: { results } = {} } = useSWR(
    `/api/v1/projects/${selectedProject.id}/jobs/`,
  )

  if (!Array.isArray(results)) return 'Loading...'

  if (results.length === 0) return 'You have 0 jobs'

  return (
    <div css={{ padding: `0 ${spacing.spacious}px` }}>
      <h2
        css={{
          fontSize: typography.size.mega,
          lineHeight: typography.height.mega,
          fontWeight: typography.weight.regular,
          paddingTop: spacing.comfy,
          paddingBottom: spacing.normal,
        }}>
        Data Queue
      </h2>

      <Table columns={COLUMNS} rows={results} />

      <div>&nbsp;</div>

      <Pagination
        legend="Jobs: 1–17 of 415"
        currentPage={1}
        totalPages={2}
        prevLink="/"
        nextLink="/?page=2"
        onClick={noop}
      />
    </div>
  )
}

DataQueue.propTypes = {
  selectedProject: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
  }).isRequired,
}

export default DataQueue
