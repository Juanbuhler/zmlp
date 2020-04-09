import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR, { useSWRPages } from 'swr'

import { spacing } from '../Styles'

import Loading from '../Loading'

import AssetsResize from './Resize'
import AssetsThumbnail from './Thumbnail'

import { reducer, INITIAL_STATE } from './reducer'

const SIZE = 50

const Assets = () => {
  const {
    query: { projectId },
  } = useRouter()

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { thumbnailCount, isMin, isMax } = state

  const containerWidth = 100 / thumbnailCount

  const { pages, isLoadingMore, isReachingEnd, loadMore } = useSWRPages(
    // page key
    'visualizer',

    // page component
    ({ offset, withSWR }) => {
      const from = offset * SIZE
      const { data: { results } = {} } = withSWR(
        // eslint-disable-next-line react-hooks/rules-of-hooks
        useSWR(
          `/api/v1/projects/${projectId}/assets/?from=${from}&size=${SIZE}`,
          { suspense: false },
        ),
      )

      if (!results) {
        return (
          <div css={{ flex: 1, display: 'flex', height: '100%' }}>
            <Loading />
          </div>
        )
      }

      return results.map((asset) => (
        <AssetsThumbnail key={asset.id} asset={asset} />
      ))
    },

    // offset of next page
    ({ data: { count } }, index) => {
      const offset = (index + 1) * SIZE
      return offset < count ? offset : null
    },

    // deps of the page component
    [],
  )

  return (
    <div css={{ flex: 1, position: 'relative' }}>
      <div
        css={{
          height: '100%',
          display: 'flex',
          flexWrap: 'wrap',
          alignContent: 'flex-start',
          overflowY: 'auto',
          padding: spacing.small,
          '.container': {
            width: `${containerWidth}%`,
            paddingBottom: `${containerWidth}%`,
          },
        }}
      >
        {pages}
      </div>

      <AssetsResize dispatch={dispatch} isMin={isMin} isMax={isMax} />
    </div>
  )
}

export default Assets
