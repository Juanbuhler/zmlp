import colors from './colors'

const borderRadius = {
  small: 2,
}

const borders = {
  default: `1px solid ${colors.structureShades.mattGrey}`,
  transparent: `1px solid transparent`,
  separator: `1px solid ${colors.rocks.pewter}`,
  tableRow: `1px solid ${colors.structureShades.steel}`,
}

const opacity = {
  half: 0.5,
}

const boxShadows = {
  default: `0 2px 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  input: `inset 0 1px 3px 0 transparent`,
  menu: `0 4px 7px 0 ${colors.structureShades.black}`,
  navBar: `0 0 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  dropdown: `0 2px 6px 0 ${colors.structureShades.black}`,
  modal: `0 0 8px 5px rgba(0, 0, 0, ${opacity.half})`,
  table: `0 0 5px 0 ${colors.structureShades.black}`,
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
}

export default constants
