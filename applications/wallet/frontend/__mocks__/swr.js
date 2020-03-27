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

export const { cache } = jest.requireActual('swr')

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
