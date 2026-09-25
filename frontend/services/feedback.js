const { buildAuthHeaders, normalizeBackendPath } = require('../utils/api');
const transfer = (method) => (options) => new Promise((resolve, reject) => {
  wx[method]({ ...options, success: resolve, fail: reject });
});

function removeTempFile(path) {
  if (path && typeof wx !== 'undefined' && wx.getFileSystemManager) {
    wx.getFileSystemManager().unlink({ filePath: path, fail() {} });
  }
}

function createFeedbackService({ request, baseUrl = '', getSession = () => null,
  upload = transfer('uploadFile'), download = transfer('downloadFile') } = {}) {
  const url = (path) => String(baseUrl).replace(/\/+$/, '') + normalizeBackendPath(path);
  return {
    async list(page = 1) { return (await request(`/api/feedback?page=${page}&pageSize=20`, { method: 'GET' })).data; },
    async detail(id) { return (await request(`/api/feedback/${encodeURIComponent(id)}`, { method: 'GET' })).data; },
    async submit(data) { return (await request('/api/feedback', { method: 'POST', data })).data; },
    async uploadImage(filePath) {
      const response = await upload({ url: url('/api/feedback/images'), filePath, name: 'file', header: buildAuthHeaders(getSession()) });
      let payload;
      try { payload = typeof response.data === 'string' ? JSON.parse(response.data) : response.data; }
      catch (_) { throw new Error('图片上传失败，请重试'); }
      if (response.statusCode !== 200 || !payload || payload.code !== 0) {
        const error = new Error((payload && payload.message) || '图片上传失败，请重试');
        error.statusCode = response.statusCode;
        throw error;
      }
      return payload.data;
    },
    async downloadImage(id) {
      const response = await download({ url: url(`/api/feedback/images/${encodeURIComponent(id)}`), header: buildAuthHeaders(getSession()) });
      if (response.statusCode !== 200 || !response.tempFilePath) {
        removeTempFile(response.tempFilePath);
        throw new Error('图片读取失败，请重试');
      }
      return response.tempFilePath;
    }
  };
}

module.exports = { createFeedbackService, removeTempFile };
