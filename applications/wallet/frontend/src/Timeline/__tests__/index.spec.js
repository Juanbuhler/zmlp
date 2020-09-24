import TestRenderer, { act } from 'react-test-renderer'

import Timeline from '..'

const noop = () => {}

const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

jest.mock('../Controls', () => 'TimelineControls')
jest.mock('../Captions', () => 'TimelineCaptions')
jest.mock('../Playhead', () => 'TimelinePlayhead')
jest.mock('../FilterTracks', () => 'TimelineFilterTracks')
jest.mock('../Ruler', () => 'TimelineRuler')
jest.mock('../Aggregate', () => 'TimelineAggregate')
jest.mock('../Detections', () => 'TimelineDetections')

describe('<Timeline />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Timeline
        assetId={ASSET_ID}
        length={18}
        videoRef={{ current: undefined }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should open the Timeline panel', () => {
    const component = TestRenderer.create(
      <Timeline
        assetId={ASSET_ID}
        length={18}
        videoRef={{
          current: {
            play: noop,
            pause: noop,
            addEventListener: noop,
            removeEventListener: noop,
            currentTime: 0,
            duration: 18,
            paused: true,
          },
        }}
      />,
    )

    // Open timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Close timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
