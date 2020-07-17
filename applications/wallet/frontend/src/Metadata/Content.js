import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'
import Button, { VARIANTS } from '../Button'
import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import JsonDisplay from '../JsonDisplay'
import MetadataPretty from '../MetadataPretty'

import { formatDisplayName } from './helpers'

const DISPLAY_OPTIONS = ['pretty', 'raw json']

const MetadataContent = ({ projectId, assetId }) => {
  const [displayOption, setDisplayOption] = useLocalStorageState({
    key: 'metadataFormat',
    initialValue: 'pretty',
  })

  const {
    data: {
      metadata,
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return (
    <>
      <div
        css={{
          padding: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
          color: colors.signal.sky.base,
        }}
      >
        {filename}
      </div>

      <div
        css={{
          display: 'flex',
          borderBottom: constants.borders.regular.smoke,
          ':hover': {
            backgroundColor: `${colors.signal.electricBlue.base}${constants.opacity.hex22Pct}`,
            div: {
              color: colors.structure.white,
              svg: { opacity: 1 },
            },
          },
        }}
      >
        <div
          css={{
            fontFamily: typography.family.condensed,
            color: colors.structure.steel,
            padding: spacing.normal,
            paddingRight: spacing.base,
          }}
        >
          ID
        </div>

        <div
          title={assetId}
          css={{
            flex: 1,
            fontFamily: typography.family.mono,
            fontSize: typography.size.small,
            lineHeight: typography.height.small,
            color: colors.structure.pebble,
            padding: spacing.normal,
            paddingLeft: spacing.base,
            wordBreak: 'break-all',
          }}
        >
          {assetId}
        </div>

        <div
          css={{
            width: COPY_SIZE + spacing.normal,
            padding: spacing.normal,
            paddingLeft: spacing.base,
          }}
        >
          <ButtonCopy value={assetId} />
        </div>
      </div>

      <div
        css={{
          padding: spacing.base,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        <div
          css={{
            display: 'flex',
            width: 'fit-content',
            border: constants.borders.regular.steel,
            borderRadius: constants.borderRadius.small,
          }}
        >
          {DISPLAY_OPTIONS.map((value) => (
            <Button
              key={value}
              style={{
                borderRadius: 0,
                paddingTop: spacing.base,
                paddingBottom: spacing.base,
                paddingLeft: spacing.moderate,
                paddingRight: spacing.moderate,
                backgroundColor:
                  displayOption === value
                    ? colors.structure.steel
                    : colors.structure.transparent,
                color:
                  displayOption === value
                    ? colors.structure.white
                    : colors.structure.steel,
                ':hover': {
                  color: colors.structure.white,
                },
                textTransform: 'uppercase',
              }}
              variant={VARIANTS.NEUTRAL}
              onClick={() => setDisplayOption({ value })}
            >
              {value}
            </Button>
          ))}
        </div>
      </div>

      {displayOption === 'pretty' && (
        <div css={{ overflow: 'auto' }}>
          {Object.keys(metadata)
            .sort()
            .map((section) => {
              const title = formatDisplayName({ name: section })

              return (
                <Accordion
                  key={section}
                  variant={ACCORDION_VARIANTS.PANEL}
                  title={title}
                  cacheKey={`Metadata.${section}`}
                  isInitiallyOpen={false}
                  isResizeable={false}
                >
                  <MetadataPretty metadata={metadata} section={section} />
                </Accordion>
              )
            })}
        </div>
      )}

      {displayOption === 'raw json' && (
        <div css={{ height: '100%', overflow: 'auto' }}>
          <JsonDisplay json={metadata} />
        </div>
      )}
    </>
  )
}

MetadataContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default MetadataContent
