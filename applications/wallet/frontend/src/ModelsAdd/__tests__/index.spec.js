import TestRenderer, { act } from 'react-test-renderer'

import modelTypes from '../../ModelTypes/__mocks__/modelTypes'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import ModelsAdd from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const MODEL_ID = '621bf775-89d9-1244-9596-d6df43f1ede5'

describe('<ModelsAdd />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/models/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: modelTypes })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <ModelsAdd />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Input invalid name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: '' } })
    })

    // Input valid name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'My New Model Really' } })
    })

    // Input valid description
    act(() => {
      component.root
        .findByProps({ id: 'description' })
        .props.onChange({ target: { value: 'A cool new description' } })
    })

    // Select model source
    act(() => {
      component.root
        .findAllByProps({ name: 'source' })[1]
        .props.onClick({ value: 'UPLOAD' })
    })

    // Select model type
    act(() => {
      component.root
        .findAllByProps({ name: 'modelType' })[0]
        .props.onClick({ value: 'PYTORCH_MODEL_ARCHIVE' })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Create New Model' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Create New Model' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ id: MODEL_ID }), {
      headers: { 'content-type': 'application/json' },
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ type: 'submit', children: 'Create New Model' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(5)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/models/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        name: 'my-new-model-really',
        description: 'A cool new description',
        type: 'PYTORCH_MODEL_ARCHIVE',
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      `/[projectId]/models/[modelId]?action=add-model-success`,
      `/${PROJECT_ID}/models/${MODEL_ID}`,
    )
  })
})
