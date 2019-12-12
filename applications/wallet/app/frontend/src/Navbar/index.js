import PropTypes from 'prop-types'

import ProjectSwitcher from '../ProjectSwitcher'
import { colors, spacing, constants } from '../Styles'
import LogoSvg from '../Icons/logo.svg'

const LOGO_WIDTH = 110

const Navbar = ({ projects, setSelectedProject }) => {
  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'left',
        alignItems: 'center',
        backgroundColor: colors.grey1,
        padding: spacing.small,
        boxShadow: constants.boxShadows.navBar,
      }}>
      <LogoSvg width={LOGO_WIDTH} />
      <ProjectSwitcher
        projects={projects}
        setSelectedProject={setSelectedProject}
      />
    </div>
  )
}

Navbar.propTypes = {
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
  setSelectedProject: PropTypes.func.isRequired,
}

export default Navbar
