function createQueryString(params = {}) {
  const query = Object.entries(params)
    .filter(([, value]) => {
      if (value === undefined || value === null || value === '') return false;
      if (typeof value === 'number' && !Number.isFinite(value)) return false;
      if (typeof value === 'string' && ['null', 'undefined', 'nan'].includes(value.trim().toLowerCase())) return false;
      return true;
    })
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`);

  return query.length ? `?${query.join('&')}` : '';
}

function unwrapData(response) {
  return response && Object.prototype.hasOwnProperty.call(response, 'data')
    ? response.data
    : response;
}

module.exports = {
  createQueryString,
  unwrapData
};
