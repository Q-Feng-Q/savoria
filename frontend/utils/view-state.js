function createViewState(phase = 'idle', data = null) {
  return {
    phase,
    data,
    submittingAction: '',
    mutationError: '',
    successFeedback: false
  };
}

function beginMutation(state, action) {
  return {
    ...state,
    submittingAction: action,
    mutationError: '',
    successFeedback: false
  };
}

function failMutation(state, message) {
  return {
    ...state,
    submittingAction: '',
    mutationError: message || '操作失败，请稍后再试',
    successFeedback: false
  };
}

function finishMutation(state) {
  return {
    ...state,
    submittingAction: '',
    mutationError: '',
    successFeedback: true
  };
}

module.exports = {
  createViewState,
  beginMutation,
  failMutation,
  finishMutation
};
