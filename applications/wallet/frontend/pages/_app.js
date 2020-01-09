/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import * as Sentry from '@sentry/browser'

import Authentication from '../src/Authentication'

if (process.env.NODE_ENV === 'production') {
  Sentry.init({
    dsn: 'https://09e9c3fc777c469ab784ff4367ff54bb@sentry.io/1848515',
  })
}

class MyApp extends App {
  static async getInitialProps({ Component, ctx }) {
    const versions = {
      COMMIT_SHA: process.env.CI_COMMIT_SHA,
    }

    if (Component.getInitialProps) {
      const pageProps = await Component.getInitialProps(ctx)

      return { ...versions, pageProps }
    }

    return { ...versions }
  }

  render() {
    const { Component, pageProps, router, err = '' } = this.props

    if (router.route === '/_error') {
      return <Component {...pageProps} err={err} />
    }

    return (
      <Authentication>
        <Component {...pageProps} />
      </Authentication>
    )
  }
}

export default MyApp
