import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import { v4 as uuidv4 } from 'uuid'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import HiddenSvg from '../Icons/hidden.svg'
import VisibleSvg from '../Icons/visible.svg'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import FlashMessageErrors from '../FlashMessage/Errors'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

import { onTest } from '../WebhooksAdd/helpers'

import { onSubmit } from './helpers'

const INITIAL_STATE = {
  url: '',
  secretKey: uuidv4(),
  showKey: false,
  triggers: {},
  active: false,
  isLoading: false,
  errors: {},
  testSent: '',
}

const reducer = (state, action) => ({ ...state, ...action })

const WebhooksEditForm = () => {
  const {
    query: { projectId, webhookId },
  } = useRouter()

  const {
    data: { results: triggers },
  } = useSWR(`/api/v1/webhooks/triggers/`)

  const { data: webhook } = useSWR(
    `/api/v1/projects/${projectId}/webhooks/${webhookId}/`,
  )

  const [state, dispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    ...webhook,
    triggers: webhook.triggers.reduce((acc, t) => ({ ...acc, [t]: true }), {}),
  })

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.normal }}
      />

      {!!state.testSent && (
        <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
          &quot;{state.testSent}&quot; test has been sent.
        </FlashMessage>
      )}

      <SectionTitle>Webhook URL</SectionTitle>

      <SectionSubTitle>
        The HTTP endpoint to your webhook service.
      </SectionSubTitle>

      <Input
        autoFocus
        isRequired
        id="url"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Webhook URL"
        type="text"
        value={state.url}
        onChange={({ target: { value } }) => dispatch({ url: value })}
        hasError={state.errors.url !== undefined}
        errorMessage={state.errors.url}
      />

      <SectionTitle>Secret Token</SectionTitle>

      <SectionSubTitle>
        You can enter your own secret token, or the system will generate one for
        you. You can change the token at any time.
      </SectionSubTitle>

      <div
        css={{
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <Input
          isRequired
          id="secretKey"
          variant={INPUT_VARIANTS.SECONDARY}
          style={{ flex: 1 }}
          label="Secret Token"
          type={state.showKey ? 'text' : 'password'}
          value={state.secretKey}
          onChange={({ target: { value } }) => dispatch({ secretKey: value })}
          hasError={state.errors.secretKey !== undefined}
          errorMessage={state.errors.secretKey}
          after={
            <Button
              aria-label={state.showKey ? 'Hide Token' : 'Show Token'}
              variant={BUTTON_VARIANTS.NEUTRAL}
              onClick={() => dispatch({ showKey: !state.showKey })}
              style={{
                color: colors.structure.zinc,
                padding: spacing.moderate,
                outlineOffset: -2,
                '&:hover': { color: colors.key.one },
              }}
            >
              {state.showKey ? (
                <VisibleSvg height={constants.icons.regular} />
              ) : (
                <HiddenSvg height={constants.icons.regular} />
              )}
            </Button>
          }
        />

        <div
          css={{
            position: 'absolute',
            left: '100%',
            paddingLeft: spacing.base,
            paddingTop: spacing.base,
          }}
        >
          <Button
            variant={BUTTON_VARIANTS.LINK}
            onClick={() => {
              dispatch({ secretKey: uuidv4() })
            }}
          >
            Generate Token
          </Button>
        </div>
      </div>

      <SectionTitle>Trigger Event</SectionTitle>

      <SectionSubTitle>
        You can specify one or more events that trigger the webhook. Note that
        you do not have to activate a trigger event to test it. You can change
        these settings at any time.
      </SectionSubTitle>

      <div
        css={{
          marginRight: -spacing.enormous,
          paddingTop: spacing.normal,
          paddingBottom: spacing.spacious,
        }}
      >
        {triggers.map((trigger) => {
          return (
            <div
              key={trigger.name}
              css={{
                display: 'flex',
                borderBottom: constants.borders.regular.iron,
                paddingTop: spacing.normal,
              }}
            >
              <div css={{ flex: 1 }}>
                <Checkbox
                  variant={CHECKBOX_VARIANTS.PRIMARY}
                  option={{
                    value: trigger.name,
                    label: trigger.displayName,
                    legend: trigger.description,
                    initialValue: !!state.triggers[trigger.name],
                    isDisabled: false,
                  }}
                  onClick={(value) =>
                    dispatch({
                      triggers: { ...state.triggers, [trigger.name]: value },
                    })
                  }
                />
              </div>
              <div css={{ paddingLeft: spacing.normal }}>
                <Button
                  variant={BUTTON_VARIANTS.SECONDARY_SMALL}
                  isDisabled={!state.url}
                  onClick={() => {
                    onTest({ dispatch, trigger, state })
                  }}
                >
                  Send Test
                </Button>
              </div>
            </div>
          )
        })}
      </div>

      <SectionTitle>Activate Webhook</SectionTitle>

      <SectionSubTitle>
        Select this checkbox to activate the trigger events that you selected in
        the preceding section. You can change this setting at any time.
      </SectionSubTitle>

      <div
        css={{
          paddingTop: spacing.normal,
          span: { fontWeight: typography.weight.regular },
        }}
      >
        <Checkbox
          variant={CHECKBOX_VARIANTS.PRIMARY}
          option={{
            value: 'active',
            label: 'Activate Webhook',
            initialValue: state.active,
            isDisabled: false,
          }}
          onClick={(value) => dispatch({ active: value })}
        />
      </div>

      <ButtonGroup>
        <Link
          href="/[projectId]/webhooks"
          as={`/${projectId}/webhooks`}
          passHref
        >
          <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
        </Link>

        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, projectId, webhookId, state })}
          isDisabled={!state.url || !state.secretKey || state.isLoading}
        >
          {state.isLoading ? 'Saving...' : 'Save Webhook'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default WebhooksEditForm
