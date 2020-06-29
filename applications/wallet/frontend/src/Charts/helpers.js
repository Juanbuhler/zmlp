const FOUR_K = 4096

const MIN_COL_WIDTH = 100

const MAX_COLS = Math.ceil(FOUR_K / MIN_COL_WIDTH)

const BREAKPOINTS = [...new Array(MAX_COLS)].map((_, index) => index)

export const MIN_ROW_HEIGHT = 50

export const breakpoints = BREAKPOINTS.reduce(
  (acc, bp, index) => ({ ...acc, [bp]: MIN_COL_WIDTH * (index + 1) }),
  {},
)

export const cols = BREAKPOINTS.reduce(
  (acc, bp, index) => ({ ...acc, [bp]: index + 1 }),
  {},
)

export const setAllLayouts = ({ setLayouts }) => (_, allLayouts) => {
  const value = Object.entries(allLayouts).reduce(
    (acc, [bp, values]) => ({
      ...acc,
      [bp]: values.map((v) => ({
        ...v,
        w: v.w > 4 ? v.w : 4,
        minW: 4,
        h: v.h > 4 ? v.h : 4,
        minH: 4,
      })),
    }),
    {},
  )

  setLayouts({ value })
}
