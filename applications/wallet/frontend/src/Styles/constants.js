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
  tabs: `1px solid ${colors.structure.iron}`,
  error: `2px solid ${colors.signal.warning.base}`,
  success: `1px solid ${colors.signal.grass.base}`,
  tableRow: `1px solid ${colors.structure.steel}`,
  input: `2px solid ${colors.key.one}`,
  divider: `1px solid ${colors.structure.smoke}`,
  pill: `2px solid ${colors.structure.steel}`,
  assetInactive: `4px solid ${colors.transparent}`,
  assetHover: `4px solid ${colors.structure.white}`,
  assetSelected: `4px solid ${colors.signal.sky.base}`,
}

const opacity = {
  third: 0.3,
  half: 0.5,
  eighth: 0.8,
  full: 1,
}

const boxShadows = {
  default: `0 2px 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  input: `inset 0 1px 3px 0 transparent`,
  menu: `0 4px 7px 0 ${colors.structure.black}`,
  navBar: `0 0 4px 0 rgba(0, 0, 0, ${opacity.half})`,
  dropdown: `0 2px 6px 0 ${colors.structure.black}`,
  tableRow: `0 0 5px 0 rgba(0, 0, 0, ${opacity.half})`,
  metadata: `-3px 0 3px 0px rgba(0, 0, 0,${opacity.third})`,
  infoBar: `0px 3px 3px 0 rgba(0, 0, 0, ${opacity.third})`,
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
  form: {
    maxWidth: 470,
  },
  paragraph: {
    maxWidth: 600,
  },
}

export default constants
