import { useContext, useState, useEffect, Suspense } from 'react'
import PropTypes from 'prop-types'
import getConfig from 'next/config'
import { SWRConfig } from 'swr'
import Router from 'next/router'

import { initializeFetcher } from '../Fetch/helpers'

import { UserContext } from '../User'

import Login from '../Login'
import Projects from '../Projects'
import Layout from '../Layout'
import ErrorBoundary, { VARIANTS } from '../ErrorBoundary'

import { authenticateUser, logout } from './helpers'

import AuthenticationLoading from './Loading'

const AUTHENTICATION_LESS_ROUTES = ['/create-account', '/reset-password']

const {
  publicRuntimeConfig: { GOOGLE_OAUTH_CLIENT_ID },
} = getConfig()

export const noop = () => () => {}

const Authentication = ({ route, children }) => {
  const { user, setUser, googleAuth, setGoogleAuth } = useContext(UserContext)

  const [hasGoogleLoaded, setHasGoogleLoaded] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const fetcher = initializeFetcher({ setUser })

  useEffect(() => {
    window.gapi.load('auth2', async () => {
      setGoogleAuth(
        window.gapi.auth2.init({
          client_id: `${GOOGLE_OAUTH_CLIENT_ID}`,
        }),
      )
      setHasGoogleLoaded(true)
    })
  }, [setGoogleAuth])

  if (AUTHENTICATION_LESS_ROUTES.includes(route)) {
    if (user.id) {
      Router.push('/')
      return null
    }

    return children
  }

  if (!user.id) {
    return (
      <Login
        googleAuth={googleAuth}
        hasGoogleLoaded={hasGoogleLoaded}
        errorMessage={errorMessage}
        onSubmit={authenticateUser({ setUser, setErrorMessage })}
      />
    )
  }

  return (
    <SWRConfig value={{ fetcher, suspense: true }}>
      <ErrorBoundary variant={VARIANTS.GLOBAL}>
        <Suspense fallback={<AuthenticationLoading />}>
          <Projects projectId={user.projectId} setUser={setUser}>
            <Layout user={user} logout={logout({ googleAuth, setUser })}>
              <ErrorBoundary variant={VARIANTS.LOCAL}>{children}</ErrorBoundary>
            </Layout>
          </Projects>
        </Suspense>
      </ErrorBoundary>
    </SWRConfig>
  )
}

Authentication.propTypes = {
  children: PropTypes.node.isRequired,
  route: PropTypes.string.isRequired,
}

export default Authentication
