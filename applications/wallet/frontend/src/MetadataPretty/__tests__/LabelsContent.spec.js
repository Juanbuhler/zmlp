import TestRenderer from 'react-test-renderer'

import labels from '../__mocks__/labels'
import models from '../../Models/__mocks__/models'

import MetadataPrettyLabelsContent from '../LabelsContent'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

describe('<MetadataPrettyLabelsContent />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: labels })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <MetadataPrettyLabelsContent
        projectId={PROJECT_ID}
        assetId={ASSET_ID}
        models={models.results}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
