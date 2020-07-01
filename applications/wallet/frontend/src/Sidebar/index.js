import { forwardRef, useEffect } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { colors, spacing, zIndex, constants } from '../Styles'

import DashboardSvg from '../Icons/dashboard.svg'
import DataSourcesSvg from '../Icons/datasources.svg'
import JobQueueSvg from '../Icons/jobQueue.svg'
import VisualizerSvg from '../Icons/visualizer.svg'
import KeySvg from '../Icons/key.svg'
import UsersSvg from '../Icons/users.svg'
import GearSvg from '../Icons/gear.svg'

import SidebarLink from './Link'
import SidebarOverlay from './Overlay'

const WIDTH = 240
const ICON_SIZE = 20

const Sidebar = forwardRef(
  ({ projectId, isSidebarOpen, setSidebarOpen }, ref) => {
    useEffect(() => {
      const handleRouteChange = () => {
        setSidebarOpen(false)
      }

      Router.events.on('routeChangeStart', handleRouteChange)

      return () => {
        Router.events.off('routeChangeStart', handleRouteChange)
      }
    }, [setSidebarOpen])

    return (
      <div>
        <nav
          ref={ref}
          css={{
            width: WIDTH,
            position: 'fixed',
            height: `calc(100% - ${constants.navbar.height}px)`,
            overflowY: 'auto',
            zIndex: zIndex.layout.drawer,
            backgroundColor: colors.structure.iron,
            transition: 'left ease-in-out .3s, visibility ease-in-out .3s',
            overscrollBehavior: 'contain',
            left: isSidebarOpen ? 0 : -WIDTH,
            top: constants.navbar.height,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <ul
            css={{
              listStyleType: 'none',
              padding: 0,
              margin: 0,
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
            }}
          >
            <SidebarLink projectId={projectId} href="/[projectId]">
              <DashboardSvg height={ICON_SIZE} />
              Project Dashboard
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/data-sources">
              <DataSourcesSvg height={ICON_SIZE} />
              Data Sources
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/jobs">
              <JobQueueSvg height={ICON_SIZE} />
              Job Queue
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/visualizer">
              <VisualizerSvg height={ICON_SIZE} />
              Visualizer
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/api-keys">
              <KeySvg height={ICON_SIZE} />
              API Keys
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/[projectId]/users">
              <UsersSvg height={ICON_SIZE} />
              User Admin
            </SidebarLink>
          </ul>
          <ul
            css={{
              listStyleType: 'none',
              padding: 0,
              margin: 0,
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
              backgroundColor: colors.structure.smoke,
            }}
          >
            <SidebarLink projectId={projectId} href="/">
              <DashboardSvg height={ICON_SIZE} />
              Account Dashboard
            </SidebarLink>

            <SidebarLink projectId={projectId} href="/account">
              <GearSvg height={ICON_SIZE} />
              Account
            </SidebarLink>
          </ul>
        </nav>
        <SidebarOverlay
          isSidebarOpen={isSidebarOpen}
          setSidebarOpen={setSidebarOpen}
        />
      </div>
    )
  },
)

Sidebar.propTypes = {
  projectId: PropTypes.string.isRequired,
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
}

export default Sidebar
