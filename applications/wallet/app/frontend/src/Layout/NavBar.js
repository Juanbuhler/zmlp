import PropTypes from 'prop-types'

import { colors, spacing, constants, zIndex } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import ProjectSwitcher from '../ProjectSwitcher'
import UserMenu from '../UserMenu'

const LOGO_WIDTH = 110

const LayoutNavBar = ({
  isSidebarOpen,
  projects,
  setSidebarOpen,
  setSelectedProject,
  logout,
}) => {
  return (
    <div
      css={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        height: constants.navbar.height,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        backgroundColor: colors.grey1,
        boxShadow: constants.boxShadows.navBar,
        zIndex: zIndex.layout.navbar,
        paddingLeft: spacing.normal,
        paddingRight: spacing.normal,
      }}>
      <div css={{ display: 'flex', alignItems: 'center' }}>
        <button type="button" onClick={() => setSidebarOpen(!isSidebarOpen)}>
          Hamburger
        </button>
        <LogoSvg width={LOGO_WIDTH} />
        <ProjectSwitcher
          projects={projects}
          setSelectedProject={setSelectedProject}
        />
      </div>

      <UserMenu logout={logout} />
    </div>
  )
}

LayoutNavBar.propTypes = {
  isSidebarOpen: PropTypes.bool.isRequired,
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
  setSelectedProject: PropTypes.func.isRequired,
  logout: PropTypes.func.isRequired,
}

export default LayoutNavBar
