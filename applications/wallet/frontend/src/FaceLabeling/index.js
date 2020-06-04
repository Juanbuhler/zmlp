import { useRouter } from 'next/router'

import { colors, spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import FaceLabelingContent from './Content'

const FaceLabeling = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  return assetId ? (
    <SuspenseBoundary key={assetId}>
      <FaceLabelingContent projectId={projectId} assetId={assetId} />
    </SuspenseBoundary>
  ) : (
    <div css={{ padding: spacing.normal, color: colors.structure.white }}>
      Select an asset to start adding names to faces and train your model.
    </div>
  )
}

export default FaceLabeling
