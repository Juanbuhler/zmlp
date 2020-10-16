import PropTypes from 'prop-types'

import { colors, constants, typography, spacing } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import WarningSvg from '../Icons/warning.svg'
import InformationSvg from '../Icons/information.svg'
import GeneratingSvg from '../Icons/generating.svg'

const PADDING = spacing.moderate

const STYLES = {
  SUCCESS: {
    backgroundColor: colors.signal.grass.background,
    icon: (
      <CheckmarkSvg
        height={constants.icons.regular}
        color={colors.signal.grass.base}
      />
    ),
    linkColor: colors.signal.grass.base,
  },
  ERROR: {
    backgroundColor: colors.signal.warning.background,
    icon: (
      <WarningSvg
        height={constants.icons.regular}
        color={colors.signal.warning.base}
      />
    ),
    linkColor: colors.signal.warning.base,
  },
  INFO: {
    backgroundColor: colors.signal.sky.background,
    icon: (
      <InformationSvg
        height={constants.icons.regular}
        color={colors.signal.sky.base}
      />
    ),
    linkColor: colors.signal.sky.base,
  },
  PROCESSING: {
    backgroundColor: colors.signal.sky.background,
    icon: (
      <GeneratingSvg
        height={constants.icons.regular}
        color={colors.signal.sky.base}
        css={{ animation: constants.animations.infiniteRotation }}
      />
    ),
    linkColor: colors.signal.sky.base,
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const FlashMessage = ({ variant, children }) => {
  return (
    <div
      css={{
        display: 'flex',
        alignItems: 'flex-start',
        backgroundColor: STYLES[variant].backgroundColor,
        borderRadius: constants.borderRadius.small,
        padding: PADDING,
      }}
    >
      {STYLES[variant].icon}

      <div
        role="alert"
        css={{
          flex: 1,
          paddingLeft: PADDING,
          color: colors.structure.coal,
          fontWeight: typography.weight.medium,
          whiteSpace: 'pre-line',
          a: {
            color: STYLES[variant].linkColor,
          },
        }}
      >
        {children}
      </div>
    </div>
  )
}

FlashMessage.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  children: PropTypes.node.isRequired,
}

export default FlashMessage
