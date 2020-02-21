import { useReducer } from 'react'
import Head from 'next/head'
import Link from 'next/link'
import { useRouter } from 'next/router'

import { colors, constants, spacing } from '../Styles'

import Navbar from '../Navbar'
import PageTitle from '../PageTitle'
import FormAlert from '../FormAlert'
import FlashMessage, { VARIANTS } from '../FlashMessage'
import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import SectionTitle from '../SectionTitle'

import { onRegister, onConfirm } from './helpers'

const INITIAL_STATE = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
  error: '',
}

export const noop = () => () => {}

const reducer = (state, action) => ({ ...state, ...action })

const CreateAccount = () => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)
  const {
    query: { token, uid, action },
  } = useRouter()

  if (token) {
    onConfirm({ token, uid })
  }

  return (
    <div css={{ height: '100%' }}>
      <Head>
        <title>Create an Account</title>
      </Head>

      <Navbar isSidebarOpen={false} setSidebarOpen={noop}>
        <div />
      </Navbar>

      <div
        css={{
          marginTop: constants.navbar.height,
          paddingLeft: spacing.spacious,
          paddingRight: spacing.spacious,
          paddingBottom: spacing.spacious,
          height: `calc(100vh - ${constants.navbar.height}px)`,
          display: 'flex',
          flexDirection: 'column',
        }}>
        <PageTitle>Create an Account</PageTitle>

        <Form>
          <FormAlert
            errorMessage={state.error}
            setErrorMessage={() => dispatch({ error: '' })}
          />

          {action && action === 'account-activation-expired' && (
            <>
              <FlashMessage variant={VARIANTS.ERROR}>
                Confirmation Link Expired
              </FlashMessage>
              <div
                css={{
                  width: 'max-content',
                  color: colors.signal.warning.base,
                }}>
                Confirmation links expire after three days. Please create a new
                account.
              </div>
            </>
          )}

          <SectionTitle>Name</SectionTitle>

          <Input
            autoFocus
            id="firstName"
            variant={INPUT_VARIANTS.SECONDARY}
            label="First Name"
            type="text"
            value={state.firstName}
            onChange={({ target: { value } }) => dispatch({ firstName: value })}
            hasError={false}
            errorMessage=""
          />

          <Input
            id="lastName"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Last Name"
            type="text"
            value={state.lastName}
            onChange={({ target: { value } }) => dispatch({ lastName: value })}
            hasError={false}
            errorMessage=""
          />

          <Input
            id="email"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Email"
            type="text"
            value={state.email}
            onChange={({ target: { value } }) => dispatch({ email: value })}
            hasError={false}
            errorMessage=""
          />

          <SectionTitle>Password</SectionTitle>

          <Input
            id="password"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Password"
            type="password"
            value={state.password}
            onChange={({ target: { value } }) => dispatch({ password: value })}
            hasError={false}
            errorMessage=""
          />

          <Input
            id="confirmPassword"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Confirm Password"
            type="password"
            value={state.confirmPassword}
            onChange={({ target: { value } }) =>
              dispatch({ confirmPassword: value })
            }
            hasError={false}
            errorMessage=""
          />

          <ButtonGroup>
            <Link href="/" passHref>
              <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
            </Link>
            <Button
              type="submit"
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={() => onRegister({ dispatch, state })}
              isDisabled={
                !state.firstName ||
                !state.lastName ||
                !state.password ||
                state.password !== state.confirmPassword
              }>
              Save
            </Button>
          </ButtonGroup>
        </Form>
      </div>
    </div>
  )
}

export default CreateAccount
