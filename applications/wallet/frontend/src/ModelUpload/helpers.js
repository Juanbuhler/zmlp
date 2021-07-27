import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({ projectId, modelId, state, dispatch }) => {
  const request = new XMLHttpRequest()

  dispatch({ isConfirmed: true, request })

  const { signedUrl } = await fetcher(
    `/api/v1/projects/${projectId}/models/${modelId}/upload_url/`,
  )

  request.open('PUT', signedUrl)

  request.setRequestHeader('Content-Type', 'application/octet-stream')

  // upload progress event
  /* istanbul ignore next */
  request.upload.addEventListener('progress', (e) => {
    // upload progress as percentage
    const progress = (e.loaded / e.total) * 100

    dispatch({ progress })
  })

  // request error event
  /* istanbul ignore next */
  request.addEventListener('error', () => {
    dispatch({ hasFailed: true })
  })

  // request finished event
  /* istanbul ignore next */
  request.addEventListener('load', () => {
    // HTTP status message (200, 404 etc)
    console.log(request.status)

    // request.response holds response from the server
    console.log(request.response)
  })

  // send POST request to server
  request.send(state.file)
}