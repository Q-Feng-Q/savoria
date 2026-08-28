function createRequestId(prefix = 'action') {
  const random = Math.random().toString(36).slice(2, 10);
  return `${prefix}-${Date.now()}-${random}`;
}

function createActionRequest(prefix = 'action') {
  const requestId = createRequestId(prefix);
  return {
    requestId,
    withPayload(payload = {}) {
      return { ...payload, requestId };
    }
  };
}

module.exports = { createRequestId, createActionRequest };
