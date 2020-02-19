import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import EnterNewPassword, { noop } from '..'

jest.mock('../../Authentication/helpers', () => ({
  getUser: () => mockUser,
}))

describe('<EnterNewPassword />', () => {
  it('should render properly', async () => {
    const mockFn = jest.fn()

    require('next/router').__setMockPushFunction(mockFn)
    require('next/router').__setUseRouter({
      pathname: '/reset-password',
      query: { action: 'enter-new-password' },
    })

    const component = TestRenderer.create(<EnterNewPassword />)

    expect(component.toJSON()).toMatchSnapshot()

    // Set new password
    act(() => {
      component.root
        .findByProps({ id: 'newPassword' })
        .props.onChange({ target: { value: 'foo' } })

      // Set unmatching confirm password
      component.root
        .findByProps({ id: 'confirmPassword' })
        .props.onChange({ target: { value: 'bar' } })
      component.root.findByProps({ id: 'confirmPassword' }).props.onBlur()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Fix confirm password to match
    act(() => {
      component.root
        .findByProps({ id: 'confirmPassword' })
        .props.onChange({ target: { value: 'foo' } })
      component.root.findByProps({ id: 'confirmPassword' }).props.onBlur()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Mock Failure
    fetch.mockResponseOnce('Something went wrong', { status: 400 })

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Dismiss Error Message
    await act(async () => {
      component.root
        .findByProps({ 'aria-label': 'Close alert' })
        .props.onClick({ preventDefault: noop })
    })

    // Mock Success
    fetch.mockResponseOnce(JSON.stringify({ detail: 'Password Changed' }))

    // Click Submit
    await act(async () => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(mockFn).toHaveBeenCalledWith(
      '/reset-password/?action=enter-new-password-success',
    )
  })

  it('noop should do nothing', () => {
    expect(noop()()).toBe(undefined)
  })
})
