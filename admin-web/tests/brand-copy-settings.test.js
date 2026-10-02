const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const source = (name) => fs.readFileSync(path.join(__dirname, '../src', name), 'utf8');
const helper = async () => import(`data:text/javascript;base64,${Buffer.from(source('utils/branding.js')).toString('base64')}`);

test('brand copy defaults and custom values are normalized', async () => {
  const { BRAND_COPY_DEFAULTS, normalizeBrandCopy } = await helper();
  const result = normalizeBrandCopy({ pickupMessage: '  自定义到店寄语  ' });
  assert.equal(result.pickupMessage, '自定义到店寄语');
  assert.equal(result.brandTagline, BRAND_COPY_DEFAULTS.brandTagline);
  assert.equal(result.deliveryMessage, BRAND_COPY_DEFAULTS.deliveryMessage);
  assert.equal(Object.keys(result).length, 8);
});

test('brand copy payload emits eight optional fields and validates limits', async () => {
  const { BRAND_COPY_DEFAULTS, brandCopyPayload } = await helper();
  const result = brandCopyPayload({ ...BRAND_COPY_DEFAULTS, pickupMessage: '   ', deliveryMessage: '  正在送达  ' });
  assert.equal(result.pickupMessage, null);
  assert.equal(result.deliveryMessage, '正在送达');
  assert.equal(Object.keys(result).length, 8);
  assert.throws(() => brandCopyPayload({ ...BRAND_COPY_DEFAULTS, pickupMessage: '字'.repeat(81) }), /80/);
  assert.throws(() => brandCopyPayload({ ...BRAND_COPY_DEFAULTS, brandTagline: '字'.repeat(121) }), /120/);
});

test('system settings composes the copy editor into the existing save flow', () => {
  const view = source('views/platform/SystemSettingsView.vue');
  const component = source('components/BrandCopySettings.vue');
  assert.match(view, /<BrandCopySettings/);
  assert.match(view, /@update:model-value="Object\.assign\(form, \$event\)"/);
  assert.match(view, /brandCopyPayload\(form\)/);
  assert.match(component, /SectionCard title="品牌文案"/);
  for (const key of ['brandTagline', 'homeHeroTagline', 'homeFooterMessage', 'cartHeroTagline', 'deliveryMessage', 'pickupMessage', 'cartFooterMessage', 'profileWelcomeMessage']) {
    assert.match(component, new RegExp(key));
  }
});
