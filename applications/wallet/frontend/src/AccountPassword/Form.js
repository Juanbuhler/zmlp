import { useReducer } from 'react'

import { spacing } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { onSubmit } from './helpers'

import AccountPasswordFormSuccess from './FormSuccess'

const INITIAL_STATE = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
  showForm: false,
  success: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const AccountPasswordForm = () => {
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  if (state.success) {
    return (
      <AccountPasswordFormSuccess onReset={() => dispatch(INITIAL_STATE)} />
    )
  }

  return (
    <Form>
      <Input
        autoFocus
        id="currentPassword"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Current Password"
        type="password"
        value={state.currentPassword}
        onChange={({ target: { value } }) =>
          dispatch({ currentPassword: value })
        }
        hasError={state.errors.oldPassword !== undefined}
        errorMessage={state.errors.oldPassword}
      />

      <Input
        id="newPassword"
        variant={INPUT_VARIANTS.SECONDARY}
        label="New Password"
        type="password"
        value={state.newPassword}
        onChange={({ target: { value } }) => dispatch({ newPassword: value })}
        hasError={state.errors.newPassword1 !== undefined}
        errorMessage={state.errors.newPassword1}
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
        hasError={state.errors.newPassword2 !== undefined}
        errorMessage={state.errors.newPassword2}
      />

      <div
        css={{
          display: 'flex',
          paddingTop: spacing.moderate,
        }}>
        <Button
          css={{ marginRight: spacing.normal }}
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() => dispatch(INITIAL_STATE)}>
          Cancel
        </Button>

        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, state })}
          isDisabled={
            !state.currentPassword ||
            !state.newPassword ||
            !state.confirmPassword
          }>
          Save
        </Button>
      </div>
    </Form>
  )
}

export default AccountPasswordForm
