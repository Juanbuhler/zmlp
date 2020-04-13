import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const IMG_HEIGHT = 32

const DataSourcesEditProvider = ({
  provider: { name, logo, description, categories },
  modules,
  onClick,
}) => {
  return (
    <Accordion title={<img src={logo} alt={name} height={IMG_HEIGHT} />}>
      <>
        <p
          css={{
            color: colors.structure.zinc,
            margin: 0,
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
            maxWidth: constants.paragraph.maxWidth,
          }}
        >
          {description}
        </p>
        {categories.map((category) => (
          <CheckboxTable
            key={category.name}
            category={{
              name: category.name,
              options: category.modules.map((module) => {
                return {
                  value: module.name,
                  label: module.description,
                  initialValue: modules[module.name] || false,
                  isDisabled: module.restricted,
                }
              }),
            }}
            onClick={onClick}
          />
        ))}
      </>
    </Accordion>
  )
}

DataSourcesEditProvider.propTypes = {
  provider: PropTypes.shape({
    name: PropTypes.string.isRequired,
    logo: PropTypes.node.isRequired,
    description: PropTypes.node.isRequired,
    categories: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        modules: PropTypes.arrayOf(PropTypes.shape()).isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  modules: PropTypes.object.isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesEditProvider
