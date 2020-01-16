import PropTypes from 'prop-types'

import { colors, typography } from '../Styles'

const HEIGHT = 600

const TableException = ({ numColumns, children }) => {
  return (
    <tr css={{ pointerEvents: 'none' }}>
      <td colSpan={numColumns}>
        <div
          css={{
            height: HEIGHT,
            width: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            color: colors.structure.steel,
            fontSize: typography.size.kilo,
            lineHeight: typography.height.kilo,
          }}>
          {children}
        </div>
      </td>
    </tr>
  )
}

TableException.propTypes = {
  numColumns: PropTypes.number.isRequired,
  children: PropTypes.node.isRequired,
}

export default TableException
