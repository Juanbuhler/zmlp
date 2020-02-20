import { createContext, useState, useEffect } from 'react'
import PropTypes from 'prop-types'

import { getUser } from './helpers'

export const noop = () => {}

export const UserContext = createContext({
  user: {},
  setUser: noop,
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ initialUser, children }) => {
  const [user, setUser] = useState(initialUser)
  const [hasLocalStorageLoaded, setHasLocalStorageLoaded] = useState(false)
  const [googleAuth, setGoogleAuth] = useState({ signIn: noop, signOut: noop })

  useEffect(() => {
    if (initialUser.email || hasLocalStorageLoaded) return

    const storedUser = getUser()

    setUser(storedUser)

    setHasLocalStorageLoaded(true)
  }, [initialUser, hasLocalStorageLoaded, user, setUser])

  if (!initialUser.email && !hasLocalStorageLoaded) return null

  return (
    <UserContext.Provider value={{ user, setUser, googleAuth, setGoogleAuth }}>
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
