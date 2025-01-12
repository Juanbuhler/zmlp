import {
  getIsDisabled,
  getLabelState,
  onSave,
  onDelete,
  onFaceDetect,
} from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const DATASET_ID = '4b0b10a8-cec1-155c-b12f-ee2bc8787e06'

describe('<AssetLabeling /> helpers', () => {
  describe('getIsDisabled()', () => {
    it('should be false for a unique blank input with any other label', () => {
      expect(
        getIsDisabled({
          assetId: ASSET_ID,
          state: { labels: { ASSET_ID: { label: 'cat', scope: 'TRAIN' } } },
          labels: [{ label: '' }],
        }),
      ).toBe(false)
    })
  })

  describe('getLabelState()', () => {
    it('should return the values of the only other label', () => {
      expect(
        getLabelState({
          id: ASSET_ID,
          state: { labels: {}, lastLabel: 'cat', lastScope: 'TEST' },
          labels: [{ label: '' }],
        }),
      ).toEqual({ label: 'cat', scope: 'TEST' })
    })
  })

  describe('onSave()', () => {
    it('should abort when there are no changes', async () => {
      const mockDispatch = jest.fn()

      await onSave({
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
        state: { labels: {} },
        labels: [],
        dispatch: mockDispatch,
      })

      expect(mockDispatch).not.toHaveBeenCalled()

      expect(fetch.mock.calls.length).toEqual(0)
    })

    it('should use the previous last label if there is one', async () => {
      const mockDispatch = jest.fn()

      await onSave({
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
        state: {
          datasetId: DATASET_ID,
          labels: {},
          lastLabel: 'cat',
          lastScope: 'TEST',
        },
        labels: [{ label: '' }],
        dispatch: mockDispatch,
      })

      expect(mockDispatch).toHaveBeenCalled()

      expect(fetch.mock.calls.length).toEqual(4)

      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/add_labels/`,
      )

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: `{"addLabels":[{"assetId":"${ASSET_ID}","label":"cat","scope":"TEST"}]}`,
      })
    })

    it('should handle errors', async () => {
      const mockDispatch = jest.fn()

      // Mock Failure
      fetch.mockRejectOnce({ error: 'Invalid' }, { status: 400 })

      await onSave({
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
        state: {
          datasetId: DATASET_ID,
          datasetType: 'Classification',
          labels: { ASSET_ID: { label: 'cat', scope: 'TRAIN' } },
        },
        labels: [],
        dispatch: mockDispatch,
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/add_labels/`,
      )

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: `{"addLabels":[{"assetId":"${ASSET_ID}","label":"cat","scope":"TRAIN"}]}`,
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    })
  })

  describe('onDelete()', () => {
    it('should handle errors', async () => {
      const mockDispatch = jest.fn()

      // Mock Failure
      fetch.mockRejectOnce({ error: 'Invalid' }, { status: 400 })

      await onDelete({
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        assetId: ASSET_ID,
        dispatch: mockDispatch,
        labels: { ASSET_ID: { label: 'cat', scope: 'TRAIN' } },
        label: {
          label: 'cat',
          bbox: [0.53, 0.113, 0.639, 0.29],
          scope: 'TEST',
        },
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/datasets/${DATASET_ID}/delete_labels/`,
      )

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body: `{"removeLabels":[{"assetId":"${ASSET_ID}","label":"cat","scope":"TEST","bbox":[0.53,0.113,0.639,0.29]}]}`,
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    })
  })

  describe('onFaceDetect()', () => {
    it('should handle errors', async () => {
      const mockDispatch = jest.fn()

      // Mock Failure
      fetch.mockRejectOnce({ error: 'Invalid' }, { status: 400 })

      await onFaceDetect({
        projectId: PROJECT_ID,
        datasetId: DATASET_ID,
        assetId: ASSET_ID,
        dispatch: mockDispatch,
      })

      expect(fetch.mock.calls.length).toEqual(1)

      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/assets/${ASSET_ID}/detect_faces/`,
      )

      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    })
  })
})
