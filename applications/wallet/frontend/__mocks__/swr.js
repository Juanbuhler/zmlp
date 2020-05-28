import { createElement } from 'react'

const noop = () => () => {}

/**
 * <SWRConfig />
 */

export const SWRConfig = ({ children, ...rest }) =>
  createElement('SWRConfig', rest, children)

/**
 * unmock
 */

export const { useSWRPages } = jest.requireActual('swr')

/**
 * cache
 */

let mockCacheKeys = []

export const __setMockCacheKeys = (data) => {
  mockCacheKeys = data
}

export const cache = {
  keys: () => {
    return mockCacheKeys
  },
  clear: () => {},
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
