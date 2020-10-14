import TestRenderer, { act } from 'react-test-renderer'

import TimelineControls from '../Controls'

const noop = () => {}

describe('<TimelineControls />', () => {
  it('should play', () => {
    const mockPlay = jest.fn()

    const component = TestRenderer.create(
      <TimelineControls
        videoRef={{
          current: {
            play: mockPlay,
            pause: noop,
            addEventListener: noop,
            removeEventListener: noop,
            currentTime: 0,
            paused: true,
          },
        }}
        length={18}
        timelines={[]}
        settings={{ filter: '', width: 100, timelines: {}, zoom: 100 }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Play' }).props.onClick()
    })

    expect(mockPlay).toHaveBeenCalled()
  })

  it('should pause', () => {
    const mockPause = jest.fn()

    const component = TestRenderer.create(
      <TimelineControls
        videoRef={{
          current: {
            play: noop,
            pause: mockPause,
            addEventListener: noop,
            removeEventListener: noop,
            currentTime: 6,
            paused: false,
          },
        }}
        length={18}
        timelines={[]}
        settings={{ filter: '', width: 100, timelines: {}, zoom: 100 }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ 'aria-label': 'Pause' }).props.onClick()
    })

    expect(mockPause).toHaveBeenCalled()
  })

  it('should seek the previous second', () => {
    const mockPause = jest.fn()

    const current = {
      play: noop,
      pause: mockPause,
      addEventListener: noop,
      removeEventListener: noop,
      currentTime: 6.5,
      paused: false,
    }

    const component = TestRenderer.create(
      <TimelineControls
        videoRef={{ current }}
        length={18}
        timelines={[]}
        settings={{ filter: '', width: 100, timelines: {}, zoom: 100 }}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Previous Second' })
        .props.onClick()
    })

    expect(mockPause).toHaveBeenCalled()

    expect(current.currentTime).toBe(5)
  })

  it('should seek the next second', () => {
    const mockPause = jest.fn()

    const current = {
      play: noop,
      pause: mockPause,
      addEventListener: noop,
      removeEventListener: noop,
      currentTime: 6.5,
      paused: false,
    }

    const component = TestRenderer.create(
      <TimelineControls
        videoRef={{ current }}
        length={18}
        timelines={[]}
        settings={{ filter: '', width: 100, timelines: {}, zoom: 100 }}
      />,
    )

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Next Second' })
        .props.onClick()
    })

    expect(mockPause).toHaveBeenCalled()

    expect(current.currentTime).toBe(7)
  })
})
