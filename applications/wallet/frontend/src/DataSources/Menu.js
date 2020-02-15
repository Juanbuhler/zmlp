import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonGear from '../Button/Gear'
import Modal from '../Modal'

const DataSourcesMenu = ({ projectId, dataSourceId, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  return (
    <Menu open="left" button={ButtonGear}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <Link
                href="/[projectId]/data-sources/[dataSourceId]/edit"
                as={`/${projectId}/data-sources/${dataSourceId}/edit`}
                passHref>
                <Button variant={VARIANTS.MENU_ITEM} isDisabled={false}>
                  Edit
                </Button>
              </Link>
            </li>
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
                    title="Delete Data Source"
                    message="Deleting this data source cannot be undone."
                    action="Delete Permanently"
                    onCancel={() => {
                      setDeleteModalOpen(false)
                      onClick()
                    }}
                    onConfirm={async () => {
                      setDeleteModalOpen(false)
                      onClick()

                      await fetcher(
                        `/api/v1/projects/${projectId}/datasources/${dataSourceId}/`,
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

DataSourcesMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  dataSourceId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DataSourcesMenu
