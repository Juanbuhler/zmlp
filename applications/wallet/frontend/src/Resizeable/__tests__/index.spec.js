import TestRenderer from 'react-test-renderer'

import Resizeable from '..'

const noop = () => () => {}

describe('<Resizeable />', () => {
  it('should render properly with the cursor left', () => {
    const component = TestRenderer.create(
      <Resizeable
        minSize={400}
        storageName="yoga-div"
        openToThe="left"
        onMouseUp={noop}
        childFixedSize={0}
      >
        Yoga div
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with the cursor right', () => {
    const component = TestRenderer.create(
      <Resizeable
        minSize={400}
        storageName="yoga-div"
        openToThe="right"
        onMouseUp={noop}
        childFixedSize={300}
      >
        Yoga div
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with the cursor top', () => {
    const component = TestRenderer.create(
      <Resizeable
        minSize={400}
        storageName="yoga-div"
        openToThe="top"
        onMouseUp={noop}
        childFixedSize={300}
      >
        Yoga div
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
