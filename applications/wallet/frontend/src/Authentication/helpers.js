import Router from 'next/router'

export const authenticateUser = ({ setUser, setErrorMessage }) => async ({
  username,
  password,
  idToken,
}) => {
  setErrorMessage('')

  const response = await fetch('/api/v1/login/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      ...(idToken ? { Authorization: `Bearer ${idToken}` } : {}),
    },
    ...(idToken ? {} : { body: JSON.stringify({ username, password }) }),
  })

  if (response.status === 401) {
    return setErrorMessage('Invalid email or password.')
  }

  if (response.status !== 200) {
    return setErrorMessage('Network error.')
  }

  const user = await response.json()

  return setUser({ user: { ...user, projectId: '' } })
}

export const logout = ({ googleAuth, setUser }) => async () => {
  googleAuth.signOut()

  const { csrftoken } = Object.fromEntries(
    document.cookie.split(/; */).map(c => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )

  await fetch('/api/v1/logout/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      'X-CSRFToken': csrftoken,
    },
  })

  setUser({ user: null })

  Router.push('/')
}
