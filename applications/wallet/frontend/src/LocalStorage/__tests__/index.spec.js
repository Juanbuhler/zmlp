import TestRenderer, { act } from 'react-test-renderer'

import useLocalStorage from '..'

describe('useLocalStorage()', () => {
  const mockSet = jest.fn()
  const mockGet = jest.fn()

  Object.defineProperty(window, 'localStorage', {
    value: { setItem: mockSet, getItem: mockGet },
  })

  it('should return properly', () => {
    const TestComponent = () => {
      const [value, setValue] = useLocalStorage('name', 100)
      return (
        <button type="button" onClick={setValue}>
          {value}
        </button>
      )
    }

    const component = TestRenderer.create(<TestComponent />)

    act(() => {
      component.root.findByProps({ type: 'button' }).props.onClick(200)
    })

    expect(mockGet).toHaveBeenCalledWith('name')
    expect(mockSet).toHaveBeenCalledWith('name', '200')
  })

  it('should get existing values', () => {
    mockGet.mockImplementationOnce(() => 200)

    const TestComponent = () => {
      const [value, setValue] = useLocalStorage('name')
      return (
        <button type="button" onClick={setValue}>
          {value}
        </button>
      )
    }

    const component = TestRenderer.create(<TestComponent />)

    expect(
      component.root.findByProps({ type: 'button' }).props.children,
    ).toEqual(200)
  })
})
