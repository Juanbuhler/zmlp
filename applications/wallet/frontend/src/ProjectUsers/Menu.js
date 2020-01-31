import { useState } from 'react'
import PropTypes from 'prop-types'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonGear from '../Button/Gear'
import Modal from '../Modal'

const ProjectUsersMenu = ({ projectId, userId, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  return (
    <Menu open="left" button={ButtonGear}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    setDeleteModalOpen(true)
                  }}
                  isDisabled={false}>
                  Delete
                </Button>
                {isDeleteModalOpen && (
                  <Modal
                    onCancel={() => {
                      setDeleteModalOpen(false)
                      onClick()
                    }}
                    onConfirm={async () => {
                      setDeleteModalOpen(false)
                      onClick()

                      await fetcher(
                        `/api/v1/projects/${projectId}/users/${userId}/`,
                        { method: 'DELETE' },
                      )

                      revalidate()
                    }}
                  />
                )}
              </>
            </li>
          </ul>
        </div>
      )}
    </Menu>
  )
}

ProjectUsersMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  userId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ProjectUsersMenu
