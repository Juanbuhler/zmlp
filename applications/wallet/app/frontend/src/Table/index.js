import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import Pagination from '../Pagination'

const SIZE = 20

const Table = ({ url, columns, renderRow }) => {
  const {
    query: { page = 1 },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE

  const { data: { count = 0, results } = {}, revalidate } = useSWR(
    `${url}?from=${from}&size=${SIZE}`,
  )

  if (!Array.isArray(results)) return 'Loading...'

  if (results.length === 0) return 'There are no results'

  return (
    <div>
      <table
        css={{
          width: '100%',
          borderSpacing: 0,
          boxShadow: constants.boxShadows.table,
          whiteSpace: 'nowrap',
          tr: {
            backgroundColor: colors.structure.lead,
            '&:nth-of-type(2n)': {
              backgroundColor: colors.structure.mattGrey,
            },
            ':hover': {
              backgroundColor: colors.structure.iron,
              boxShadow: constants.boxShadows.tableRow,
              td: {
                border: constants.borders.tableRow,
                borderLeft: '0',
                borderRight: '0',
                '&:first-of-type': {
                  borderLeft: constants.borders.tableRow,
                },
                '&:last-of-type': {
                  borderRight: constants.borders.tableRow,
                },
              },
            },
          },
          td: {
            fontWeight: typography.weight.extraLight,
            color: colors.structure.pebble,
            padding: `${spacing.base}px ${spacing.normal}px`,
            border: constants.borders.transparent,
            borderLeft: '0',
            borderRight: '0',
            ':first-of-type': {
              borderLeft: constants.borders.transparent,
            },
            ':last-of-type': {
              borderRight: constants.borders.transparent,
            },
          },
        }}>
        <thead>
          <tr>
            {columns.map(column => (
              <th
                key={column}
                css={{
                  textAlign: 'left',
                  fontSize: typography.size.kilo,
                  lineHeight: typography.height.kilo,
                  fontWeight: typography.weight.medium,
                  color: colors.structure.pebble,
                  backgroundColor: colors.structure.iron,
                  padding: `${spacing.moderate}px ${spacing.normal}px`,
                  borderBottom: constants.borders.default,
                  ':nth-of-type(2)': { width: '100%' },
                  '&:not(:last-child)': {
                    borderRight: constants.borders.default,
                  },
                }}>
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {results.map(result => renderRow({ result, revalidate }))}
        </tbody>
      </table>

      <div>&nbsp;</div>

      <Pagination
        currentPage={parsedPage}
        totalPages={Math.ceil(count / SIZE)}
      />
    </div>
  )
}

Table.propTypes = {
  url: PropTypes.string.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string).isRequired,
  renderRow: PropTypes.func.isRequired,
}

export default Table
