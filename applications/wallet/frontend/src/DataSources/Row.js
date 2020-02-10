import PropTypes from 'prop-types'

import { formatFullDate } from '../Date/helpers'

import Pills from '../Pills'

import DataSourcesMenu from './Menu'

const DataSourcesRow = ({
  projectId,
  dataSource: {
    id: dataSourceId,
    name,
    uri,
    timeCreated,
    timeModified,
    fileTypes,
  },
  revalidate,
}) => {
  return (
    <tr>
      <td>{name}</td>
      <td>Google Cloud Storage</td>
      <td>{uri}</td>
      <td>{formatFullDate({ timestamp: timeCreated })}</td>
      <td>{formatFullDate({ timestamp: timeModified })}</td>
      <td>
        <Pills>{fileTypes}</Pills>
      </td>
      <td>
        <DataSourcesMenu
          projectId={projectId}
          dataSourceId={dataSourceId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

DataSourcesRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  dataSource: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    uri: PropTypes.string.isRequired,
    timeCreated: PropTypes.number.isRequired,
    timeModified: PropTypes.number.isRequired,
    fileTypes: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DataSourcesRow
