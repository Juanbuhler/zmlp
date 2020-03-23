import PropTypes from 'prop-types'

import checkboxOptionShape from './optionShape'

import { spacing, colors, constants, typography } from '../Styles'

import CheckboxTableRow from './TableRow'

const CheckboxTable = ({ category: { name, options }, onClick }) => {
  return (
    <fieldset
      css={{
        border: 'none',
        padding: 0,
        margin: 0,
        paddingTop: spacing.moderate,
        paddingBottom: spacing.spacious,
      }}
    >
      <legend
        css={{
          float: 'left',
          padding: 0,
          paddingBottom: spacing.moderate,
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
        }}
      >
        {name}
      </legend>
      <div css={{ clear: 'both' }} />
      <table
        cellSpacing="0"
        css={{
          width: '100%',
          color: colors.structure.white,
          textAlign: 'left',
          'th, td': {
            padding: spacing.normal,
            paddingLeft: 0,
            ':first-of-type': {
              paddingLeft: spacing.normal,
            },
            ':nth-of-type(2)': {
              minWidth: 300,
            },
            ':nth-of-type(3)': { width: '100%' },
          },
          th: {
            color: colors.structure.zinc,
            fontWeight: typography.weight.medium,
          },
          td: {
            borderTop: constants.borders.tabs,
            verticalAlign: 'top',
            ':first-of-type': {
              border: 'none',
            },
            ':nth-of-type(2)': {
              fontFamily: 'Roboto Mono',
            },
          },
          tr: {
            ':hover': {
              td: {
                backgroundColor: colors.structure.iron,
              },
              '+ tr': {
                td: {
                  borderTopColor: 'transparent',
                },
              },
            },
          },
          'tr:last-of-type': {
            td: {
              borderBottom: constants.borders.tabs,
              ':first-of-type': {
                border: 'none',
              },
            },
          },
        }}
      >
        <thead>
          <tr>
            <th>&nbsp;</th>
            <th>Module Name</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {options.map((option) => (
            <CheckboxTableRow
              key={option.value}
              option={option}
              onClick={(value) => onClick({ [option.value]: value })}
            />
          ))}
        </tbody>
      </table>
    </fieldset>
  )
}

CheckboxTable.propTypes = {
  category: PropTypes.shape({
    name: PropTypes.string.isRequired,
    options: PropTypes.arrayOf(PropTypes.shape(checkboxOptionShape)).isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxTable
