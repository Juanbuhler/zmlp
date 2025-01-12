import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, spacing, typography, colors } from '../Styles'

import chartShape from '../Chart/shape'

import { encode, cleanup, ACTIONS, dispatch } from '../Filters/helpers'
import { getQueryString } from '../Fetch/helpers'

import Button, { VARIANTS } from '../Button'

import FilterSvg from '../Icons/filter.svg'

const ChartRangeContent = ({ chart: { type, id, attribute } }) => {
  const {
    pathname,
    query: { projectId, query },
  } = useRouter()

  const visuals = encode({ filters: [{ type, id, attribute }] })

  const q = cleanup({ query })

  const queryString = getQueryString({ query: q, visuals })

  const { data = [] } = useSWR(
    `/api/v1/projects/${projectId}/visualizations/load/${queryString}`,
  )

  const { results = {}, defaultFilterType } =
    data.find((r) => r.id === id) || {}

  return (
    <div
      css={{
        display: 'flex',
        flexWrap: 'wrap',
        '> div': {
          display: 'flex',
          justifyContent: 'flex-end',
          flexDirection: 'column',
          width: `calc(${(1 / 3) * 100}% - ${(spacing.normal * 2) / 3}px)`,
          marginRight: spacing.normal,
          paddingBottom: spacing.normal,
          ':nth-of-type(3n)': { marginRight: 0 },
        },
      }}
    >
      {Object.entries(results).map(([key, value]) => (
        <div key={key}>
          <div
            css={{
              textTransform: 'uppercase',
              fontFamily: typography.family.condensed,
              color: colors.structure.zinc,
              paddingBottom: spacing.small,
            }}
          >
            {key}
          </div>
          <div
            css={{
              border: constants.borders.regular.smoke,
              borderRadius: constants.borderRadius.small,
              padding: spacing.moderate,
              paddingLeft: spacing.normal,
              paddingRight: spacing.normal,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {value?.toLocaleString() || '-'}
          </div>
        </div>
      ))}
      <div>
        <div
          css={{
            height: typography.height.regular,
            marginBottom: spacing.small,
          }}
        >
          &nbsp;
        </div>
        <div css={{ display: 'flex' }}>
          <Button
            aria-label="Add Filter"
            variant={VARIANTS.MICRO}
            onClick={() => {
              dispatch({
                type: ACTIONS.ADD_FILTER,
                payload: {
                  pathname,
                  projectId,
                  filter: { type: defaultFilterType, attribute, values: {} },
                  query,
                },
              })
            }}
            css={{ flex: 1, display: 'flex' }}
          >
            <div css={{ display: 'flex', alignItems: 'center' }}>
              <div css={{ display: 'flex', paddingRight: spacing.small }}>
                <FilterSvg height={constants.icons.regular} />
              </div>
              Add Filter
            </div>
          </Button>
        </div>
      </div>
    </div>
  )
}

ChartRangeContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartRangeContent
