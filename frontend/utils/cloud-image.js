function trimTrailingSlash(value) {
  return String(value || '').replace(/\/+$/, '');
}

function normalizeCloudAssetPath(source, assetBaseUrl = '') {
  const value = String(source || '').trim();
  const base = trimTrailingSlash(assetBaseUrl);
  if (base && value.startsWith(base)) {
    const path = value.slice(base.length);
    return path.startsWith('/') ? path : `/${path}`;
  }
  return value;
}

function isCloudContainerAsset(source, assetBaseUrl = '') {
  const value = String(source || '').trim();
  if (!value) return false;
  const base = trimTrailingSlash(assetBaseUrl);
  if (/^https?:\/\//i.test(value) && (!base || !value.startsWith(base))) return false;
  return /^\/(images|uploads)\//.test(normalizeCloudAssetPath(value, base));
}

function hashPath(value) {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16);
}

function extensionFor(pathname, header = {}) {
  const cleanPath = pathname.split(/[?#]/)[0];
  const pathMatch = cleanPath.match(/\.(png|jpe?g|webp|gif)$/i);
  if (pathMatch) return `.${pathMatch[1].toLowerCase().replace('jpeg', 'jpg')}`;
  const contentType = String(header['content-type'] || header['Content-Type'] || '').toLowerCase();
  if (contentType.includes('png')) return '.png';
  if (contentType.includes('webp')) return '.webp';
  if (contentType.includes('gif')) return '.gif';
  return '.jpg';
}

function mkdir(fileSystem, dirPath) {
  return new Promise((resolve, reject) => {
    fileSystem.mkdir({
      dirPath,
      recursive: true,
      success: resolve,
      fail(error) {
        if (String(error && error.errMsg).includes('file already exists')) resolve();
        else reject(error);
      }
    });
  });
}

function writeFile(fileSystem, filePath, data) {
  return new Promise((resolve, reject) => {
    fileSystem.writeFile({ filePath, data, success: resolve, fail: reject });
  });
}

function createCloudImageResolver({
  env,
  service,
  assetBaseUrl = '',
  callContainer,
  fileSystem,
  userDataPath
} = {}) {
  const cache = new Map();
  const cacheRoot = `${trimTrailingSlash(userDataPath)}/cloud-images`;
  let directoryPromise;

  return async function resolveCloudImage(source) {
    const value = String(source || '').trim();
    if (!isCloudContainerAsset(value, assetBaseUrl)) return value;
    const requestPath = normalizeCloudAssetPath(value, assetBaseUrl);
    if (cache.has(requestPath)) return cache.get(requestPath);

    const pending = (async () => {
      if (!callContainer || !fileSystem || !userDataPath) {
        throw new Error('当前环境不支持云托管图片下载');
      }
      const response = await callContainer({
        config: { env },
        path: requestPath,
        method: 'GET',
        header: { 'X-WX-SERVICE': service },
        responseType: 'arraybuffer'
      });
      if (!response || response.statusCode < 200 || response.statusCode >= 300) {
        throw new Error(`图片下载失败：${response && response.statusCode ? response.statusCode : '无响应'}`);
      }
      directoryPromise = directoryPromise || mkdir(fileSystem, cacheRoot);
      await directoryPromise;
      const localPath = `${cacheRoot}/${hashPath(requestPath)}${extensionFor(requestPath, response.header)}`;
      await writeFile(fileSystem, localPath, response.data);
      return localPath;
    })();

    cache.set(requestPath, pending);
    try {
      return await pending;
    } catch (error) {
      cache.delete(requestPath);
      throw error;
    }
  };
}

module.exports = {
  createCloudImageResolver,
  isCloudContainerAsset,
  normalizeCloudAssetPath
};
