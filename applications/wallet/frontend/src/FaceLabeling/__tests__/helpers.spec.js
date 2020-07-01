import { onSave, onTrain, getSaveButtonCopy } from '../helpers'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'
const LABELS = { MNONPMMKPLRLONLJMRLNM: 'face0' }
const PREDICTIONS = [
  {
    score: 0.999,
    bbox: [0.38, 0.368, 0.484, 0.584],
    label: 'face1',
    simhash: 'MNONPMMKPLRLONLJMRLNM',
    b64_image: 'data:image/png;base64',
  },
]
const ERRORS = { labels: {}, global: '' }

describe('<FaceLabelingForm /> helpers', () => {
  describe('onSave()', () => {
    it('should update the asset labels', async () => {
      const mockDispatch = jest.fn()
      const mockMutate = jest.fn()

      require('swr').__setMockMutateFn(mockMutate)

      fetch.mockResponseOnce(JSON.stringify({}))

      await onSave({
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
        labels: LABELS,
        predictions: PREDICTIONS,
        errors: ERRORS,
        dispatch: mockDispatch,
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/faces/${ASSET_ID}/save/`,
      )
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body:
          '{"labels":[{"bbox":[0.38,0.368,0.484,0.584],"simhash":"MNONPMMKPLRLONLJMRLNM","label":"face0"}]}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        isLoading: true,
        errors: { ...ERRORS, global: '' },
      })
      expect(mockMutate).toHaveBeenCalledTimes(3)
    })

    it('should set a global error message when the response contains an error message', async () => {
      const mockDispatch = jest.fn()

      fetch.mockRejectOnce({
        json: () =>
          Promise.resolve({
            labels: [{ nonFieldErrors: ['Error message'] }],
          }),
      })

      await onSave({
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
        labels: LABELS,
        predictions: PREDICTIONS,
        errors: ERRORS,
        dispatch: mockDispatch,
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/faces/${ASSET_ID}/save/`,
      )
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body:
          '{"labels":[{"bbox":[0.38,0.368,0.484,0.584],"simhash":"MNONPMMKPLRLONLJMRLNM","label":"face0"}]}',
      })
      expect(mockDispatch).toHaveBeenCalledWith({
        isLoading: true,
        errors: { ...ERRORS, global: '' },
      })
      expect(mockDispatch).toHaveBeenLastCalledWith({
        isLoading: false,
        errors: { labels: {}, global: 'Error message' },
      })
    })

    it('should set a global error message when the request fails', async () => {
      const mockDispatch = jest.fn()

      fetch.mockRejectOnce({
        json: () => Promise.reject(new Error('fail')),
      })

      await onSave({
        projectId: PROJECT_ID,
        assetId: ASSET_ID,
        labels: LABELS,
        predictions: PREDICTIONS,
        errors: ERRORS,
        dispatch: mockDispatch,
      })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/faces/${ASSET_ID}/save/`,
      )
      expect(fetch.mock.calls[0][1]).toEqual({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8',
          'X-CSRFToken': 'CSRF_TOKEN',
        },
        body:
          '{"labels":[{"bbox":[0.38,0.368,0.484,0.584],"simhash":"MNONPMMKPLRLONLJMRLNM","label":"face0"}]}',
      })

      expect(mockDispatch).toHaveBeenCalledWith({
        isLoading: true,
        errors: { ...ERRORS, global: '' },
      })
      expect(mockDispatch).toHaveBeenLastCalledWith({
        isLoading: false,
        errors: {
          labels: {},
          global: 'Something went wrong. Please try again.',
        },
      })
    })
  })

  describe('onTrain()', () => {
    it('should start a training job', async () => {
      const mockSetError = jest.fn()

      await onTrain({ projectId: PROJECT_ID, setError: mockSetError })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/faces/train/`,
      )
      expect(mockSetError.mock.calls[0][0]).toEqual('')
      expect(mockSetError.mock.calls.length).toEqual(1)
    })

    it('should set an error message', async () => {
      const mockSetError = jest.fn()

      fetch.mockRejectOnce()

      await onTrain({ projectId: PROJECT_ID, setError: mockSetError })

      expect(fetch.mock.calls.length).toEqual(1)
      expect(fetch.mock.calls[0][0]).toEqual(
        `/api/v1/projects/${PROJECT_ID}/faces/train/`,
      )
      expect(mockSetError.mock.calls[0][0]).toEqual('')
      expect(mockSetError.mock.calls[1][0]).toEqual(
        'Something went wrong. Please try again.',
      )
    })
  })

  describe('getSaveButtonCopy()', () => {
    it('should return correct string', () => {
      expect(getSaveButtonCopy({ isChanged: false, isLoading: true })).toBe(
        'Saving...',
      )
      expect(getSaveButtonCopy({ isChanged: true, isLoading: false })).toBe(
        'Save',
      )
    })
  })
})
