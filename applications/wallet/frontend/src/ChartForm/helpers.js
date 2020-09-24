import { capitalizeFirstLetter } from '../Text/helpers'

export const CHART_TYPE_FIELDS = {
  range: ['range'],
  facet: ['facet', 'labelConfidence'],
  histogram: ['labelConfidence'],
}

export const formatFields = ({ fields, type, path = '' }) => {
  const enabledFields = CHART_TYPE_FIELDS[type]

  if (Array.isArray(fields)) {
    if (fields.filter((f) => enabledFields.includes(f)).length > 0) {
      return path
    }

    return null
  }

  return Object.keys(fields).reduce((acc, field) => {
    const child = formatFields({
      fields: fields[field],
      type,
      path: path ? [path, field].join('.') : field,
    })

    if (!child || Object.keys(child).length === 0) return acc

    return {
      ...acc,
      [typeof child === 'string'
        ? field
        : capitalizeFirstLetter({ word: field })]: child,
    }
  }, {})
}
