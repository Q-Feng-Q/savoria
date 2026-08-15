const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')
const app = JSON.parse(read('app.json'))

const components = [
  'page-state', 'brand-scene', 'action-button', 'filter-bar', 'group-menu',
  'quantity-stepper', 'dish-row', 'order-row', 'purchase-row',
  'notification-row', 'status-timeline', 'bottom-action-bar'
]

test('all 33 registered pages use the new warm kitchen view shell', () => {
  assert.equal(app.pages.length, 33)
  for (const page of app.pages) {
    const wxml = read(`${page}.wxml`)
    assert.match(wxml, /warm-page/, `${page} must use warm-page`)
  }
})

test('app removes old visual imports and loads the new foundation', () => {
  const wxss = read('app.wxss')
  assert.doesNotMatch(wxss, /animal-theme|responsive-pages|concept-02-pages/)
  assert.match(wxss, /warm-kitchen-tokens/)
  assert.match(wxss, /warm-kitchen-foundation/)
  assert.match(wxss, /warm-kitchen-responsive/)
})

test('all global visual components are rebuilt with warm component roots', () => {
  for (const component of components) {
    assert.ok(fs.existsSync(path.join(root, 'components', component, 'index.wxml')))
    assert.ok(fs.existsSync(path.join(root, 'components', component, 'index.wxss')))
    assert.match(read(`components/${component}/index.wxml`), /warm-/, `${component} must use a warm root class`)
  }
})

test('anchor and merchant pages expose their approved visual roles', () => {
  assert.match(read('pages/family/home/index.wxml'), /小熊主厨/)
  assert.match(read('pages/ordering/menu/index.wxml'), /兔子帮厨/)
  assert.match(read('pages/account/profile/index.wxml'), /橘猫/)
  assert.match(read('pages/merchant/index.wxml'), /merchant-vignette-strip/)
})
