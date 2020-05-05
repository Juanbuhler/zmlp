import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import { dispatch, ACTIONS } from './helpers'

const FiltersReset = ({
  projectId,
  assetId,
  filters,
  updatedFilter,
  filterIndex,
}) => {
  return (
    <div
      css={{
        display: 'flex',
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
      }}
    >
      <div css={{ flex: 1 }} />
      <Button
        style={{
          width: '100%',
          color: colors.structure.zinc,
          fontFamily: 'Roboto Condensed',
          ':hover': {
            color: colors.structure.white,
          },
        }}
        variant={VARIANTS.NEUTRAL}
        onClick={() =>
          dispatch({
            action: ACTIONS.UPDATE_FILTER,
            payload: {
              projectId,
              assetId,
              filters,
              updatedFilter: { ...updatedFilter, values: {} },
              filterIndex,
            },
          })
        }
      >
        RESET
      </Button>
    </div>
  )
}

FiltersReset.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      attribute: PropTypes.string,
      values: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    }).isRequired,
  ).isRequired,
  updatedFilter: PropTypes.shape({
    type: PropTypes.string.isRequired,
    attribute: PropTypes.string.isRequired,
  }).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FiltersReset
