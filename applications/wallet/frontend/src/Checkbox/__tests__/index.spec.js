import TestRenderer, { act } from 'react-test-renderer'

import Checkbox, { VARIANTS } from '..'

describe('<Checkbox />', () => {
  it('should render properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <Checkbox
        variant={VARIANTS.PRIMARY}
        option={{
          value: 'checkbox',
          label: 'Checkbox',
          icon: '',
          legend: '',
          initialValue: false,
          isDisabled: false,
        }}
        onClick={mockFn}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ type: 'checkbox' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    expect(mockFn).toHaveBeenCalledWith(true)
  })
})
