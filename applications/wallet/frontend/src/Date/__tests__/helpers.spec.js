import { formatFullDate, formatPrettyDate } from '../helpers'

describe('<Date /> helpers', () => {
  describe('formatFullDate()', () => {
    it('should return formatted date string', () => {
      const timestamp = 1573090717162
      const formattedString = '2019-11-07 01:38:37'
      expect(formatFullDate({ timestamp })).toEqual(formattedString)
    })
  })

  describe('formatPrettyDate()', () => {
    it('should return formatted date string', () => {
      const timestamp = '2020-04-10T00:27:25.526192Z'
      const formattedString = '2020-04-10 12:27 UTC'
      expect(formatPrettyDate({ timestamp })).toEqual(formattedString)
    })
  })
})
