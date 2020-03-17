import PropTypes from 'prop-types'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonGear from '../Button/Gear'
import { ACTIONS } from '../Job/helpers'

const JobsMenu = ({ projectId, jobId, status, revalidate }) => {
  if (!ACTIONS[status].length) return null

  return (
    <Menu open="left" button={ButtonGear}>
      {({ onBlur, onClick }) => (
        <div>
          <ul>
            {ACTIONS[status].map(({ name, action }) => (
              <li key={action}>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={async () => {
                    onClick()

                    await fetcher(
                      `/api/v1/projects/${projectId}/jobs/${jobId}/${action}/`,
                      { method: 'PUT' },
                    )

                    revalidate()
                  }}
                  isDisabled={false}>
                  {name}
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Menu>
  )
}

JobsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  status: PropTypes.oneOf(Object.keys(ACTIONS)).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default JobsMenu
