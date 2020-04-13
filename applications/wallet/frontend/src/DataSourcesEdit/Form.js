import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'
import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import SectionSubTitle from '../SectionSubTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import ButtonGroup from '../Button/Group'
import CheckboxGroup from '../Checkbox/Group'

import { FILE_TYPES } from '../DataSourcesAdd/helpers'

import DataSourcesAddAutomaticAnalysis from '../DataSourcesAdd/AutomaticAnalysis'
import DataSourcesAddProvider from '../DataSourcesAdd/Provider'
import DataSourcesAddCopy from '../DataSourcesAdd/Copy'

import { onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const DataSourcesEditForm = ({ initialState }) => {
  const {
    query: { projectId, dataSourceId },
  } = useRouter()

  const {
    data: { results: providers },
  } = useSWR(`/api/v1/projects/${projectId}/providers/`)

  const [state, dispatch] = useReducer(reducer, initialState)

  const { errors, fileTypes, name, uri } = state

  return (
    <>
      {errors.global && (
        <FlashMessage variant={FLASH_VARIANTS.ERROR}>
          {errors.global}
        </FlashMessage>
      )}

      <Form style={{ width: 'auto' }}>
        <DataSourcesAddCopy />

        <div
          css={{
            width: constants.form.maxWidth,
            paddingBottom: spacing.normal,
          }}
        >
          <SectionTitle>Data Source Name </SectionTitle>

          <Input
            autoFocus
            id="name"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Name"
            type="text"
            value={name}
            onChange={({ target: { value } }) => dispatch({ name: value })}
            hasError={errors.name !== undefined}
            errorMessage={errors.name}
          />

          <SectionTitle>{`Storage Address: ${uri}`}</SectionTitle>
        </div>

        <CheckboxGroup
          legend="Select File Types to Import"
          description={<div>A minimum of one file type must be selected </div>}
          onClick={(fileType) =>
            dispatch({ fileTypes: { ...fileTypes, ...fileType } })
          }
          options={FILE_TYPES.map(({ value, label, legend, icon }) => ({
            value,
            label,
            icon: <img src={icon} alt={label} width="40px" />,
            legend,
            initialValue: fileTypes[value] || false,
            isDisabled: false,
          }))}
          variant={CHECKBOX_VARIANTS.SECONDARY}
        />

        <SectionTitle>Select Analysis</SectionTitle>

        <SectionSubTitle>
          Choose the type of analysis you would like performed on your data set:
        </SectionSubTitle>

        <DataSourcesAddAutomaticAnalysis />

        {providers.map((provider) => (
          <DataSourcesAddProvider
            key={provider.name}
            provider={provider}
            onClick={(module) =>
              dispatch({ modules: { ...module, ...module } })
            }
          />
        ))}

        <ButtonGroup>
          <Link
            href="/[projectId]/data-sources"
            as={`/${projectId}/data-sources`}
            passHref
          >
            <Button variant={BUTTON_VARIANTS.SECONDARY}>Cancel</Button>
          </Link>
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() =>
              onSubmit({ dispatch, projectId, dataSourceId, state })
            }
            isDisabled={!name}
          >
            Edit Data Source
          </Button>
        </ButtonGroup>
      </Form>
    </>
  )
}

DataSourcesEditForm.propTypes = {
  initialState: PropTypes.shape({
    name: PropTypes.string,
    uri: PropTypes.string,
    fileTypes: PropTypes.object,
    modules: PropTypes.arrayOf(PropTypes.string),
    errors: PropTypes.shape({ global: PropTypes.string }),
  }).isRequired,
}

export default DataSourcesEditForm
