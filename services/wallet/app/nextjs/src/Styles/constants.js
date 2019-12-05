import colors from './colors'

const borderRadius = {
  small: 2,
}

const borders = {
  default: `1px solid ${colors.grey5}`,
  transparent: `1px solid transparent`,
}

const opacity = {
  half: 0.59,
}

const boxShadows = {
  default: `0 2px 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  input: `inset 0 1px 3px 0 transparent`,
}

const constants = {
  borderRadius,
  borders,
  opacity,
  boxShadows,
}

export default constants
