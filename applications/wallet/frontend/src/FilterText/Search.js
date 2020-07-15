import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import HiddenSvg from '../Icons/hidden.svg'
import CrossSvg from '../Icons/cross.svg'
import TextSvg from '../Icons/text.svg'

import { spacing, constants, colors, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from '../Filters/helpers'

const ICON_SIZE = 20

const FilterTextSearch = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    isDisabled,
    values: { query },
  },
  filterIndex,
}) => {
  return (
    <div
      css={{
        border: constants.borders.regular.transparent,
        borderBottom: constants.borders.regular.iron,
        ':hover': {
          border: constants.borders.regular.steel,
          svg: { opacity: 1 },
        },
        padding: spacing.small,
        paddingLeft: spacing.base,
      }}
    >
      <div
        css={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <div
          css={{
            display: 'flex',
            paddingLeft: spacing.comfy,
          }}
        >
          <TextSvg css={{ width: ICON_SIZE, color: colors.key.one }} />
        </div>

        <div
          css={{
            flex: 1,
            minWidth: 0,
            width: '100%',
            display: 'flex',
            alignItems: 'center',
          }}
        >
          <span
            css={{
              fontFamily: typography.family.mono,
              fontSize: typography.size.small,
              lineHeight: typography.height.small,
              paddingLeft: spacing.base,
              overflow: 'hidden',
              whiteSpace: 'nowrap',
              textOverflow: 'ellipsis',
            }}
          >
            {query}
          </span>
        </div>

        <Button
          aria-label={`${isDisabled ? 'Enable' : 'Disable'} Filter`}
          variant={VARIANTS.ICON}
          css={{
            display: 'flex',
            padding: spacing.small,
            justifyContent: 'center',
            alignItems: 'center',
            borderRadius: constants.borderRadius.small,
            ':hover, &.focus-visible:focus': {
              backgroundColor: colors.structure.smoke,
              svg: {
                opacity: 1,
                color: isDisabled
                  ? colors.signal.canary.strong
                  : colors.structure.white,
              },
            },
          }}
          onClick={() =>
            dispatch({
              type: ACTIONS.UPDATE_FILTER,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                updatedFilter: {
                  ...filter,
                  isDisabled: !isDisabled,
                },
                filterIndex,
              },
            })
          }
        >
          <HiddenSvg
            height={ICON_SIZE}
            color={
              isDisabled ? colors.signal.canary.strong : colors.structure.steel
            }
            css={{ opacity: isDisabled ? 1 : 0 }}
          />
        </Button>

        <Button
          aria-label="Delete Filter"
          variant={VARIANTS.ICON}
          css={{
            display: 'flex',
            padding: spacing.small,
            justifyContent: 'center',
            alignItems: 'center',
            borderRadius: constants.borderRadius.small,
            ':hover, &.focus-visible:focus': {
              backgroundColor: colors.structure.smoke,
              svg: { opacity: 1, color: colors.structure.white },
            },
          }}
          onClick={() =>
            dispatch({
              type: ACTIONS.DELETE_FILTER,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                filterIndex,
              },
            })
          }
        >
          <CrossSvg
            height={ICON_SIZE}
            color={colors.structure.steel}
            css={{ opacity: 0 }}
          />
        </Button>
      </div>
    </div>
  )
}

FilterTextSearch.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterTextSearch
