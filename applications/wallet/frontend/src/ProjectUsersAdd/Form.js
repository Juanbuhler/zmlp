import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Loading from '../Loading'

import { onSubmit } from './helpers'

import ProjectUsersAddFormSuccess from './FormSuccess'

const INITIAL_STATE = {
  emails: '',
  permissions: {},
  succeeded: [],
  failed: [],
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ProjectUsersAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: { results: permissions } = {} } = useSWR(
    `/api/v1/projects/${projectId}/permissions/`,
  )

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  if (!Array.isArray(permissions)) return <Loading />

  if (state.succeeded.length > 0) {
    return (
      <ProjectUsersAddFormSuccess
        projectId={projectId}
        succeeded={state.succeeded}
        failed={state.failed}
        onReset={() => dispatch(INITIAL_STATE)}
      />
    )
  }

  return (
    <Form>
      <SectionTitle>Invite User to view projects</SectionTitle>

      <Input
        autoFocus
        id="emails"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Email(s)"
        type="text"
        value={state.emails}
        onChange={({ target: { value } }) => dispatch({ emails: value })}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <CheckboxGroup
        legend="Add Permissions"
        onClick={permission =>
          dispatch({ permissions: { ...state.permissions, ...permission } })
        }
        options={permissions.map(({ name, description }) => ({
          key: name,
          label: name.replace(/([A-Z])/g, match => ` ${match}`),
          legend: description,
          initialValue: false,
        }))}
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ projectId, dispatch, state })}
          isDisabled={!state.emails}>
          Send Invite
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ProjectUsersAddForm
