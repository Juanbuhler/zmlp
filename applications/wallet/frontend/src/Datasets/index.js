/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */
import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, typography, colors } from '../Styles'

import PageTitle from '../PageTitle'
import BetaBadge from '../BetaBadge'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { usePanel, ACTIONS, MIN_WIDTH } from '../Panel/helpers'
import { useLabelTool } from '../AssetLabeling/helpers'

import DatasetsRow from './Row'

const Datasets = () => {
  const {
    query: { projectId, action, datasetId },
  } = useRouter()

  const [, setLeftOpeningPanel] = usePanel({ openToThe: 'left' })

  const [, setDatasetFields] = useLabelTool({ projectId })

  return (
    <>
      <Head>
        <title>Datasets</title>
      </Head>

      <PageTitle>
        <BetaBadge isLeft />
        Datasets
      </PageTitle>

      {action === 'add-dataset-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Dataset created.{' '}
            <Link
              href="/[projectId]/visualizer"
              as={`/${projectId}/visualizer`}
              passHref
            >
              <a
                onClick={() => {
                  setLeftOpeningPanel({
                    type: ACTIONS.OPEN,
                    payload: { minSize: MIN_WIDTH, openPanel: 'assetLabeling' },
                  })

                  setDatasetFields({ datasetId, labels: {} })
                }}
              >
                Start Labeling
              </a>
            </Link>
          </FlashMessage>
        </div>
      )}

      {action === 'delete-dataset-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Dataset deleted.
          </FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/datasets' },
          { title: 'Create New Dataset', href: '/[projectId]/datasets/add' },
        ]}
      />

      <Table
        role={ROLES.ML_Tools}
        legend="Datasets"
        url={`/api/v1/projects/${projectId}/datasets/`}
        refreshKeys={[]}
        refreshButton={false}
        columns={['Name', 'Type', '#Actions#']}
        expandColumn={0}
        renderEmpty={
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
            }}
          >
            <div
              css={{
                fontSize: typography.size.colossal,
                lineHeight: typography.height.colossal,
                fontWeight: typography.weight.bold,
                color: colors.structure.white,
                paddingBottom: spacing.normal,
              }}
            >
              There are currently no datasets.
            </div>

            <div css={{ display: 'flex' }}>
              <Link href={`/${projectId}/datasets/add`} passHref>
                <Button variant={BUTTON_VARIANTS.PRIMARY}>
                  Create a Dataset
                </Button>
              </Link>
            </div>
          </div>
        }
        renderRow={({ result, revalidate }) => (
          <DatasetsRow
            key={result.id}
            projectId={projectId}
            dataset={result}
            revalidate={revalidate}
            setDatasetFields={setDatasetFields}
          />
        )}
      />
    </>
  )
}

export default Datasets
