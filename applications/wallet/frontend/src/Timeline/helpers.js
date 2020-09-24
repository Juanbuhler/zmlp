export const GUIDE_WIDTH = 2

export const formatPaddedSeconds = ({ seconds: s }) => {
  const seconds = Number.isFinite(s) ? s : 0

  const ISOString = new Date(seconds * 1000).toISOString()

  // has double digit hours
  if (seconds > 36000) return ISOString.substr(11, 8)

  // has single digit hours
  if (seconds > 3600) return `0${ISOString.substr(12, 7)}`

  // has double digit minutes
  if (seconds > 600) return `00:${ISOString.substr(14, 5)}`

  // has single digit minutes or less than 1 minute
  return `00:0${ISOString.substr(15, 4)}`
}

export const updatePlayheadPosition = ({ video, playhead }) => {
  if (!video || !playhead) return null

  return playhead.style.setProperty(
    'left',
    `calc(${(video.currentTime / video.duration) * 100}% - ${
      GUIDE_WIDTH / 2
    }px)`,
  )
}

export const filterDetections = ({ detections, settings }) => {
  return detections
    .map(({ name, predictions }) => {
      const filteredPredictions = predictions.filter((prediction) => {
        return prediction.label
          .toLowerCase()
          .includes(settings.filter.toLowerCase())
      })

      return { name, predictions: filteredPredictions }
    })
    .filter(({ predictions }) => predictions.length > 0)
}
