import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const ModelLabelsMenu = ({ projectId, modelId, label, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  return (
    <Menu open="left" button={ButtonActions}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onClick={() => {
                  Router.push(
                    `/[projectId]/models/[modelId]?edit=${label}`,
                    `/${projectId}/models/${modelId}`,
                  )
                }}
                isDisabled={false}
              >
                Edit
              </Button>
            </li>
            <li>
              <>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    setDeleteModalOpen(true)
                  }}
                  isDisabled={false}
                >
                  Delete
                </Button>
                {isDeleteModalOpen && (
                  <Modal
                    title="Delete Label"
                    message="Deleting this label cannot be undone."
                    action="Delete Permanently"
                    onCancel={() => {
                      setDeleteModalOpen(false)

                      onClick()
                    }}
                    onConfirm={async () => {
                      setDeleteModalOpen(false)

                      onClick()

                      // TODO: update endpoint
                      await fetcher(
                        `/api/v1/projects/${projectId}/models/${modelId}/delete_labels/`,
                        { method: 'DELETE', body: JSON.stringify({ label }) },
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

ModelLabelsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ModelLabelsMenu
