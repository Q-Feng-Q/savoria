const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const migratedWxmlFiles = [
  'components/action-button/index.wxml',
  'components/bottom-action-bar/index.wxml',
  'components/notification-row/index.wxml',
  'components/order-row/index.wxml',
  'components/page-state/index.wxml',
  'components/purchase-row/index.wxml',
  'components/quantity-stepper/index.wxml',
  'pages/account/account-management/index.wxml',
  'pages/account/account-security/index.wxml',
  'pages/account/notifications/index.wxml',
  'pages/family/address-edit/index.wxml',
  'pages/family/addresses/index.wxml',
  'pages/family/family-management/index.wxml',
  'pages/family/family-start/index.wxml',
  'pages/family/home/index.wxml',
  'pages/family/wallet/index.wxml',
  'pages/merchant/dish-edit/index.wxml',
  'pages/merchant/family-menu/index.wxml',
  'pages/merchant/index.wxml',
  'pages/merchant/ingredient-edit/index.wxml',
  'pages/merchant/merchant-dishes/index.wxml',
  'pages/merchant/merchant-families/index.wxml',
  'pages/merchant/merchant-family-detail/index.wxml',
  'pages/merchant/merchant-order-detail/index.wxml',
  'pages/merchant/merchant-orders/index.wxml',
  'pages/merchant/purchase/index.wxml',
  'pages/ordering/cart/index.wxml',
  'pages/ordering/dish-detail/index.wxml',
  'pages/ordering/menu/index.wxml'
]

const listFiles = (directory, extension) => fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
  const fullPath = path.join(directory, entry.name)
  if (entry.isDirectory()) return listFiles(fullPath, extension)
  return entry.name.endsWith(extension) ? [fullPath] : []
})

test('production WXML contains no native WeChat buttons', () => {
  for (const file of listFiles(root, '.wxml')) {
    const source = fs.readFileSync(file, 'utf8')
    assert.doesNotMatch(source, /<\/?button\b/, path.relative(root, file))
  }
})

test('every migrated button root has custom semantics and press feedback', () => {
  for (const relativePath of migratedWxmlFiles) {
    const source = fs.readFileSync(path.join(root, relativePath), 'utf8')
    const controls = (source.match(/<view(?:"[^"]*"|'[^']*'|[^>])*>/g) || []).filter((tag) => /class="[^"]*\bui-button\b/.test(tag))
    assert.ok(controls.length > 0, `${relativePath} should expose migrated ui-button controls`)
    for (const control of controls) {
      assert.match(control, /aria-role="button"/, `${relativePath}: ${control}`)
      assert.match(control, /aria-label="[^"]+"/, `${relativePath}: ${control}`)
      assert.match(control, /hover-class="[^"]+"/, `${relativePath}: ${control}`)
    }
  }
})

test('production WXSS contains no native button tag selectors', () => {
  const selectorPattern = /(^|[,{]\s*|\s+)button(?=[:.#\s>{+~])/m
  for (const file of listFiles(root, '.wxss')) {
    const source = fs.readFileSync(file, 'utf8')
    assert.doesNotMatch(source, selectorPattern, path.relative(root, file))
  }
})

test('merchant custom tap controls expose semantics and press feedback', () => {
  const merchantRoot = path.join(root, 'pages', 'merchant')
  for (const file of listFiles(merchantRoot, '.wxml')) {
    const source = fs.readFileSync(file, 'utf8')
    const controls = (source.match(/<view\b[^>]*\bbindtap="[^"]+"[^>]*>/g) || [])
    for (const control of controls) {
      assert.match(control, /aria-role="(?:button|checkbox)"/, `${path.relative(root, file)}: ${control}`)
      assert.match(control, /aria-label="[^"]+"/, `${path.relative(root, file)}: ${control}`)
      assert.match(control, /hover-class="[^"]+"/, `${path.relative(root, file)}: ${control}`)
    }
  }
})
