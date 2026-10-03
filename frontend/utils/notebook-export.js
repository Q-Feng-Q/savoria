const CLIPBOARD_LIMIT_BYTES = 1024 * 1024;

function prepareExport(payload, clipboardLimit = CLIPBOARD_LIMIT_BYTES) {
  if (!payload || payload.schemaVersion !== 'notebook-export/v1') {
    throw new Error('记事导出格式不受支持');
  }
  const text = JSON.stringify(payload, null, 2);
  const byteLength = typeof TextEncoder === 'function' ? new TextEncoder().encode(text).length
    : unescape(encodeURIComponent(text)).length;
  return { text, byteLength, tooLargeForClipboard: byteLength > clipboardLimit };
}

function writeExportFile(payload, { fileSystem, userDataPath, now = Date.now() } = {}) {
  if (!fileSystem || !userDataPath || !/^wxfile:\/\/usr(?:\/|$)/.test(userDataPath)) {
    return Promise.reject(new Error('无法安全写入记事导出文件'));
  }
  const filePath = `${userDataPath.replace(/\/+$/, '')}/notebook-export-${now}.json`;
  const data = prepareExport(payload).text;
  return new Promise((resolve, reject) => fileSystem.writeFile({ filePath, data, encoding: 'utf8',
    success: () => resolve(filePath), fail: reject }));
}

module.exports = { prepareExport, writeExportFile, CLIPBOARD_LIMIT_BYTES };
