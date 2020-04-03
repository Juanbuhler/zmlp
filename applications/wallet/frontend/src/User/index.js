import { createContext, useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import * as Sentry from '@sentry/browser'

import { noop, meFetcher } from './helpers'

export const UserContext = createContext({
  user: {},
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ initialUser, children }) => {
  const [googleAuth, setGoogleAuth] = useState({ signIn: noop, signOut: noop })

  const { data } = useSWR(`/api/v1/me/`, meFetcher)

  const user = initialUser.id ? initialUser : data

  useEffect(() => {
    /* istanbul ignore next */
    Sentry.configureScope((scope) => {
      scope.setUser(user)
    })
  }, [user])

  if (!user) return null

  return (
    <UserContext.Provider
      value={{
        user,
        googleAuth,
        setGoogleAuth,
      }}
    >
      {children}
    </UserContext.Provider>
  )
}

User.propTypes = {
  initialUser: PropTypes.shape({
    id: PropTypes.number,
    email: PropTypes.string,
  }).isRequired,
  children: PropTypes.node.isRequired,
}

export default User
