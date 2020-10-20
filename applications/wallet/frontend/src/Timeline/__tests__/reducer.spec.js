import timelines from '../__mocks__/timelines'

import { reducer, ACTIONS, INITIAL_STATE } from '../reducer'

describe('<Timeline /> reducer', () => {
  it('should return the state', () => {
    expect(reducer(INITIAL_STATE, {})).toEqual(INITIAL_STATE)
  })

  it('should update the filter', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.UPDATE_FILTER,
        payload: { value: 'cat' },
      }),
    ).toEqual({ filter: 'cat', width: 200, timelines: {}, zoom: 100 })
  })

  it('should update the timelines width', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.RESIZE_MODULES,
        payload: { value: 300 },
      }),
    ).toEqual({ filter: '', width: 300, timelines: {}, zoom: 100 })
  })

  it('should open an undefined module', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.TOGGLE_OPEN,
        payload: { timeline: 'gcp-video-logo-detection' },
      }),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: { 'gcp-video-logo-detection': { isOpen: true } },
      zoom: 100,
    })
  })

  it('should open a closed module', () => {
    expect(
      reducer(
        {
          filter: '',
          width: 200,
          timelines: { 'gcp-video-logo-detection': { isOpen: false } },
        },
        {
          type: ACTIONS.TOGGLE_OPEN,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: { 'gcp-video-logo-detection': { isOpen: true } },
    })
  })

  it('should close an open module', () => {
    expect(
      reducer(
        {
          filter: '',
          width: 200,
          timelines: { 'gcp-video-logo-detection': { isOpen: true } },
        },
        {
          type: ACTIONS.TOGGLE_OPEN,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: { 'gcp-video-logo-detection': { isOpen: false } },
    })
  })

  it('should hide an undefined module', () => {
    expect(
      reducer(INITIAL_STATE, {
        type: ACTIONS.TOGGLE_VISIBLE,
        payload: { timeline: 'gcp-video-logo-detection' },
      }),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: { 'gcp-video-logo-detection': { isVisible: false } },
      zoom: 100,
    })
  })

  it('should hide a visible module', () => {
    expect(
      reducer(
        {
          filter: '',
          width: 200,
          timelines: { 'gcp-video-logo-detection': { isVisible: true } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: { 'gcp-video-logo-detection': { isVisible: false } },
    })
  })

  it('should show a hidden module', () => {
    expect(
      reducer(
        {
          filter: '',
          width: 200,
          timelines: { 'gcp-video-logo-detection': { isVisible: false } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE,
          payload: { timeline: 'gcp-video-logo-detection' },
        },
      ),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: { 'gcp-video-logo-detection': { isVisible: true } },
    })
  })

  it('should show all timelines when one of them is hidden', () => {
    expect(
      reducer(
        {
          filter: '',
          width: 200,
          timelines: { 'gcp-video-logo-detection': { isVisible: false } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE_ALL,
          payload: { timelines },
        },
      ),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: {
        'gcp-video-label-detection': { isVisible: true },
        'gcp-video-logo-detection': { isVisible: true },
        'gcp-video-object-detection': { isVisible: true },
        'gcp-video-text-detection': { isVisible: true },
      },
    })
  })

  it('should hide all timelines when all of them are visible', () => {
    expect(
      reducer(
        {
          filter: '',
          width: 200,
          timelines: { 'gcp-video-logo-detection': { isVisible: true } },
        },
        {
          type: ACTIONS.TOGGLE_VISIBLE_ALL,
          payload: { timelines },
        },
      ),
    ).toEqual({
      filter: '',
      width: 200,
      timelines: {
        'gcp-video-label-detection': { isVisible: false },
        'gcp-video-logo-detection': { isVisible: false },
        'gcp-video-object-detection': { isVisible: false },
        'gcp-video-text-detection': { isVisible: false },
      },
    })
  })
})
