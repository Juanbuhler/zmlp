import { useRef, useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import { constants, spacing } from '../Styles'

import AssetNavigation from './Navigation'
import AssetVideo from './Video'

const FALLBACK_IMG = '/icons/fallback_3x.png'

const AssetAsset = ({ isQuickView }) => {
  const [hasError, setHasError] = useState(false)

  const {
    query: { projectId, assetId, query = '' },
  } = useRouter()

  const assetRef = useRef()

  /* istanbul ignore next */
  useEffect(() => {
    const asset = assetRef.current

    if (!asset) return () => {}

    const fallback = () => {
      setHasError(true)
    }

    asset.addEventListener('error', fallback)

    return () => asset.removeEventListener('error', fallback)
  })

  const {
    data: {
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const {
    data: { mediaType, uri },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/signed_url/`)

  const isVideo = mediaType.includes('video')

  return (
    <>
      {!isQuickView && (
        <AssetNavigation
          projectId={projectId}
          assetId={assetId}
          query={query}
          filename={filename}
        />
      )}

      <div
        css={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          width: '100%',
          height: '100%',
          overflowY: 'hidden',
          boxShadow: constants.boxShadows.inset,
        }}
      >
        <div
          css={{
            flex: 1,
            display: 'flex',
            width: '100%',
            marginTop: spacing.hairline,
          }}
        >
          {isVideo && !hasError ? (
            <AssetVideo
              assetRef={assetRef}
              uri={uri}
              mediaType={mediaType}
              isQuickView={isQuickView}
            />
          ) : (
            <img
              ref={assetRef}
              css={{ width: '100%', height: '100%', objectFit: 'contain' }}
              src={hasError ? /* istanbul ignore next */ FALLBACK_IMG : uri}
              alt={filename}
            />
          )}
        </div>
      </div>
    </>
  )
}

AssetAsset.propTypes = {
  isQuickView: PropTypes.bool.isRequired,
}

export default AssetAsset
