import TestRenderer, { act } from 'react-test-renderer'

import DatasetsMenu from '../Menu'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('<DatasetsMenu />', () => {
  it('should render properly', async () => {
    const mockRouterPush = jest.fn()
    const mockDispatch = jest.fn()

    require('next/router').__setMockPushFunction(mockRouterPush)

    const component = TestRenderer.create(
      <DatasetsMenu
        projectId={PROJECT_ID}
        datasetId={DATASET_ID}
        name="My Amazing Dataset"
        setDatasetFields={mockDispatch}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

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

    expect(mockDispatch).toHaveBeenCalledWith({ datasetId: '', labels: {} })

    expect(fetch.mock.calls.length).toEqual(3)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      headers: {
        'X-CSRFToken': 'CSRF_TOKEN',
        'Content-Type': 'application/json;charset=UTF-8',
      },
      method: 'DELETE',
    })

    expect(mockRouterPush).toHaveBeenCalledWith(
      '/[projectId]/datasets?action=delete-dataset-success',
      `/${PROJECT_ID}/datasets`,
    )
  })
})
