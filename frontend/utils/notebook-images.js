const { buildAuthHeaders, normalizeBackendPath } = require('./api');

function transfer(method) {
  return (options) => new Promise((resolve, reject) => {
    wx[method]({ ...options, success: resolve, fail: reject });
  });
}

function createNotebookImages({ baseUrl = '', getSession = () => null,
  upload = transfer('uploadFile'), download = transfer('downloadFile'),
  unlink = (filePath) => wx.getFileSystemManager().unlink({ filePath, fail() {} }) } = {}) {
  const url = (path) => `${String(baseUrl).replace(/\/+$/, '')}${normalizeBackendPath(path)}`;
  const sameSession = (session) => {
    const current = getSession();
    return Boolean(current && session && current.userId === session.userId
      && current.accessToken === session.accessToken);
  };
  async function send(filePath, formData, endpoint) {
    const session = getSession();
    if (!filePath || !session) throw new Error('请先登录后上传图片');
    const response = await upload({ url: url(endpoint), filePath, name: 'file', formData,
      header: buildAuthHeaders(session) });
    if (!sameSession(session)) throw new Error('账号已切换，请重新打开记事');
    let payload;
    try { payload = typeof response.data === 'string' ? JSON.parse(response.data) : response.data; }
    catch (_) { throw new Error('图片上传失败，请重试'); }
    if (response.statusCode !== 200 || !payload || payload.code !== 0
        || !payload.data || !payload.data.valueKey) {
      throw new Error((payload && payload.message) || '图片上传失败，请重试');
    }
    return { imageId: payload.data.imageId, valueKey: payload.data.valueKey };
  }
  return {
    stage: (eventId, filePath) => send(filePath, { eventId }, '/api/notebook/images/staged'),
    upload: (recordId, filePath) => send(filePath, { recordId }, '/api/notebook/images'),
    async preview(imageId) {
      const session = getSession();
      if (!session) throw new Error('请先登录后查看图片');
      const response = await download({ url: url(`/api/notebook/images/${encodeURIComponent(imageId)}`),
        header: buildAuthHeaders(session) });
      if (!sameSession(session)) {
        if (response.tempFilePath) unlink(response.tempFilePath);
        throw new Error('账号已切换，请重新打开记事');
      }
      if (response.statusCode !== 200 || !response.tempFilePath) {
        if (response.tempFilePath) unlink(response.tempFilePath);
        throw new Error('图片读取失败，请重试');
      }
      return response.tempFilePath;
    }
  };
}

module.exports = { createNotebookImages };
