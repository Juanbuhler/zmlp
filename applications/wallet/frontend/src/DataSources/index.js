import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import FlashMessage, { VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table from '../Table'

import DataSourcesRow from './Row'

const DataSources = () => {
  const {
    query: { projectId, action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Data Sources</title>
      </Head>

      <PageTitle>Data Sources</PageTitle>

      {action === 'add-datasource-success' && (
        <FlashMessage variant={VARIANTS.SUCCESS}>
          Data Source Created
        </FlashMessage>
      )}

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/data-sources' },
          { title: 'Add Data Source', href: '/[projectId]/data-sources/add' },
        ]}
      />

      <div>&nbsp;</div>

      <Table
        url={`/api/v1/projects/${projectId}/datasources/`}
        columns={[
          'Name',
          'Source Type',
          'Path',
          'Date Created',
          'Date Modified',
          'File Types',
          '#Actions#',
        ]}
        expandColumn={3}
        renderEmpty="No data sources"
        renderRow={({ result, revalidate }) => (
          <DataSourcesRow
            key={result.id}
            projectId={projectId}
            dataSource={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default DataSources
