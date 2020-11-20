import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CirclePlusSvg from '../Icons/circlePlus.svg'
import CircleMinusSvg from '../Icons/circleMinus.svg'

import { ACTIONS } from './reducer'

const TimelineResize = ({ dispatch, zoom }) => (
  <div
    css={{
      position: 'absolute',
      bottom: spacing.normal,
      right: spacing.normal,
      display: 'flex',
      border: constants.borders.regular.iron,
      borderRadius: constants.borderRadius.small,
      backgroundColor: colors.structure.lead,
      boxShadow: constants.boxShadows.default,
      zIndex: zIndex.timeline.menu,
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
      isDisabled={zoom === 100}
      variant={VARIANTS.NEUTRAL}
      css={{
        padding: spacing.base,
        ':hover': {
          color: colors.key.one,
        },
        '&[aria-disabled=true]': {
          color: colors.structure.steel,
        },
        opacity: constants.opacity.full,
      }}
    >
      <CircleMinusSvg height={constants.icons.regular} />
    </Button>
    <Button
      aria-label="Zoom In"
      onClick={() => {
        dispatch({ type: ACTIONS.INCREMENT })
      }}
      isDisabled={false}
      variant={VARIANTS.NEUTRAL}
      css={{
        padding: spacing.base,
        ':hover': {
          color: colors.key.one,
        },
        '&[aria-disabled=true]': {
          color: colors.structure.steel,
        },
        opacity: constants.opacity.full,
      }}
    >
      <CirclePlusSvg height={constants.icons.regular} />
    </Button>
  </div>
)

TimelineResize.propTypes = {
  dispatch: PropTypes.func.isRequired,
  zoom: PropTypes.number.isRequired,
}

export default TimelineResize
