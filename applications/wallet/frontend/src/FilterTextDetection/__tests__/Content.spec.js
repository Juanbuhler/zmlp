import TestRenderer, { act } from 'react-test-renderer'

import FilterTextDetection from '../Content'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filters/Reset', () => 'FiltersReset')

const noop = () => () => {}

describe('<FilterTextDetection />', () => {
  it('should select a facet', () => {
    const filter = {
      type: 'textContent',
      attribute: 'analysis.zvi-text-content',
      values: {},
    }

    const mockFn = jest.fn()
    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterTextDetection
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Attempt to submit without search
    act(() => {
      component.root.findByProps({ type: 'submit' }).props.onClick()
      component.root
        .findByProps({ method: 'post' })
        .props.onSubmit({ preventDefault: mockFn })
    })

    // Fill in search
    act(() => {
      component.root
        .findByProps({ type: 'search' })
        .props.onChange({ target: { value: 'cats' } })
    })

    // Submit search
    act(() => {
      component.root.findByProps({ type: 'submit' }).props.onClick()
    })

    const encodedQuery = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: 'analysis.zvi-text-content',
          values: { query: 'cats' },
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: encodedQuery,
        },
      },
      `/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=${encodedQuery}`,
    )
  })

  it('should clear a text detection', () => {
    const filter = {
      type: 'textContent',
      attribute: 'analysis.zvi-text-content',
      values: { query: ['cats'] },
    }

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <FilterTextDetection
        projectId={PROJECT_ID}
        assetId=""
        filters={[filter]}
        filter={filter}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Clear Text Detection' })
        .props.onClick({ preventDefault: noop })
    })

    const encodedQuery = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: 'analysis.zvi-text-content',
          values: {},
        },
      ]),
    )

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: encodedQuery,
        },
      },
      `/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=${encodedQuery}`,
    )
  })
})
