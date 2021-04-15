import Router from 'next/router'

import {
  fetcher,
  revalidate,
  getQueryString,
  parseResponse,
} from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { url, secretKey, triggers: t, active },
}) => {
  dispatch({ isLoading: true, testSent: '', errors: {} })

  try {
    const triggers = Object.keys(t).filter((key) => t[key])

    await fetcher(`/api/v1/projects/${projectId}/webhooks/`, {
      method: 'POST',
      body: JSON.stringify({ url, secretKey, triggers, active }),
    })

    await revalidate({
      key: `/api/v1/projects/${projectId}/webhooks/`,
    })

    const queryString = getQueryString({
      action: 'add-webhook-success',
    })

    Router.push(`/[projectId]/webhooks${queryString}`, `/${projectId}/webhooks`)
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

export const onTest = async ({
  dispatch,
  projectId,
  trigger,
  state: { url, secretKey },
}) => {
  dispatch({ testSent: '', errors: {} })

  try {
    await fetcher(`/api/v1/projects/${projectId}/webhooks/test/`, {
      method: 'PUT',
      body: JSON.stringify({ url, secretKey, triggers: [trigger.name] }),
    })

    dispatch({ testSent: trigger.displayName, errors: {} })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ errors })
  }
}
