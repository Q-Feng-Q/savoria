const { buildAuthHeaders } = require('../utils/api');
const { toImageUrl } = require('../utils/image-url');

function defaultUploadAdapter(options) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      ...options,
      success: resolve,
      fail: reject
    });
  });
}

function createFilesService({ baseUrl = '', getSession = () => null, upload = defaultUploadAdapter } = {}) {
  return {
    async uploadImage(filePath) {
      const response = await upload({
        url: `${String(baseUrl || '').replace(/\/+$/, '')}/api/files/images`,
        filePath,
        name: 'file',
        header: buildAuthHeaders(getSession())
      });
      const payload = typeof response.data === 'string' ? JSON.parse(response.data) : (response.data || {});
      if ((response.statusCode && response.statusCode >= 400) || payload.code !== 0) {
        const error = new Error(payload.message || 'upload failed');
        error.code = payload.code || response.statusCode;
        throw error;
      }

      return {
        ...payload.data,
        imageUrl: toImageUrl(baseUrl, payload.data.url)
      };
    }
  };
}

module.exports = {
  createFilesService
};
