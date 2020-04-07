import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Head from 'next/head'

import { colors, constants, typography, spacing } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

import { CURRENT_POLICIES_DATE, onSubmit } from './helpers'

import PoliciesForm from './Form'

const WIDTH = 446
const LOGO_WIDTH = 143

const INITIAL_STATE = {
  isChecked: false,
  errors: { global: '' },
}

const reducer = (state, action) => ({ ...state, ...action })

const Policies = ({ userId, setUser }) => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { isChecked } = state

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
      }}
    >
      <Head>
        <title>Policies</title>
      </Head>

      <form
        method="post"
        onSubmit={(event) => event.preventDefault()}
        css={{
          display: 'flex',
          flexDirection: 'column',
          padding: spacing.colossal,
          width: WIDTH,
          backgroundColor: colors.structure.mattGrey,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
        }}
      >
        <LogoSvg width={LOGO_WIDTH} css={{ alignSelf: 'center' }} />

        <h3
          css={{
            textAlign: 'center',
            fontSize: typography.size.large,
            lineHeight: typography.height.large,
            paddingTop: spacing.spacious,
          }}
        >
          Terms &amp; Conditions and Privacy Policy
        </h3>

        {state.errors.global && (
          <div css={{ paddingTop: spacing.spacious }}>
            <FlashMessage variant={FLASH_VARIANTS.ERROR}>
              {state.errors.global}
            </FlashMessage>
          </div>
        )}

        <div
          css={{
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
          }}
        >
          <PoliciesForm dispatch={dispatch} />
        </div>

        <div
          css={{
            paddingTop: spacing.normal,
            display: 'flex',
            justifyContent: 'center',
          }}
        >
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => onSubmit({ dispatch, userId, setUser })}
            isDisabled={!isChecked}
          >
            Continue
          </Button>
        </div>
      </form>
    </div>
  )
}

Policies.propTypes = {
  userId: PropTypes.number.isRequired,
  setUser: PropTypes.func.isRequired,
}

export { Policies as default, CURRENT_POLICIES_DATE }
