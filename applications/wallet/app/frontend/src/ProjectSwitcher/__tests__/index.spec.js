import TestRenderer, { act } from 'react-test-renderer'

import ProjectSwitcher from '..'

import { projects } from '../../Projects/__mocks__'

const noop = () => () => {}

describe('<ProjectSwitcher />', () => {
  it('should render properly with data', () => {
    const mockFn = jest.fn()
    const mockProjects = projects.map(({ id, name }) => {
      return { id, name, selected: id === '1' }
    })

    const component = TestRenderer.create(
      <ProjectSwitcher projects={mockProjects} setSelectedProject={mockFn} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('button')
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ children: 'Zorroa EasyAs123' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
    expect(mockFn).toHaveBeenCalledWith({ id: '2', name: 'Zorroa EasyAs123' })
  })
})
