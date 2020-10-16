import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { filterTimelines } from './helpers'

import TimelineAccordion, { COLOR_TAB_WIDTH } from './Accordion'
import TimelineTracks from './Tracks'

const TimelineTimelines = ({
  videoRef,
  length,
  timelines,
  settings,
  dispatch,
}) => {
  const filteredTimelines = filterTimelines({ timelines, settings })

  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        overflow: 'overlay',
        marginLeft: -settings.width,
        borderTop: constants.borders.regular.smoke,
      }}
    >
      <div css={{ width: settings.width }}>
        {filteredTimelines
          .filter(({ timeline }) => {
            return settings.timelines[timeline]?.isVisible !== false
          })
          .sort((a, b) => (a.timeline > b.timeline ? 1 : -1))
          .map(({ timeline, tracks }) => {
            return (
              <TimelineAccordion
                key={timeline}
                color={settings.timelines[timeline]?.color}
                timeline={timeline}
                tracks={tracks}
                dispatch={dispatch}
                isOpen={settings.timelines[timeline]?.isOpen || false}
              >
                {tracks.map(({ track, hits }) => {
                  return (
                    <div key={track} css={{ display: 'flex' }}>
                      <div
                        css={{
                          width: COLOR_TAB_WIDTH,
                          backgroundColor: settings.timelines[timeline]?.color,
                        }}
                      />
                      <div
                        css={{
                          display: 'flex',
                          width: '100%',
                          borderTop: constants.borders.regular.smoke,
                          backgroundColor: colors.structure.coal,
                          overflow: 'hidden',
                        }}
                      >
                        <div
                          css={{
                            flex: 1,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            padding: spacing.base,
                            paddingLeft: spacing.base + spacing.spacious,
                            paddingRight: 0,
                          }}
                        >
                          {track}
                        </div>
                        <div
                          css={{ padding: spacing.base }}
                        >{`(${hits.length})`}</div>
                      </div>
                    </div>
                  )
                })}
              </TimelineAccordion>
            )
          })}
      </div>

      <div
        css={{
          flex: 1,
          overflowY: 'hidden',
          overflowX: 'overlay',
          height: 'fit-content',
        }}
      >
        <div css={{ width: `${settings.zoom}%` }}>
          {filteredTimelines
            .filter(({ timeline }) => {
              return settings.timelines[timeline]?.isVisible !== false
            })
            .sort((a, b) => (a.timeline > b.timeline ? 1 : -1))
            .map(({ timeline, tracks }) => {
              return (
                <TimelineTracks
                  key={timeline}
                  videoRef={videoRef}
                  length={length}
                  color={settings.timelines[timeline]?.color}
                  tracks={tracks}
                  isOpen={settings.timelines[timeline]?.isOpen || false}
                />
              )
            })}
        </div>
      </div>
    </div>
  )
}

TimelineTimelines.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({}),
  }).isRequired,
  length: PropTypes.number.isRequired,
  timelines: PropTypes.arrayOf(
    PropTypes.shape({
      timeline: PropTypes.string.isRequired,
      tracks: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
    }),
  ).isRequired,
  settings: PropTypes.shape({
    width: PropTypes.number.isRequired,
    filter: PropTypes.string.isRequired,
    timelines: PropTypes.shape({}).isRequired,
    zoom: PropTypes.number.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineTimelines