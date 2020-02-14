import { typography, colors, spacing } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

import Accordion, { CHECKMARK_WIDTH } from '../Accordion'

const DataSourcesAddAutomaticAnalysis = () => {
  return (
    <Accordion
      title={
        <>
          <CheckmarkSvg
            width={CHECKMARK_WIDTH}
            css={{ color: colors.key.one, marginRight: spacing.normal }}
          />
          Zorroa Automatic Analysis
        </>
      }>
      <>
        <p
          css={{
            color: colors.structure.zinc,
            margin: 0,
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
          }}>
          The following basic analysis is applied to all data sets.
        </p>
        <ul
          css={{
            display: 'flex',
            color: colors.structure.zinc,
            listStyleType: 'none',
            margin: 0,
            padding: 0,
            paddingTop: spacing.base,
            li: {
              width: '33%',
              display: 'flex',
              alignItems: 'flex-start',
              paddingRight: spacing.large,
            },
            svg: {
              color: colors.key.one,
              marginRight: spacing.normal,
              minWidth: '20px',
              maxWidth: '20px',
            },
            span: {
              fontWeight: typography.weight.bold,
              color: colors.structure.white,
            },
          }}>
          <li>
            <CheckmarkSvg />
            <div>
              <span>File Introspection</span>
              <br />
              Extracts native metadata from file.
            </div>
          </li>
          <li>
            <CheckmarkSvg />
            <div>
              <span>Similarity Hash</span>
              <br />
              Adds a hash to all images used to quantify the similarity between
              images.
            </div>
          </li>
          <li>
            <CheckmarkSvg />
            <div>
              <span>Proxy Creation</span>
              <br />
              Creates proxy files suitable for web streaming for all media.
            </div>
          </li>
        </ul>
      </>
    </Accordion>
  )
}

export default DataSourcesAddAutomaticAnalysis
