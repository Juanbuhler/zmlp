import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import AutoSizer from 'react-virtualized-auto-sizer'
import { useRouter } from 'next/router'

import { constants } from '../Styles'

import ResizeableWithMessage from '../Resizeable/WithMessage'

import { getMetadata } from './helpers'

import MetadataCuesContent from './Content'

export const MIN_WIDTH = 400

export const noop = () => {}

const MetadataCues = ({ videoRef }) => {
  const {
    query: { assetId },
  } = useRouter()

  const [metadata, setMetadata] = useState({})

  const video = videoRef.current
  const textTracks = video?.textTracks || {}

  const metadataTracks = Object.values(textTracks).filter(
    ({ kind }) => kind === 'metadata',
  )

  /* istanbul ignore next */
  useEffect(() => {
    if (!metadataTracks) return () => {}

    const onCueChange = (event) => {
      const newMetadata = getMetadata(event)

      setMetadata((m) => ({ ...m, ...newMetadata }))
    }

    metadataTracks.forEach((track) =>
      track.addEventListener('cuechange', onCueChange),
    )

    return () =>
      metadataTracks.forEach((track) =>
        track.removeEventListener('cuechange', onCueChange),
      )
  }, [metadataTracks])

  return (
    <AutoSizer defaultHeight={0} disableWidth>
      {
        /* istanbul ignore next */
        ({ height }) => (
          <div
            css={{
              height,
              borderLeft: constants.borders.regular.black,
              borderBottom: constants.borders.regular.black,
            }}
          >
            <ResizeableWithMessage
              storageName={`MetadataCues.${assetId}`}
              minSize={MIN_WIDTH}
              openToThe="left"
              header={noop}
              isInitiallyOpen
            >
              {() => <MetadataCuesContent metadata={metadata} />}
            </ResizeableWithMessage>
          </div>
        )
      }
    </AutoSizer>
  )
}

MetadataCues.propTypes = {
  videoRef: PropTypes.shape({
    current: PropTypes.shape({
      duration: PropTypes.number,
      textTracks: PropTypes.shape({
        addEventListener: PropTypes.func,
        removeEventListener: PropTypes.func,
      }).isRequired,
    }),
  }).isRequired,
}

export default MetadataCues
