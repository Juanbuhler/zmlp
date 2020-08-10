import { fetcher, revalidate, getQueryString, getPathname } from '../helpers'

describe('<Fetch /> helpers', () => {
  describe('fetcher()', () => {
    it('should fetch data', async () => {
      fetch.mockResponseOnce(JSON.stringify({ id: 12345 }))

      const data = await fetcher('/url')

      expect(data).toEqual({ id: 12345 })
    })

    it('should return the raw response in case of error', async () => {
      fetch.mockResponseOnce(null, { status: 500 })

      try {
        await fetcher('/url')
      } catch (response) {
        expect(response.status).toBe(500)

        expect(response.statusText).toBe('Internal Server Error')

        expect(response).toMatchSnapshot()
      }
    })

    it('should return the raw response if its not a json', async () => {
      fetch.mockResponseOnce(null, { status: 200 })

      try {
        await fetcher('/url')
      } catch (response) {
        expect(response.status).toBe(200)

        expect(response.statusText).toBe('Ok')

        expect(response).toMatchSnapshot()
      }
    })

    it('should logout the user', async () => {
      const mockMutate = jest.fn()

      require('swr').__setMockMutateFn(mockMutate)

      fetch.mockResponseOnce(null, { status: 401 })

      try {
        await fetcher('/url')
      } catch (response) {
        expect(response.status).toBe(401)

        expect(response.statusText).toBe('Unauthorized')

        expect(mockMutate).toHaveBeenCalledWith({})
      }
    })
  })

  describe('revalidate()', () => {
    it('should fetch data', async () => {
      await revalidate({ key: '/url' })

      expect(fetch.mock.calls[0][0]).toEqual('/url')
    })
  })

  describe('getQueryString()', () => {
    it('should return an empty string with no query params', () => {
      expect(getQueryString()).toEqual('')
    })
  })

  describe('getPathname()', () => {
    it('should return a pathname', () => {
      expect(getPathname({ pathname: '/' })).toEqual('/')
    })

    it('should strip query params', () => {
      expect(getPathname({ pathname: '/?foo=bar' })).toEqual('/')
    })

    it('should strip the projectId', () => {
      expect(
        getPathname({ pathname: '/a0952c03-cc04-461c-a367-9ffae8c4199a' }),
      ).toEqual('/<projectId>')
    })

    it('should camelCase', () => {
      expect(
        getPathname({
          pathname:
            '/a0952c03-cc04-461c-a367-9ffae8c4199a/data-sources/81ca313d-dc65-1391-9fe9-1aeeeaea6f33/edit',
        }),
      ).toEqual('/<projectId>/data-sources/<dataSourceId>/edit')
    })

    it('should strip recursively', () => {
      expect(
        getPathname({
          pathname:
            '/a0952c03-cc04-461c-a367-9ffae8c4199a/jobs/bc8e8b24-7aa2-1f49-a1c6-420403fdacd8/tasks/bc8e92b5-7aa2-1f49-a1c6-420403fdacd8/errors/91325a65-cf73-17a9-bd8f-76ee1900ec47',
        }),
      ).toEqual('/<projectId>/jobs/<jobId>/tasks/<taskId>/errors/<errorId>')
    })

    it('should strip the assetId', () => {
      expect(
        getPathname({
          pathname:
            '/a0952c03-cc04-461c-a367-9ffae8c4199a/visualizer/2SdbPKHfNc0CPKBRAyA_V3oM5Oenpt3B',
        }),
      ).toEqual('/<projectId>/visualizer/<assetId>')
    })
  })
})
