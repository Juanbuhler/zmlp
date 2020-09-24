export const ACTIONS = {
  TOGGLE_OPEN: 'TOGGLE_OPEN',
  TOGGLE_VISIBLE: 'TOGGLE_VISIBLE',
  TOGGLE_VISIBLE_ALL: 'TOGGLE_VISIBLE_ALL',
  UPDATE_FILTER: 'UPDATE_FILTER',
}

export const INITIAL_STATE = { filter: '', modules: {} }

export const reducer = (
  state,
  { type: actionType, payload: { name, detections, value } = {} },
) => {
  const module = state.modules[name] || {}

  switch (actionType) {
    case ACTIONS.TOGGLE_OPEN: {
      if (state.modules[name]?.isOpen === true) {
        return {
          ...state,
          modules: { ...state.modules, [name]: { ...module, isOpen: false } },
        }
      }

      return {
        ...state,
        modules: { ...state.modules, [name]: { ...module, isOpen: true } },
      }
    }

    case ACTIONS.TOGGLE_VISIBLE: {
      if (state.modules[name]?.isVisible === false) {
        return {
          ...state,
          modules: { ...state.modules, [name]: { ...module, isVisible: true } },
        }
      }

      return {
        ...state,
        modules: { ...state.modules, [name]: { ...module, isVisible: false } },
      }
    }

    case ACTIONS.TOGGLE_VISIBLE_ALL: {
      const isAllVisible = Object.values(state.modules).every(
        ({ isVisible }) => isVisible === true,
      )

      return {
        ...state,
        modules: Object.values(detections).reduce(
          (acc, { name: moduleName }) => ({
            ...acc,
            [moduleName]: {
              ...state.modules[moduleName],
              isVisible: !isAllVisible,
            },
          }),
          [],
        ),
      }
    }

    case ACTIONS.UPDATE_FILTER: {
      return { ...state, filter: value }
    }

    default:
      return state
  }
}