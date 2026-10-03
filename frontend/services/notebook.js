const { createQueryString, unwrapData } = require('./_shared');

// Keep every notebook endpoint in one account-scoped module so it can move out later.
function createNotebookService({ request }) {
  const data = (path, method = 'GET', payload) => request(`/api/notebook${path}`,
    { method, ...(payload === undefined ? {} : { data: payload }) }).then(unwrapData);
  return {
    getConfig: () => data('/config'),
    listEvents: (includeArchived = false) => data(`/events${createQueryString({ includeArchived })}`),
    createEvent: (draft) => data('/events', 'POST', draft),
    getEvent: (id) => data(`/events/${id}`),
    updateEvent: (id, patch) => data(`/events/${id}`, 'PATCH', patch),
    getDeleteImpact: (id) => data(`/events/${id}/delete-impact`),
    deleteEvent: (id) => data(`/events/${id}?confirm=true`, 'DELETE'),
    orderEvents: (eventIds) => data('/events/order', 'PUT', { eventIds }),
    listTemplates: (id) => data(`/events/${id}/templates`),
    publishTemplate: (id, fields) => data(`/events/${id}/templates`, 'POST', { fields }),
    listRecords: (id, query) => data(`/events/${id}/records${createQueryString(query)}`),
    getCalendar: (id, query) => data(`/events/${id}/calendar${createQueryString(query)}`),
    createRecord: (id, draft) => data(`/events/${id}/records`, 'POST', draft),
    getRecord: (id) => data(`/records/${id}`),
    updateRecord: (id, patch) => data(`/records/${id}`, 'PATCH', patch),
    upgradeRecord: (id, values, expectedVersion) => data(`/records/${id}/upgrade-template`, 'POST',
      { values, expectedVersion }),
    deleteRecord: (id) => data(`/records/${id}`, 'DELETE'),
    listContacts: () => data('/contacts'),
    listInvites: () => data('/contacts/invites'),
    inviteContact: (identifier) => data('/contacts/invites', 'POST', { identifier }),
    acceptInvite: (id, token) => data(`/contacts/invites/${id}/accept`, 'POST', { token }),
    rejectInvite: (id, token) => data(`/contacts/invites/${id}/reject`, 'POST', { token }),
    removeContact: (id) => data(`/contacts/${id}`, 'DELETE'),
    listGrants: (eventId) => data(`/events/${eventId}/grants`),
    createGrant: (eventId, grant) => data(`/events/${eventId}/grants`, 'POST', grant),
    updateGrant: (id, grant) => data(`/grants/${id}`, 'PATCH', grant),
    revokeGrant: (id) => data(`/grants/${id}`, 'DELETE'),
    listShared: () => data('/shared'),
    exportEvent: (id, range, mode) => data(`/events/${id}/export`, 'POST', { ...range, mode }),
    listAudit: (query) => data(`/audit${createQueryString(query)}`)
  };
}

module.exports = { createNotebookService };
