const MAX_STEP_IMAGES = 5;
function validateStepImages(images) {
  if (images == null) return '';
  if (!Array.isArray(images) || images.length > MAX_STEP_IMAGES) return '每个步骤最多 5 张图片';
  if (images.some(url => typeof url !== 'string' || url.length > 2048 || /[\s\\\u0000-\u001f\u007f]/.test(url)
    || !(/^(?:\/uploads\/(?:images|dish-template-assets)\/|\/images\/)[a-zA-Z0-9_./-]+$/.test(url) || /^https:\/\/[a-zA-Z0-9.-]+(?::\d+)?\/[^\s]*$/.test(url))
    || url.includes('/../'))) return '步骤图片地址无效';
  return '';
}

async function uploadStepImages({ existing = [], files = [], upload, isCurrent = () => true }) {
  if (existing.length + files.length > MAX_STEP_IMAGES) throw new Error('每个步骤最多 5 张图片');
  const images = [...existing];
  let failed = 0;
  for (const file of files) {
    if (!isCurrent()) return { images: [], failed, stale: true };
    try {
      if (Number(file.size || 0) > 4 * 1024 * 1024) throw new Error('图片不能超过 4MB');
      const result = await upload(file.tempFilePath);
      if (!isCurrent()) return { images: [], failed, stale: true };
      const url = result.url;
      if (!url || validateStepImages([url])) throw new Error('图片地址无效');
      images.push(url);
    } catch (_) { failed++; }
  }
  return { images, failed, stale: !isCurrent() };
}
module.exports = { MAX_STEP_IMAGES, validateStepImages, uploadStepImages };
