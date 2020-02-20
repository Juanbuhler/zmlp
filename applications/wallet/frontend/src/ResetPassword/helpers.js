import Router from 'next/router'

import { getCsrfToken } from '../Fetch/helpers'

export const onRequest = async ({ dispatch, state: { email } }) => {
  const csrftoken = getCsrfToken()

  try {
    const response = await fetch(`/api/v1/password/reset/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': csrftoken,
      },
      body: JSON.stringify({
        email,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=password-reset-request-success')
  } catch (response) {
    dispatch({ error: 'Something went wrong. Please try again.' })
  }
}

export const onConfirm = async ({
  state: { newPassword, newPassword2 },
  dispatch,
  uid,
  token,
}) => {
  const csrftoken = getCsrfToken()

  try {
    const response = await fetch(`/api/v1/password/reset/confirm/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': csrftoken,
      },
      body: JSON.stringify({
        new_password1: newPassword,
        new_password2: newPassword2,
        uid,
        token,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=password-reset-update-success')
  } catch (response) {
    const errors = await response.json()

    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({
      errors: parsedErrors,
    })
  }
}
