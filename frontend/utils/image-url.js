function trimTrailingSlash(value) {
  return String(value || '').replace(/\/+$/, '');
}

function toImageUrl(baseUrl, url) {
  if (!url) return '';
  if (/^https?:\/\//.test(url)) return url;
  return `${trimTrailingSlash(baseUrl)}${url}`;
}

module.exports = {
  toImageUrl
};
