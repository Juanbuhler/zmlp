import colors from './colors'

const borderRadius = {
  small: 2,
  medium: 4,
  large: 14,
  round: 32,
}

const borders = {
  default: `1px solid ${colors.structure.mattGrey}`,
  transparent: `1px solid transparent`,
  separator: `1px solid ${colors.structure.zinc}`,
  error: `1px solid ${colors.signal.warning.base}`,
  tableRow: `1px solid ${colors.structure.steel}`,
}

const opacity = {
  half: 0.5,
  full: 1,
}

const boxShadows = {
  default: `0 2px 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  input: `inset 0 1px 3px 0 transparent`,
  menu: `0 4px 7px 0 ${colors.structure.black}`,
  navBar: `0 0 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  dropdown: `0 2px 6px 0 ${colors.structure.black}`,
  modal: `0 0 8px 5px rgba(0, 0, 0, ${opacity.half})`,
  table: `0 0 5px 0 ${colors.structure.black}`,
  tableRow: `0 0 5px 0 rgba(0, 0, 0, ${opacity.half})`,
}

const constants = {
  borderRadius,
  borders,
  opacity,
  boxShadows,
  navbar: {
    height: 44,
  },
  pageTitle: {
    height: 61,
  },
  tableHeader: {
    height: 45,
  },
}

export default constants
