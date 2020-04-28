import { constants, colors } from '../Styles'

const DataSourcesAddCopy = () => {
  return (
    <p
      css={{
        marginTop: 0,
        maxWidth: constants.paragraph.maxWidth,
        color: colors.structure.zinc,
      }}
    >
      A data source is the location where your assets are stored. Add your data
      source credentials below to connect and ingest the files into the Zorroa
      system. Zorroa will automatically run a basic analysis on your assets and
      you may add additional modules.{' '}
      <a
        css={{ color: colors.key.one }}
        target="_blank"
        rel="noopener noreferrer"
        href="https://app.gitbook.com/@zorroa/s/zmlp/client/assets/importing-assets/asset-datasource"
      >
        Learn more about data sources.
      </a>
    </p>
  )
}

export default DataSourcesAddCopy
