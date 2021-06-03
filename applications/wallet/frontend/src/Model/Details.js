import { useState } from 'react'
import useSWR from 'swr'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { constants, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import SuspenseBoundary from '../SuspenseBoundary'

import FilterSvg from '../Icons/filter.svg'
import PenSvg from '../Icons/pen.svg'

import { encode } from '../Filters/helpers'
import { ACTIONS, reducer as resizeableReducer } from '../Resizeable/reducer'

import { MIN_WIDTH as PANEL_MIN_WIDTH } from '../Panel'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Tabs from '../Tabs'
import ModelAssets from '../ModelAssets'
import ModelAssetsDropdown from '../ModelAssets/Dropdown'
import ModelLabels from '../ModelLabels'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'
import Toggletip from '../Toggletip'

import ModelDeleteModal from './DeleteModal'
import ModelMatrixLink from './MatrixLink'

import { onTrain } from './helpers'

const LINE_HEIGHT = '23px'

const ModelDetails = () => {
  const {
    pathname,
    query: { projectId, modelId, edit = '' },
  } = useRouter()

  const [error, setError] = useState('')

  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  const [, setLeftOpeningPanel] = useLocalStorage({
    key: 'leftOpeningPanelSettings',
    reducer: resizeableReducer,
    initialState: {
      size: PANEL_MIN_WIDTH,
      originSize: 0,
      isOpen: false,
    },
  })

  const [, setRightOpeningPanel] = useLocalStorage({
    key: 'rightOpeningPanelSettings',
    reducer: resizeableReducer,
    initialState: {
      size: PANEL_MIN_WIDTH,
      originSize: 0,
      isOpen: false,
    },
  })

  const [, setModelFields] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}`,
    reducer: (state, action) => ({ ...state, ...action }),
    initialState: {
      modelId,
      label: '',
      scope: '',
    },
  })

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
    {
      refreshInterval: 3000,
    },
  )

  const {
    name,
    type,
    unappliedChanges,
    moduleName,
    runningJobId,
    modelTypeRestrictions: {
      requiredLabels,
      missingLabels,
      requiredAssetsPerLabel,
      missingLabelsOnAssets,
    },
  } = model

  const encodedFilter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${moduleName}`,
        modelId,
        values: {},
      },
    ],
  })

  return (
    <>
      <div css={{ display: 'flex', justifyContent: 'space-between' }}>
        <div css={{ flexDirection: 'column' }}>
          {runningJobId && (
            <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
              <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
                &quot;{name}&quot; training in progress.{' '}
                <Link
                  href="/[projectId]/jobs/[jobId]"
                  as={`/${projectId}/jobs/${runningJobId}`}
                  passHref
                >
                  <a>Check Status</a>
                </Link>
              </FlashMessage>
            </div>
          )}

          {error && (
            <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
              <FlashMessage variant={FLASH_VARIANTS.ERROR}>
                {error}
              </FlashMessage>
            </div>
          )}

          <div css={{ display: 'flex' }}>
            <ul
              css={{
                margin: 0,
                padding: 0,
                listStyle: 'none',
                fontSize: typography.size.medium,
                lineHeight: LINE_HEIGHT,
              }}
            >
              <li>
                <strong>Model Name:</strong> {name}
              </li>
              <li>
                <strong>Model Type:</strong> {type}
              </li>
              <li>
                <strong>Module Name:</strong> {moduleName}
              </li>
            </ul>
          </div>

          <div css={{ paddingTop: spacing.base, display: 'flex' }}>
            {(!!missingLabels || !!missingLabelsOnAssets) && (
              <FlashMessage variant={FLASH_VARIANTS.INFO}>
                {!!missingLabels && (
                  <>
                    {missingLabels} more{' '}
                    {missingLabels === 1 ? 'label is' : 'labels are'} required
                    (min. = {requiredLabels} unique)
                    <br />
                  </>
                )}

                {!!missingLabelsOnAssets && (
                  <>
                    {missingLabelsOnAssets} more
                    {missingLabelsOnAssets === 1
                      ? ' asset needs '
                      : ' assets need '}
                    to be labeled (min. = {requiredAssetsPerLabel} of each
                    label)
                  </>
                )}
              </FlashMessage>
            )}

            {!missingLabels && !missingLabelsOnAssets && (
              <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
                The model is ready to train.
              </FlashMessage>
            )}
          </div>

          <ButtonGroup>
            <Button
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={() =>
                onTrain({
                  model,
                  deploy: false,
                  projectId,
                  modelId,
                  setError,
                })
              }
              isDisabled={
                !unappliedChanges || !!missingLabels || !!missingLabelsOnAssets
              }
            >
              Train
            </Button>

            <Button
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={() =>
                onTrain({ model, deploy: true, projectId, modelId, setError })
              }
              isDisabled={
                !unappliedChanges || !!missingLabels || !!missingLabelsOnAssets
              }
            >
              Train &amp; Apply
            </Button>

            <Button
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={() => {
                setDeleteModalOpen(true)
              }}
            >
              Delete
            </Button>

            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                div: { marginLeft: 0 },
                button: { marginRight: 0 },
              }}
            >
              <Toggletip openToThe="left" label="Training Options">
                <div
                  css={{
                    fontSize: typography.size.regular,
                    lineHeight: typography.height.regular,
                    padding: spacing.base,
                  }}
                >
                  <h3
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                    }}
                  >
                    Train Model
                  </h3>
                  Train a model without applying it to your assets.
                  <h3
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                      paddingTop: spacing.normal,
                    }}
                  >
                    Train &amp; Test
                  </h3>
                  Train and apply the model to your test set ONLY. This enables
                  you to view the results in the matrix and assess your model’s
                  performance. To save processing time, make adjustments to your
                  model before applying it to all of your assets.
                  <h3
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                      paddingTop: spacing.normal,
                    }}
                  >
                    Train &amp; Apply
                  </h3>
                  Train and apply the model to all the assets in the project.
                  <h3
                    css={{
                      fontSize: typography.size.regular,
                      lineHeight: typography.height.regular,
                      paddingTop: spacing.normal,
                    }}
                  >
                    Delete
                  </h3>
                  Delete a model. This action cannot be undone.
                  <br />
                  <i>
                    Note: Labels that you added to your assets, either manually
                    or through training, remain attached.
                  </i>
                </div>
              </Toggletip>
            </div>

            <ModelDeleteModal
              projectId={projectId}
              modelId={modelId}
              isDeleteModalOpen={isDeleteModalOpen}
              setDeleteModalOpen={setDeleteModalOpen}
            />
          </ButtonGroup>
        </div>

        <ModelMatrixLink projectId={projectId} modelId={modelId} />
      </div>

      <Tabs
        tabs={[
          {
            title: 'View Labels',
            href: '/[projectId]/models/[modelId]',
            isSelected: edit ? false : undefined,
          },
          {
            title: 'Labeled Assets',
            href: '/[projectId]/models/[modelId]/assets',
            isSelected: edit ? false : undefined,
          },
          edit
            ? {
                title: 'Edit Label',
                href: '/[projectId]/models/[modelId]',
                isSelected: true,
              }
            : {},
        ]}
      />

      <SuspenseBoundary>
        {!edit && (
          <div
            css={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              paddingBottom: spacing.base,
            }}
          >
            {pathname === '/[projectId]/models/[modelId]/assets' && (
              <ModelAssetsDropdown projectId={projectId} modelId={modelId} />
            )}

            <div
              css={{
                display: 'flex',
                flex: 1,
                justifyContent: 'flex-end',
              }}
            >
              <Link
                href={`/${projectId}/visualizer?query=${encodedFilter}`}
                passHref
              >
                <Button
                  aria-label="Add Filter in Visualizer"
                  variant={BUTTON_VARIANTS.SECONDARY_SMALL}
                  onClick={() => {
                    setRightOpeningPanel({
                      type: ACTIONS.OPEN,
                      payload: { openPanel: 'filters' },
                    })
                  }}
                  style={{
                    display: 'flex',
                    paddingTop: spacing.moderate,
                    paddingBottom: spacing.moderate,
                  }}
                >
                  <div css={{ display: 'flex', alignItems: 'center' }}>
                    <FilterSvg
                      height={constants.icons.regular}
                      css={{ paddingRight: spacing.base }}
                    />
                    Add Filter in Visualizer
                  </div>
                </Button>
              </Link>

              <div css={{ width: spacing.normal }} />

              <Link
                href="/[projectId]/visualizer"
                as={`/${projectId}/visualizer`}
                passHref
              >
                <Button
                  aria-label="Add More Labels"
                  variant={BUTTON_VARIANTS.SECONDARY_SMALL}
                  onClick={() => {
                    setLeftOpeningPanel({
                      type: ACTIONS.OPEN,
                      payload: { openPanel: 'assetLabeling' },
                    })

                    setModelFields({
                      modelId,
                      scope: SCOPE_OPTIONS[0].value,
                      label: '',
                    })
                  }}
                  style={{
                    display: 'flex',
                    paddingTop: spacing.moderate,
                    paddingBottom: spacing.moderate,
                  }}
                >
                  <div css={{ display: 'flex', alignItems: 'center' }}>
                    <PenSvg
                      height={constants.icons.regular}
                      css={{ paddingRight: spacing.base }}
                    />
                    Add More Labels
                  </div>
                </Button>
              </Link>
            </div>
          </div>
        )}

        {pathname === '/[projectId]/models/[modelId]' && !edit && (
          <ModelLabels requiredAssetsPerLabel={requiredAssetsPerLabel} />
        )}

        {pathname === '/[projectId]/models/[modelId]/assets' && (
          <ModelAssets moduleName={moduleName} />
        )}
      </SuspenseBoundary>
    </>
  )
}

export default ModelDetails
