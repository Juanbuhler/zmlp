import TestRenderer, { act } from 'react-test-renderer'

import Refresh from '../Refresh'

describe('<Refresh />', () => {
  it('should render properly', async () => {
    jest.useFakeTimers()

    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Refresh onClick={mockFn} legend="Stuff" />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ variant: 'PRIMARY_SMALL' }).props.onClick()
    })

    expect(mockFn).toHaveBeenCalled()
    // SVG should be animated
    expect(component.toJSON()).toMatchSnapshot()

    // Attempt to click again while already clicked
    act(() => {
      component.root.findByProps({ variant: 'PRIMARY_SMALL' }).props.onClick()
    })

    // End setTimeout()
    act(() => jest.runAllTimers())

    // SVG should not be animated
    expect(component.toJSON()).toMatchSnapshot()
  })
})
