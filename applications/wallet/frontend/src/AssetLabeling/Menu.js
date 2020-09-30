import { useState } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'
import { mutate } from 'swr'

import { colors } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import { ACTIONS, dispatch } from '../Filters/helpers'
import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const AssetLabelingMenu = ({
  label,
  modelId,
  moduleName,
  triggerReload,
  setError,
}) => {
  const {
    pathname,
    query: { projectId, id, assetId, query },
  } = useRouter()

  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const [, setLocalModelId] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}.modelId`,
  })
  const [, setLocalLabel] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}.label`,
  })

  return (
    <>
      <Menu
        open="bottom-left"
        button={ButtonActions}
        style={{ color: colors.structure.steel }}
      >
        {({ onBlur, onClick }) => (
          <ul>
            <li>
              <Link
                href="/[projectId]/models/[modelId]"
                as={`/${projectId}/models/${modelId}`}
                passHref
              >
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={onClick}
                >
                  View Model/Train & Apply
                </Button>
              </Link>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={() => {
                  onClick()

                  dispatch({
                    type: ACTIONS.ADD_VALUE,
                    payload: {
                      pathname,
                      projectId,
                      assetId: id || assetId,
                      filter: {
                        type: 'label',
                        attribute: `labels.${moduleName}`,
                        modelId,
                        values: { labels: [label] },
                      },
                      query,
                    },
                  })
                }}
              >
                Add Model/Label Filter
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={() => {
                  setLocalModelId({ value: modelId })
                  setLocalLabel({ value: label })

                  triggerReload()

                  onClick()
                }}
              >
                Edit Label
              </Button>
            </li>
            <li>
              <Button
                variant={VARIANTS.MENU_ITEM}
                onBlur={onBlur}
                onClick={() => {
                  setDeleteModalOpen(true)

                  onClick()
                }}
              >
                Delete Label
              </Button>
            </li>
          </ul>
        )}
      </Menu>
      {isDeleteModalOpen && (
        <Modal
          title="Delete Label"
          message="Deleting this label cannot be undone."
          action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={async () => {
            setIsDeleting(true)

            setError('')

            try {
              await fetcher(
                `/api/v1/projects/${projectId}/models/${modelId}/delete_labels/`,
                {
                  method: 'DELETE',
                  body: JSON.stringify({ removeLabels: [{ assetId, label }] }),
                },
              )

              await mutate(`/api/v1/projects/${projectId}/assets/${assetId}/`)
            } catch (response) {
              setError('Something went wrong. Please try again.')

              setDeleteModalOpen(false)
            }

            // In the case where a user is deleting a Model/Label that matches
            // the current Model/Label value in the `AssetLabelingAdd` form,
            // this re-enables the submit button
            triggerReload()
          }}
        />
      )}
    </>
  )
}

AssetLabelingMenu.propTypes = {
  label: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  moduleName: PropTypes.string.isRequired,
  triggerReload: PropTypes.func.isRequired,
  setError: PropTypes.func.isRequired,
}

export default AssetLabelingMenu
