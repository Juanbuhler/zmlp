import TestRenderer, { act } from 'react-test-renderer'

import fields from '../__mocks__/fields'

import FiltersReset from '../Reset'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = ''

describe('<FiltersReset />', () => {
  it('should reset properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <FiltersReset
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Reset' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalled()

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                attribute: 'clip.length',
                type: 'range',
                values: {},
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3siYXR0cmlidXRlIjoiY2xpcC5sZW5ndGgiLCJ0eXBlIjoicmFuZ2UiLCJ2YWx1ZXMiOnt9fV0=',
    )
  })

  it('should switch properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'range' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <FiltersReset
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ defaultValue: 'range' })
        .props.onChange({ target: { value: 'exists' } })
    })

    expect(mockFn).toHaveBeenCalled()

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                attribute: 'clip.length',
                type: 'exists',
                values: { exists: true },
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3siYXR0cmlidXRlIjoiY2xpcC5sZW5ndGgiLCJ0eXBlIjoiZXhpc3RzIiwidmFsdWVzIjp7ImV4aXN0cyI6dHJ1ZX19XQ==',
    )
  })

  it('should switch properly', () => {
    const filters = [{ attribute: 'clip.length', type: 'exists' }]

    const mockRouterPush = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const mockFn = jest.fn()

    require('swr').__setMockUseSWRResponse({ data: fields })

    const component = TestRenderer.create(
      <FiltersReset
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        filters={filters}
        filter={filters[0]}
        filterIndex={0}
        onReset={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ defaultValue: 'exists' })
        .props.onChange({ target: { value: 'range' } })
    })

    expect(mockFn).toHaveBeenCalled()

    expect(mockRouterPush).toHaveBeenCalledWith(
      {
        pathname: '/[projectId]/visualizer',
        query: {
          projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
          id: '',
          query: btoa(
            JSON.stringify([
              {
                attribute: 'clip.length',
                type: 'range',
                values: {},
              },
            ]),
          ),
        },
      },
      '/76917058-b147-4556-987a-0a0f11e46d9b/visualizer?query=W3siYXR0cmlidXRlIjoiY2xpcC5sZW5ndGgiLCJ0eXBlIjoicmFuZ2UiLCJ2YWx1ZXMiOnt9fV0=',
    )
  })
})
