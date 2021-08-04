/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import PropTypes from 'prop-types'
import Link from 'next/link'

import { spacing } from '../Styles'

import Tabs from '../Tabs'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import ItemList from '../Item/List'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { formatFullDate } from '../Date/helpers'
import { usePanel, ACTIONS } from '../Panel/helpers'
import { useLabelTool } from '../AssetLabeling/helpers'

import { onTrain } from './helpers'

import ModelMatrixLink from './MatrixLink'
import ModelTip from './Tip'

const ModelTrain = ({ projectId, model, setError }) => {
  const [, setLeftOpeningPanel] = usePanel({ openToThe: 'left' })
  const [, setDataSet] = useLabelTool({ projectId })

  const {
    modelTypeRestrictions: {
      missingLabels,
      missingLabelsOnAssets,
      requiredLabels,
      requiredAssetsPerLabel,
    },
    datasetId,
    timeLastTrained,
    timeLastApplied,
    unappliedChanges,
    state,
    runningJobId,
  } = model

  return (
    <div>
      <div css={{ height: spacing.normal }} />

      {!datasetId && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.INFO}>
            You must add a dataset below before you can train or apply the
            model, or view the matrix.
          </FlashMessage>
        </div>
      )}

      {!runningJobId &&
        datasetId &&
        (!!missingLabels || !!missingLabelsOnAssets) && (
          <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
            <FlashMessage variant={FLASH_VARIANTS.INFO}>
              This type of model requires a larger dataset before it can be
              trained.
              <Link
                href="/[projectId]/visualizer"
                as={`/${projectId}/visualizer`}
                passHref
              >
                <a
                  onClick={() => {
                    setLeftOpeningPanel({
                      type: ACTIONS.OPEN,
                      payload: { openPanel: 'assetLabeling' },
                    })

                    setDataSet({ datasetId, labels: {} })
                  }}
                >
                  Add More Labels
                </a>
              </Link>
              <br />
              <ul css={{ margin: 0 }}>
                {!!missingLabels && (
                  <li>
                    {missingLabels} more label{missingLabels > 1 && 's'} (min. ={' '}
                    {requiredLabels} unique{requiredLabels > 1 && 's'})
                  </li>
                )}
                {!!missingLabelsOnAssets && (
                  <li>
                    {missingLabelsOnAssets} more labeled asset
                    {missingLabelsOnAssets > 1 && 's'} (min. ={' '}
                    {requiredAssetsPerLabel} of each label)
                  </li>
                )}
              </ul>
            </FlashMessage>
          </div>
        )}

      {!runningJobId &&
        datasetId &&
        !missingLabels &&
        !missingLabelsOnAssets &&
        !timeLastTrained &&
        !!unappliedChanges && (
          <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
            <FlashMessage variant={FLASH_VARIANTS.INFO}>
              The model is ready to train.
            </FlashMessage>
          </div>
        )}

      {!runningJobId &&
        datasetId &&
        !missingLabels &&
        !missingLabelsOnAssets &&
        !!timeLastTrained &&
        !!unappliedChanges && (
          <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
            <FlashMessage variant={FLASH_VARIANTS.INFO}>
              Changes have been made to the dataset since the model was last
              trained and applied.
            </FlashMessage>
          </div>
        )}

      <div css={{ display: 'flex', justifyContent: 'space-between' }}>
        <div>
          <ItemList
            attributes={[
              [
                'Last Trained',
                timeLastTrained
                  ? formatFullDate({ timestamp: timeLastTrained })
                  : 'Untrained',
              ],
              [
                'Last Analyzed',
                timeLastApplied
                  ? formatFullDate({ timestamp: timeLastApplied })
                  : 'Model analysis has not been run.',
              ],
            ]}
          />

          <ButtonGroup>
            <Button
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() =>
                onTrain({
                  model,
                  apply: false,
                  test: false,
                  projectId,
                  modelId: model.id,
                  setError,
                })
              }
              isDisabled={!!missingLabels}
            >
              Train Model
            </Button>

            <Button
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() =>
                onTrain({
                  model,
                  apply: false,
                  test: true,
                  projectId,
                  modelId: model.id,
                  setError,
                })
              }
              isDisabled={!!missingLabels}
            >
              Train &amp; Test
            </Button>

            <Button
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() =>
                onTrain({
                  model,
                  apply: true,
                  test: false,
                  projectId,
                  modelId: model.id,
                  setError,
                })
              }
              isDisabled={!!missingLabels}
            >
              Train &amp; Analyze All
            </Button>

            <ModelTip />
          </ButtonGroup>
        </div>

        <ModelMatrixLink
          key={`${state}${runningJobId}`}
          projectId={projectId}
          model={model}
        />
      </div>

      <Tabs
        tabs={[
          { title: 'Dataset', href: '/[projectId]/models/[modelId]' },
          {
            title: 'Deployment',
            href: '/[projectId]/models/[modelId]/deployment',
          },
        ]}
      />
    </div>
  )
}

ModelTrain.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    modelTypeRestrictions: PropTypes.shape({
      missingLabels: PropTypes.number,
      missingLabelsOnAssets: PropTypes.number,
      requiredLabels: PropTypes.number,
      requiredAssetsPerLabel: PropTypes.number,
    }).isRequired,
    unappliedChanges: PropTypes.bool.isRequired,
    state: PropTypes.string.isRequired,
    timeLastTrained: PropTypes.number,
    timeLastApplied: PropTypes.number,
    runningJobId: PropTypes.string,
  }).isRequired,
  setError: PropTypes.func.isRequired,
}

export default ModelTrain