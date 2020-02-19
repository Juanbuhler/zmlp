import { useReducer } from 'react'
import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'

import { onConfirm } from './helpers'

export const noop = () => () => {}

const INITIAL_STATE = {
  newPassword: '',
  newPassword2: '',
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ResetPasswordConfirm = ({ uid, token }) => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)
  return (
    <>
      <h3
        css={{
          textAlign: 'center',
          fontSize: typography.size.large,
          lineHeight: typography.height.large,
          fontWeight: typography.weight.regular,
          paddingTop: spacing.spacious,
          paddingBottom: spacing.spacious,
        }}>
        Enter New Password
      </h3>

      <Input
        autoFocus
        id="newPassword"
        variant={INPUT_VARIANTS.PRIMARY}
        label="Password"
        type="password"
        value={state.newPassword}
        onChange={({ target: { value } }) => dispatch({ newPassword: value })}
        hasError={state.errors.newPassword !== undefined}
        errorMessage={state.errors.newPassword}
      />
      <Input
        id="newPassword2"
        variant={INPUT_VARIANTS.PRIMARY}
        label="Confirm Password"
        type="password"
        value={state.newPassword2}
        onChange={({ target: { value } }) => dispatch({ newPassword2: value })}
        hasError={state.errors.newPassword2 !== undefined}
        errorMessage={state.errors.newPassword2}
      />

      <div
        css={{
          paddingTop: spacing.normal,
          display: 'flex',
          justifyContent: 'center',
        }}>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() =>
            onConfirm({
              state,
              dispatch,
              uid,
              token,
            })
          }
          isDisabled={!state.newPassword || !state.newPassword2}>
          Save
        </Button>
      </div>
    </>
  )
}

ResetPasswordConfirm.propTypes = {
  uid: PropTypes.string.isRequired,
  token: PropTypes.string.isRequired,
}

export default ResetPasswordConfirm
