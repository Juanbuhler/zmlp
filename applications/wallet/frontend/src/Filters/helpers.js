import Router from 'next/router'
import utf8 from 'utf8'

import { getQueryString } from '../Fetch/helpers'

export const ACTIONS = {
  ADD_VALUE: 'ADD_VALUE',
  ADD_FILTER: 'ADD_FILTER',
  ADD_FILTERS: 'ADD_FILTERS',
  UPDATE_FILTER: 'UPDATE_FILTER',
  APPLY_SIMILARITY: 'APPLY_SIMILARITY',
  DELETE_FILTER: 'DELETE_FILTER',
  CLEAR_FILTERS: 'CLEAR_FILTERS',
}

export const getNewFacets = ({
  facets,
  isSelected,
  hasModifier,
  facetIndex,
  key,
}) => {
  if (hasModifier && isSelected) {
    return [...facets.slice(0, facetIndex), ...facets.slice(facetIndex + 1)]
  }

  if (hasModifier && !isSelected) {
    return [...facets, key]
  }

  if (isSelected && facets.length === 1) {
    return []
  }

  if (isSelected && facets.length > 1) {
    return [key]
  }

  return [key]
}

export const encode = ({ filters }) => {
  return btoa(utf8.encode(JSON.stringify(filters)))
}

export const decode = ({ query }) => {
  try {
    return JSON.parse(utf8.decode(atob(query)))
  } catch (error) {
    return []
  }
}

export const cleanup = ({ query }) => {
  const filters = decode({ query }).filter(
    ({ type, values = {}, isDisabled = false }) => {
      if (type === 'label' && values.labels) {
        return values.labels.length > 0 && !isDisabled
      }

      return Object.keys(values).length > 0 && !isDisabled
    },
  )

  return encode({ filters })
}

export const dispatch = ({ type, payload }) => {
  switch (type) {
    /**
     * Checks if a filter is already present or not first, and then
     * adds a single filter with a single value if the filter is **not** already present
     * or updates a single filter with a single value if the filter **is** already present
     * => idempotent
     */
    case ACTIONS.ADD_VALUE: {
      const { pathname, projectId, assetId, filter, query: q } = payload

      const filters = decode({ query: q })

      const filterIndex = filters.findIndex(
        ({ attribute }) => attribute === filter.attribute,
      )

      if (filterIndex === -1) {
        dispatch({
          type: ACTIONS.ADD_FILTER,
          payload: {
            pathname,
            projectId,
            assetId,
            filter,
            query: q,
          },
        })

        break
      }

      dispatch({
        type: ACTIONS.UPDATE_FILTER,
        payload: {
          pathname,
          projectId,
          assetId,
          filters,
          updatedFilter: filter,
          filterIndex,
        },
      })

      break
    }

    /**
     * Checks if a filter is already present or not first, and then
     * adds a single filter to the query, only if that filter is not already present
     * or does nothing otherwise
     * => idempotent
     */
    case ACTIONS.ADD_FILTER: {
      const { pathname, projectId, assetId, filter, query: q } = payload

      const filters = decode({ query: q })

      const filterIndex = filters.findIndex(
        ({ attribute }) => attribute === filter.attribute,
      )

      if (filterIndex > -1) break

      const query = encode({ filters: [filter, ...filters] })

      Router.push(
        {
          pathname,
          query: { projectId, id: assetId, query },
        },
        `${pathname.replace('[projectId]', projectId)}${getQueryString({
          id: assetId,
          query,
        })}`,
      )

      break
    }

    /**
     * Adds one or multiple filters to the query without ensuring whether those
     * filters are already present or not
     * => NOT idempotent
     */
    case ACTIONS.ADD_FILTERS: {
      const { pathname, projectId, assetId, filters, newFilters } = payload

      const query = encode({ filters: [...newFilters, ...filters] })

      Router.push(
        {
          pathname,
          query: { projectId, assetId, query },
        },
        `${pathname.replace('[projectId]', projectId)}${getQueryString({
          assetId,
          query,
        })}`,
      )

      break
    }

    case ACTIONS.UPDATE_FILTER: {
      const {
        pathname,
        projectId,
        assetId,
        filters,
        updatedFilter,
        filterIndex,
      } = payload

      const query = encode({
        filters: [
          ...filters.slice(0, filterIndex),
          updatedFilter,
          ...filters.slice(filterIndex + 1),
        ],
      })

      Router.push(
        {
          pathname,
          query: { projectId, assetId, query },
        },
        `${pathname.replace('[projectId]', projectId)}${getQueryString({
          assetId,
          query,
        })}`,
      )

      break
    }

    case ACTIONS.DELETE_FILTER: {
      const { pathname, projectId, assetId, filters, filterIndex } = payload

      const newFilters = [
        ...filters.slice(0, filterIndex),
        ...filters.slice(filterIndex + 1),
      ]

      const query = newFilters.length > 0 ? encode({ filters: newFilters }) : ''

      Router.push(
        {
          pathname,
          query: { projectId, assetId, query },
        },
        `${pathname.replace('[projectId]', projectId)}${getQueryString({
          assetId,
          query,
        })}`,
      )

      break
    }

    case ACTIONS.APPLY_SIMILARITY: {
      const { projectId, assetId, thumbnailId, query: q, attribute } = payload

      const filters = decode({ query: q })

      const similarityFilterIndex = filters.findIndex(
        (filter) => filter.type === 'similarity',
      )

      const minScore =
        similarityFilterIndex > -1
          ? filters[similarityFilterIndex].values.minScore || 0.75
          : 0.75

      const similarityFilter = {
        type: 'similarity',
        attribute,
        values: { ids: [thumbnailId], minScore },
      }

      const combinedFilters =
        similarityFilterIndex === -1
          ? [similarityFilter, ...filters]
          : [
              ...filters.slice(0, similarityFilterIndex),
              similarityFilter,
              ...filters.slice(similarityFilterIndex + 1),
            ]

      const query = encode({ filters: combinedFilters })

      Router.push(
        {
          pathname: '/[projectId]/visualizer',
          query: {
            projectId,
            assetId,
            query,
          },
        },
        `/${projectId}/visualizer${getQueryString({
          assetId,
          query,
        })}`,
      )

      break
    }

    case ACTIONS.CLEAR_FILTERS: {
      const { pathname, projectId, assetId } = payload

      Router.push(
        {
          pathname,
          query: { projectId, assetId },
        },
        `${pathname.replace('[projectId]', projectId)}${getQueryString({
          assetId,
        })}`,
      )

      break
    }

    default:
      break
  }
}
