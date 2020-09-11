/* eslint-disable jsx-a11y/media-has-caption */
import { useRef } from 'react'
import PropTypes from 'prop-types'

import Feature from '../Feature'
import MetadataCues from '../MetadataCues'
import Timeline from '../Timeline'

// TODO: fetch tracks from backend
const TRACKS = [
  { label: 'English', kind: 'captions', src: '/webvtt/english.vtt' },
  { label: 'French', kind: 'captions', src: '/webvtt/french.vtt' },
  {
    label: 'gcp-label-detection',
    kind: 'metadata',
    src: '/webvtt/gcp-label-detection.vtt',
  },
  {
    label: 'gcp-object-detection',
    kind: 'metadata',
    src: '/webvtt/gcp-object-detection.vtt',
  },
]

const AssetVideo = ({ assetRef, uri, mediaType, isQuickView }) => {
  const videoRef = useRef()

  return (
    <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <div css={{ flex: 1, display: 'flex', flexDirection: 'row' }}>
        <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <div
            css={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              height: 0,
            }}
          >
            <video
              ref={videoRef}
              css={{ flex: 1, width: '100%', height: 0 }}
              autoPlay
              controls
              controlsList="nodownload"
              disablePictureInPicture
            >
              <source ref={assetRef} src={uri} type={mediaType} />

              <Feature flag="timeline" envs={[]}>
                {TRACKS.map(({ label, kind, src }) => {
                  return (
                    <track
                      key={label}
                      kind={kind}
                      label={label}
                      src={src}
                      default={kind === 'metadata'}
                    />
                  )
                })}
              </Feature>
            </video>
          </div>
        </div>

        {!isQuickView && (
          <Feature flag="timeline" envs={[]}>
            <MetadataCues videoRef={videoRef} />
          </Feature>
        )}
      </div>

      {!isQuickView && (
        <Feature flag="timeline" envs={[]}>
          <Timeline videoRef={videoRef} />
        </Feature>
      )}
    </div>
  )
}

AssetVideo.propTypes = {
  assetRef: PropTypes.shape({}).isRequired,
  uri: PropTypes.string.isRequired,
  mediaType: PropTypes.string.isRequired,
  isQuickView: PropTypes.bool.isRequired,
}

export default AssetVideo