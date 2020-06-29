import PropTypes from 'prop-types'
import useResizeObserver from 'use-resize-observer'
import { Responsive as ResponsiveGridLayout } from 'react-grid-layout'

import chartShape from '../Chart/shape'

import { spacing } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'

import ChartFacet from '../ChartFacet'
import ChartRange from '../ChartRange'

import { MIN_ROW_HEIGHT, breakpoints, cols, setAllLayouts } from './helpers'

const Charts = ({ projectId, charts, dispatch }) => {
  const { ref, width = 1200 } = useResizeObserver()

  const [layouts, setLayouts] = useLocalStorageState({
    key: `Charts.${projectId}`,
    initialValue: {},
  })

  return (
    <div ref={ref}>
      <ResponsiveGridLayout
        width={width}
        layouts={layouts}
        breakpoints={breakpoints}
        cols={cols}
        margin={[spacing.normal, spacing.normal]}
        containerPadding={[0, 0]}
        rowHeight={MIN_ROW_HEIGHT}
        onLayoutChange={setAllLayouts({ setLayouts })}
      >
        {charts
          .filter(({ id }) => !!id)
          .map((chart, index) => {
            switch (chart.type) {
              case 'facet': {
                return (
                  <div key={chart.id}>
                    <ChartFacet
                      chart={chart}
                      chartIndex={index}
                      dispatch={dispatch}
                    />
                  </div>
                )
              }

              case 'range': {
                return (
                  <div key={chart.id}>
                    <ChartRange
                      chart={chart}
                      chartIndex={index}
                      dispatch={dispatch}
                    />
                  </div>
                )
              }

              default:
                return null
            }
          })}
      </ResponsiveGridLayout>
    </div>
  )
}

Charts.propTypes = {
  projectId: PropTypes.string.isRequired,
  charts: PropTypes.arrayOf(PropTypes.shape(chartShape).isRequired).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default Charts
