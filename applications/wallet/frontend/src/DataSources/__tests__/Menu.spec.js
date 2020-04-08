import TestRenderer, { act } from 'react-test-renderer'

import DataSourcesMenu from '../Menu'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATA_SOURCE_ID = '2f0de857-95fd-120e-85f3-0242ac120002'

describe('<DataSourcesMenu />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()
    const mockRouter = jest.fn()

    require('next/router').__setMockPushFunction(mockRouter)

    fetch.mockResponseOnce('{}')

    const component = TestRenderer.create(
      <DataSourcesMenu
        projectId={PROJECT_ID}
        dataSourceId={DATA_SOURCE_ID}
        revalidate={mockFn}
      />,
    )

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Select Delete
    act(() => {
      component.root.findByProps({ children: 'Delete' }).props.onClick()
    })

    // Cancel
    act(() => {
      component.root.findByProps({ children: 'Cancel' }).props.onClick()
    })

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Delete
    act(() => {
      component.root.findByProps({ children: 'Delete' }).props.onClick()
    })

    // Confirm
    await act(async () => {
      component.root
        .findByProps({ children: 'Delete Permanently' })
        .props.onClick()
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/data_sources/${DATA_SOURCE_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'DELETE',
    })

    expect(mockFn).toHaveBeenCalledWith()

    // Open Menu
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Toggle Actions Menu' })
        .props.onClick()
    })

    // Select Edit
    await act(async () => {
      component.root.findByProps({ children: 'Edit' }).props.onClick()
    })

    expect(mockRouter).toHaveBeenCalledWith(
      '/[projectId]/data-sources/[dataSourceId]/edit',
      `/${PROJECT_ID}/data-sources/${DATA_SOURCE_ID}/edit`,
    )
  })
})
