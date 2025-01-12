import TestRenderer from 'react-test-renderer'

import dataSources from '../__mocks__/dataSources'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import DataSources from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const JOB_ID = '1a1d45af-7477-1396-ae57-a618e8efb91f'

describe('<DataSources />', () => {
  it('should render properly with no data sources', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources',
      query: { projectId: PROJECT_ID, action: 'delete-datasource-success' },
    })

    require('swr').__setMockUseSWRResponse({
      data: {
        count: 0,
        next: null,
        previous: null,
        results: [],
      },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSources />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data sources', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources',
      query: {
        projectId: PROJECT_ID,
        action: 'add-datasource-success',
        jobId: JOB_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: dataSources,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSources />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with edit success', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources',
      query: { projectId: PROJECT_ID, action: 'edit-datasource-success' },
    })

    require('swr').__setMockUseSWRResponse({
      data: dataSources,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSources />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with scan success', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/data-sources',
      query: {
        projectId: PROJECT_ID,
        action: 'scan-datasource-success',
        jobId: JOB_ID,
      },
    })

    require('swr').__setMockUseSWRResponse({
      data: dataSources,
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <DataSources />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
