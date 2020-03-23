import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CirclePlusSvg from './circlePlus.svg'
import CircleMinusSvg from './circleMinus.svg'

import { ACTIONS } from './reducer'

const AssetsResize = ({ dispatch, isMin, isMax }) => (
  <div
    css={{
      position: 'absolute',
      bottom: 16,
      right: 16,
      display: 'flex',
      border: constants.borders.tab,
      borderRadius: constants.borderRadius.small,
      backgroundColor: colors.structure.lead,
      boxShadow: constants.boxShadows.default,
      zIndex: zIndex.layout.overlay,
      opacity: constants.opacity.eighth,
      paddingTop: spacing.small,
      paddingBottom: spacing.small,
      paddingLeft: spacing.base,
      paddingRight: spacing.base,
    }}
  >
    <Button
      aria-label="Zoom Out"
      onClick={() => {
        dispatch({ type: ACTIONS.DECREMENT })
      }}
      isDisabled={isMin}
      variant={VARIANTS.ICON}
      css={{ opacity: constants.opacity.full, padding: spacing.base }}
    >
      <CircleMinusSvg width={20} />
    </Button>
    <Button
      aria-label="Zoom In"
      onClick={() => {
        dispatch({ type: ACTIONS.INCREMENT })
      }}
      isDisabled={isMax}
      variant={VARIANTS.ICON}
      css={{ opacity: constants.opacity.full, padding: spacing.base }}
    >
      <CirclePlusSvg width={20} />
    </Button>
  </div>
)

AssetsResize.propTypes = {
  dispatch: PropTypes.func.isRequired,
  isMin: PropTypes.bool.isRequired,
  isMax: PropTypes.bool.isRequired,
}

export default AssetsResize
