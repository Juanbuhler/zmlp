import PropTypes from 'prop-types'

import { constants, spacing, colors, typography } from '../Styles'

const PADDING = spacing.base
const BORDER_WIDTH = 2

const BASE = ({ isDisabled }) => ({
  outlineOffset: 0,
  padding: `${spacing.moderate - BORDER_WIDTH}px ${spacing.moderate}px`,
  borderRadius: constants.borderRadius.small,
  boxShadow: constants.boxShadows.input,
  width: '100%',
  cursor: isDisabled ? 'not-allowed' : '',
  '&:focus': {
    border: constants.borders.input,
    outline: colors.key.one,
  },
})

const STYLES = {
  PRIMARY: {
    color: colors.structure.black,
    backgroundColor: colors.structure.white,
  },
  SECONDARY: {
    color: colors.structure.white,
    backgroundColor: colors.structure.smoke,
    '&:focus': {
      color: colors.structure.black,
      backgroundColor: colors.structure.white,
    },
  },
}

const BORDER_STYLES = ({ hasError }) => ({
  border: hasError ? constants.borders.error : constants.borders.transparent,
  borderWidth: BORDER_WIDTH,
})

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

const Input = ({
  id,
  variant,
  type,
  label,
  value,
  onChange,
  hasError,
  errorMessage,
  after,
  isRequired,
  isDisabled,
  ...props
}) => {
  return (
    <div css={{ paddingTop: PADDING }}>
      <label
        htmlFor={id}
        css={{
          display: 'block',
          paddingBottom: PADDING,
          color: colors.structure.zinc,
        }}
      >
        {label}

        {!!isRequired && (
          <span css={{ color: colors.signal.warning.base }}> *</span>
        )}
      </label>
      <div css={{ position: 'relative' }}>
        <input
          id={id}
          type={type}
          name={id}
          value={value}
          onChange={onChange}
          css={[
            BASE({ isDisabled }),
            STYLES[variant],
            BORDER_STYLES({ hasError }),
          ]}
          disabled={isDisabled}
          // eslint-disable-next-line react/jsx-props-no-spreading
          {...props}
        />
        {!!after && (
          <div
            css={{
              position: 'absolute',
              right: 0,
              top: 0,
              bottom: 0,
              display: 'flex',
              justifyContent: 'center',
            }}
          >
            {after}
          </div>
        )}
      </div>
      <div css={{ paddingTop: PADDING }}>
        {hasError && (
          <span
            css={{
              color: colors.signal.warning.base,
              fontStyle: typography.style.italic,
            }}
          >
            {errorMessage}
          </span>
        )}
        &nbsp;
      </div>
    </div>
  )
}

Input.defaultProps = {
  after: false,
  errorMessage: '',
  isRequired: false,
  isDisabled: false,
}

Input.propTypes = {
  id: PropTypes.string.isRequired,
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  type: PropTypes.oneOf(['text', 'password']).isRequired,
  label: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  hasError: PropTypes.bool.isRequired,
  errorMessage: PropTypes.string,
  after: PropTypes.node,
  isRequired: PropTypes.bool,
  isDisabled: PropTypes.bool,
}

export default Input
