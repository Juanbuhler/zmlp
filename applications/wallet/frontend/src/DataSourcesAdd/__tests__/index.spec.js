import TestRenderer, { act } from 'react-test-renderer'

import providers from '../../Providers/__mocks__/providers'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import DataSourcesAdd from '..'

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<DataSourcesAdd />', () => {
  it('should render properly with no credentials', async () => {
    const mockFn = jest.fn()
    const mockScrollTo = jest.fn()
    Object.defineProperty(global.window, 'scrollTo', { value: mockScrollTo })

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: providers })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSourcesAdd />
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
        .props.onChange({ target: { value: 'My Data Source' } })
    })

    // Input source
    act(() => {
      component.root
        .findByProps({ id: 'source-selection' })
        .props.onChange({ target: { value: 'AWS' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Input source
    act(() => {
      component.root
        .findByProps({ id: 'source-selection' })
        .props.onChange({ target: { value: 'AZURE' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Input source
    act(() => {
      component.root
        .findByProps({ id: 'source-selection' })
        .props.onChange({ target: { value: 'GCP' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Input invalid uri
    act(() => {
      component.root
        .findByProps({ id: 'uri' })
        .props.onChange({ target: { value: 'fooBar://zorroa-dev-data/' } })
    })

    // Input invalid uri
    act(() => {
      component.root
        .findByProps({ id: 'uri' })
        .props.onChange({ target: { value: 'gs://' } })
    })

    // Input valid uri
    // No credentials required
    act(() => {
      component.root
        .findByProps({ id: 'uri' })
        .props.onChange({ target: { value: 'gs://zorroa-dev-data' } })
    })

    // Select file type
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'images' })
        .props.onClick()
    })

    // Select disabled module
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'gcp-label-detection' })
        .props.onClick({ preventDefault: noop })
    })

    // Select module
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'zvi-label-detection' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Failure
    fetch.mockResponseOnce(JSON.stringify({ name: ['Name already in use'] }), {
      status: 400,
    })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockScrollTo).toHaveBeenCalledWith(0, 0)
    mockScrollTo.mockClear()

    // Mock Unknown Failure
    fetch.mockRejectOnce(null, { status: 500 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Data Source Created' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(3)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/data_sources/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        name: 'My Data Source',
        uri: 'gs://zorroa-dev-data',
        credentials: {},
        file_types: ['gif', ' png', ' jpg', ' jpeg', ' tif', ' tiff', ' psd'],
        modules: ['zvi-label-detection'],
      }),
    })

    expect(mockFn).toHaveBeenCalledWith(
      '/[projectId]/data-sources?action=add-datasource-success',
      `/${PROJECT_ID}/data-sources?action=add-datasource-success`,
    )
  })

  it('should render properly with optional credentials', () => {
    const mockFn = jest.fn()
    const mockScrollTo = jest.fn()
    Object.defineProperty(global.window, 'scrollTo', { value: mockScrollTo })

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: providers })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSourcesAdd />
      </User>,
    )

    // Input valid name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'My Data Source' } })
    })

    // Input source
    act(() => {
      component.root
        .findByProps({ id: 'source-selection' })
        .props.onChange({ target: { value: 'GCP' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Input valid uri
    act(() => {
      component.root
        .findByProps({ id: 'uri' })
        .props.onChange({ target: { value: 'gs://zorroa-dev-data' } })
    })

    // Input optional credential
    act(() => {
      component.root
        .findByProps({ id: 'service_account_json_key' })
        .props.onChange({ target: { value: 'jkdT9Uherdozguie89FHIJS' } })
    })

    // Select file type
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'images' })
        .props.onClick()
    })

    // Click Submit
    act(async () => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/data_sources/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        name: 'My Data Source',
        uri: 'gs://zorroa-dev-data',
        credentials: {
          type: 'GCP',
          service_account_json_key: 'jkdT9Uherdozguie89FHIJS',
        },
        file_types: ['gif', ' png', ' jpg', ' jpeg', ' tif', ' tiff', ' psd'],
        modules: [],
      }),
    })
  })

  it('should render properly with required credentials', () => {
    const mockFn = jest.fn()
    const mockScrollTo = jest.fn()
    Object.defineProperty(global.window, 'scrollTo', { value: mockScrollTo })

    require('next/router').__setMockPushFunction(mockFn)

    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources/add',
      query: { projectId: PROJECT_ID },
    })

    require('swr').__setMockUseSWRResponse({ data: providers })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSourcesAdd />
      </User>,
    )

    // Input valid name
    act(() => {
      component.root
        .findByProps({ id: 'name' })
        .props.onChange({ target: { value: 'My Data Source' } })
    })

    // Input source
    act(() => {
      component.root
        .findByProps({ id: 'source-selection' })
        .props.onChange({ target: { value: 'AWS' } })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Input uri
    act(() => {
      component.root
        .findByProps({ id: 'uri' })
        .props.onChange({ target: { value: 's3://zorroa-dev-data' } })
    })

    // Input invalid required credential
    act(() => {
      component.root
        .findByProps({ id: 'aws_access_key_id' })
        .props.onChange({ target: { value: '' } })
    })

    // Input valid required credentials
    act(() => {
      component.root
        .findByProps({ id: 'aws_access_key_id' })
        .props.onChange({ target: { value: 'sdlkmsoijes;kfjnskajnre' } })
    })

    act(() => {
      component.root
        .findByProps({ id: 'aws_secret_access_key' })
        .props.onChange({ target: { value: 'sdkjfipuenkjrfewrf' } })
    })

    // Select file type
    act(() => {
      component.root
        .findByProps({ type: 'checkbox', value: 'images' })
        .props.onClick()
    })

    // Click Submit
    act(async () => {
      component.root
        .findByProps({ children: 'Create Data Source' })
        .props.onClick({ preventDefault: noop })
    })

    expect(fetch.mock.calls.length).toEqual(1)

    expect(fetch.mock.calls[0][0]).toEqual(
      `/api/v1/projects/${PROJECT_ID}/data_sources/`,
    )

    expect(fetch.mock.calls[0][1]).toEqual({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': 'CSRF_TOKEN',
      },
      body: JSON.stringify({
        name: 'My Data Source',
        uri: 's3://zorroa-dev-data',
        credentials: {
          type: 'AWS',
          aws_access_key_id: 'sdlkmsoijes;kfjnskajnre',
          aws_secret_access_key: 'sdkjfipuenkjrfewrf',
        },
        file_types: ['gif', ' png', ' jpg', ' jpeg', ' tif', ' tiff', ' psd'],
        modules: [],
      }),
    })
  })
})
