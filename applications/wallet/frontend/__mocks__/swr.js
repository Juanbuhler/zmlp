import { createElement } from 'react'

const noop = () => () => {}

/**
 * <SWRConfig />
 */

export const SWRConfig = ({ children, ...rest }) =>
  createElement('SWRConfig', rest, children)

/**
 * cache
 */

let mockCacheKeys = []

export const __setMockCacheKeys = (data) => {
  mockCacheKeys = data
}

let mockCacheDeleteFn = () => {}

export const __setMockCacheDeleteFn = (fn) => {
  mockCacheDeleteFn = fn
}

export const cache = {
  keys: () => {
    return mockCacheKeys
  },
  clear: () => {},
  delete: (...args) => {
    mockCacheDeleteFn(...args)
  },
}

/**
 * mutate
 */

let mockMutateFn = () => {}

export const __setMockMutateFn = (fn) => {
  mockMutateFn = fn
}

export const mutate = (_, cb) => {
  return mockMutateFn(typeof cb === 'function' ? cb() : cb)
}

/**
 * useSWR
 */

let mockUseSWRResponse = {}

export const __setMockUseSWRResponse = (data) => {
  mockUseSWRResponse = { revalidate: noop, ...data }
}

const useSWR = () => {
  return mockUseSWRResponse
}

export default useSWR

/**
 * useSWRInfinite
 */

const { useSWRInfinite: actualUseSWRInfinite } = jest.requireActual('swr')

let mockData = []

export const __setMockUseSWRInfiniteResponse = (data) => {
  mockData = data
}

export const useSWRInfinite = (getKey, fetcher, options) => {
  const { mutate: m, size, setSize } = actualUseSWRInfinite(
    getKey,
    fetcher,
    options,
  )

  return {
    data: mockData,
    mutate: m,
    size,
    setSize,
  }
}
