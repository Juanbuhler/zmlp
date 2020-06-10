const withSourceMaps = require('@zeit/next-source-maps')()

require('dotenv').config()

const {
  ANALYZE,
  CI_COMMIT_SHA,
  GOOGLE_OAUTH_CLIENT_ID,
  ENVIRONMENT,
  ENABLE_SENTRY,
} = process.env

module.exports = withSourceMaps({
  reactStrictMode: true,
  publicRuntimeConfig: {
    CI_COMMIT_SHA,
    GOOGLE_OAUTH_CLIENT_ID,
    ENVIRONMENT,
    ENABLE_SENTRY,
  },
  webpack: (config, { isServer }) => {
    if (ANALYZE && !isServer) {
      const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer')

      config.plugins.push(
        new BundleAnalyzerPlugin({
          analyzerMode: 'static',
          analyzerPort: 8888,
          openAnalyzer: true,
          defaultSizes: 'gzip',
        }),
      )
    }

    config.module.rules.push({
      test: /\.svg$/,
      use: ['@svgr/webpack'],
    })

    return config
  },
})
