import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

export const FILE_TYPES = [
  {
    value: 'images',
    label: 'Image Files',
    legend: 'GIF, PNG, JPG, JPEG, TIF, TIFF, PSD',
    icon: '/icons/images.png',
    identifier: 'jpg',
  },
  {
    value: 'documents',
    label: 'Documents (PDF & MS Office)',
    legend: 'PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX',
    icon: '/icons/documents.png',
    identifier: 'pdf',
  },
  {
    value: 'video',
    label: 'Video Files',
    legend: 'MP4, M4V, MOV, MPG, MPEG, OGG',
    icon: '/icons/videos.png',
    identifier: 'mp4',
  },
]

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, uri, credentials, source, fileTypes, modules },
}) => {
  const parsedCredentials = Object.keys(credentials[source]).reduce(
    (acc, credential) => {
      const { value } = credentials[source][credential]
      if (value) {
        acc[credential] = value
      }
      return acc
    },
    {},
  )

  try {
    await fetcher(`/api/v1/projects/${projectId}/data_sources/`, {
      method: 'POST',
      body: JSON.stringify({
        name,
        uri,
        credentials: Object.keys(parsedCredentials).length
          ? { type: source, ...parsedCredentials }
          : {},
        file_types: Object.keys(fileTypes)
          .filter((f) => fileTypes[f])
          .flatMap((f) => {
            const { legend: extensions } = FILE_TYPES.find(
              ({ value }) => value === f,
            )
            return extensions.toLowerCase().split(',')
          }),
        modules: Object.keys(modules).filter((m) => modules[m]),
      }),
    })

    Router.push(
      '/[projectId]/data-sources?action=add-datasource-success',
      `/${projectId}/data-sources?action=add-datasource-success`,
    )
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ errors: parsedErrors })

      window.scrollTo(0, 0)
    } catch (error) {
      dispatch({
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}
