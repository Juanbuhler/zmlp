import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { colors, spacing } from '../Styles'

import Select from '../Select'
import FilterReset from '../Filter/Reset'
import FilterBuckets from '../FilterBuckets'

import { dispatch, ACTIONS, encode } from '../Filters/helpers'

export const noop = () => {}

const FilterLabel = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    modelId,
    values: { labels = [], scope = 'all' },
  },
  filterIndex,
}) => {
  const encodedFilter = encode({ filters: { type, modelId } })

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )
  const { results } = data || {}

  const { buckets = [] } = results || {}

  return (
    <>
      <FilterReset
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={noop}
      />

      <div css={{ height: spacing.moderate }} />

      <Select
        key={scope}
        label="Scope"
        options={[
          { value: 'all', label: 'All' },
          { value: 'test', label: 'Test' },
          { value: 'train', label: 'Train' },
        ]}
        defaultValue={scope}
        onChange={({ value }) => {
          dispatch({
            type: ACTIONS.UPDATE_FILTER,
            payload: {
              pathname,
              projectId,
              assetId,
              filters,
              updatedFilter: {
                ...filter,
                values: { labels, scope: value },
              },
              filterIndex,
            },
          })
        }}
        isRequired={false}
        isDisabled={labels.length === 0}
        style={{
          width: '100%',
          height: 'auto',
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
          backgroundColor: colors.structure.smoke,
        }}
      />

      <div css={{ height: spacing.moderate }} />

      <FilterBuckets
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        buckets={buckets}
        searchString=""
      />
    </>
  )
}

FilterLabel.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabel
