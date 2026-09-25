export const MAX_STEP_IMAGES = 5;
export function validateDishStepImages(steps = []) {
  for (const step of steps) {
    if (step.imageUrls && (!Array.isArray(step.imageUrls) || step.imageUrls.length > MAX_STEP_IMAGES)) return '每个步骤最多 5 张图片';
    if (step.imageUrls?.length && !String(step.content || '').trim()) return '请填写已上传图片的步骤内容';
  }
  return '';
}
export async function uploadStepImages({existing = [], files = [], upload, isCurrent = () => true}) {
  if (existing.length + files.length > MAX_STEP_IMAGES) throw new Error('每个步骤最多 5 张图片');
  const images = [...existing]; let failed = 0;
  for (const file of files) {
    if (!isCurrent()) return {images:[],failed,stale:true};
    try {
      if (file.size > 4 * 1024 * 1024) throw new Error('图片不能超过 4MB');
      const result = await upload(file);
      if (!isCurrent()) return {images:[],failed,stale:true};
      if (!result.url) throw new Error('图片地址缺失');
      images.push(result.url);
    } catch (_) { failed++; }
  }
  return {images,failed,stale:!isCurrent()};
}

export function stepImageUrl(url, base = '/api') {
  if (typeof url !== 'string' || !url || url.length > 2048 || /[\s\\\u0000-\u001f\u007f]/.test(url)) return '';
  try {
    const decoded=decodeURIComponent(url.split(/[?#]/)[0]);
    if (/[\\\u0000-\u001f\u007f]/.test(decoded) || decoded.split('/').some(part => part === '.' || part === '..')) return '';
    if (/^https:\/\//i.test(url)) {
      const parsed=new URL(url);
      return parsed.hostname && !parsed.username && !parsed.password ? url : '';
    }
  } catch { return ''; }
  if (!/^\/(?:uploads\/images|uploads\/dish-template-assets|images)\//.test(url) || /[\s\\]/.test(url) || url.includes('/../')) return '';
  if (/^https?:\/\//i.test(base)) return new URL(url, base).href;
  return `${String(base).replace(/\/$/,'')}${url}`;
}
