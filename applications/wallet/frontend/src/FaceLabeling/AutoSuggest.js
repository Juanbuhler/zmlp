import PropTypes from 'prop-types'
import { useReducer } from 'react'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import Form from '../Form'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Combobox from '../Combobox'
import CheckmarkSvg from '../Icons/checkmark.svg'

import { onSave } from './helpers'

const BBOX_SIZE = 64

const INITIAL_STATE = ({ labels }) => ({
  labels,
  isLoading: false,
  isSaved: false,
  errors: { labels: {}, global: '' },
})

const reducer = (state, action) => ({ ...state, ...action })

let reloadKey = 0

const FaceLabelingAutoSuggest = ({ projectId, assetId, predictions }) => {
  const initializedState = INITIAL_STATE({
    labels: predictions.reduce((acc, { simhash, label }) => {
      return { ...acc, [simhash]: label }
    }, {}),
  })

  const [state, dispatch] = useReducer(reducer, initializedState)

  const { data } = useSWR(`/api/v1/projects/${projectId}/faces/labels`, {
    revalidateOnFocus: false,
    revalidateOnReconnect: false,
    shouldRetryOnError: false,
  })

  const { possibleLabels = [] } = data || {}

  const changedLabelsCount = Object.entries(state.labels).reduce(
    (acc, [key, value]) => {
      if (value === predictions.find((p) => p.simhash === key).label) {
        return acc
      }
      return acc + 1
    },
    0,
  )

  const isChanged = changedLabelsCount > 0

  const getSaveButtonCopy = () => {
    if (isChanged) {
      return 'Save'
    }
    if (state.isSaved) {
      return (
        <div css={{ display: 'flex' }}>
          <CheckmarkSvg width={20} />
          <div>Saved</div>
        </div>
      )
    }
    return 'Saved'
  }

  return (
    <Form
      style={{
        padding: spacing.normal,
        width: '100%',
        height: '100%',
        overflow: 'auto',
        backgroundColor: colors.structure.coal,
      }}
    >
      <div>
        {predictions.map(({ simhash, bbox, b64Image }) => {
          const originalValue = predictions.find((p) => p.simhash === simhash)
            .label
          const isSaved = !isChanged || state.isSaved
          return (
            <div
              key={simhash}
              css={{
                display: 'flex',
                alignItems: 'center',
                paddingBottom: spacing.normal,
              }}
            >
              <img
                css={{
                  maxHeight: BBOX_SIZE,
                  width: BBOX_SIZE,
                  objectFit: 'contain',
                  marginRight: spacing.normal,
                }}
                alt={bbox}
                title={bbox}
                src={b64Image}
              />
              <Combobox
                key={reloadKey}
                id={simhash}
                inputLabel="Name:"
                options={possibleLabels}
                originalValue={originalValue}
                currentValue={state.labels[simhash]}
                onChange={({ value }) => {
                  return dispatch({
                    labels: {
                      ...state.labels,
                      [simhash]: value,
                    },
                    isSaved,
                  })
                }}
                hasError={state.errors.labels[simhash] !== undefined}
                errorMessage={state.errors.labels[simhash]}
              />
            </div>
          )
        })}
      </div>
      <div css={{ display: 'flex' }}>
        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() => {
            reloadKey += 1
            dispatch({ ...initializedState, isSaved: state.isSaved })
          }}
          style={{ flex: 1 }}
          isDisabled={!isChanged}
        >
          Cancel
        </Button>

        <div css={{ width: spacing.base, minWidth: spacing.base }} />

        <Button
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() =>
            onSave({
              projectId,
              assetId,
              labels: state.labels,
              predictions,
              errors: state.errors,
              dispatch,
            })
          }
          style={{ flex: 1 }}
          isDisabled={!isChanged}
        >
          {getSaveButtonCopy()}
        </Button>
      </div>
    </Form>
  )
}

FaceLabelingAutoSuggest.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(
    PropTypes.shape({ simhash: PropTypes.string.isRequired }),
  ).isRequired,
}

export default FaceLabelingAutoSuggest
