import TestRenderer from 'react-test-renderer'

import Breadcrumbs from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Breadcrumbs />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <Breadcrumbs
        crumbs={[
          { title: 'Bread', href: '/[projectId]/bread' },
          { title: 'Crumb', href: false },
        ]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
