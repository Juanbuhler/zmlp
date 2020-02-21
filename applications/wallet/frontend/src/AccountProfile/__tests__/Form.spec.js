import TestRenderer, { act } from 'react-test-renderer'

import User from '../../User'

import AccountProfileForm from '../Form'

jest.mock('../helpers')

const noop = () => () => {}

describe('<AccountProfileForm />', () => {
  it('should render properly when a user edits their name', () => {
    const component = TestRenderer.create(
      <User
        initialUser={{
          id: 1,
          username: 'jane.doe',
          email: 'jane.doe@zorroa.com',
        }}>
        <AccountProfileForm />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Click edit
    act(() => {
      component.root
        .findByProps({ children: 'Edit' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ id: 'firstName' })
        .props.onChange({ target: { value: 'Jane' } })
    })

    act(() => {
      component.root
        .findByProps({ id: 'lastName' })
        .props.onChange({ target: { value: 'Doe' } })
    })

    // Submit the form
    act(() => {
      component.root
        .findByProps({ children: 'Save' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly when a user cancels', () => {
    const component = TestRenderer.create(
      <User
        initialUser={{
          id: 1,
          username: 'jane.doe',
          email: 'jane.doe@zorroa.com',
        }}>
        <AccountProfileForm />
      </User>,
    )

    // Click edit
    act(() => {
      component.root
        .findByProps({ children: 'Edit' })
        .props.onClick({ preventDefault: noop })
    })

    // Click cancel
    act(() => {
      component.root
        .findByProps({ children: 'Cancel' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
