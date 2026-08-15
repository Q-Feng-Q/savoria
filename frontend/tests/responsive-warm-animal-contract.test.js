const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('global shell protects safe areas and prevents horizontal overflow', () => {
  const app = read('styles/warm-kitchen-foundation.wxss')
  const tokens = read('styles/warm-kitchen-tokens.wxss')

  assert.match(tokens, /--warm-safe-bottom:\s*env\(safe-area-inset-bottom\)/)
  assert.match(tokens, /--warm-tap-min:\s*88rpx/)
  assert.match(app, /overflow-x:\s*hidden/)
  assert.match(app, /var\(--warm-safe-bottom\)/)
})

test('visual system contains a dedicated compact phone fallback', () => {
  const app = read('styles/warm-kitchen-responsive.wxss')
  const home = read('pages/family/home/index.wxss')
  const profile = read('pages/account/profile/index.wxss')

  assert.match(app, /@media\s*\(max-width:\s*359px\)/)
  assert.match(home, /@media\s*\(max-width:\s*360px\)/)
  assert.match(profile, /@media\s*\(max-width:\s*360px\)/)
})

test('core grids allow content to shrink without horizontal overflow', () => {
  const files = [
    'pages/family/home/index.wxss',
    'pages/ordering/menu/index.wxss',
    'pages/ordering/orders/index.wxss',
    'pages/account/profile/index.wxss',
    'pages/merchant/index.wxss'
  ]

  for (const file of files) {
    const source = read(file)
    assert.match(source, /minmax\(0,\s*1fr\)/, `${file} should use shrink-safe grids`)
  }
})

test('bottom action bar includes safe area spacing and full width sizing', () => {
  const source = read('components/bottom-action-bar/index.wxss')
  assert.match(source, /safe-area-inset-bottom/)
  assert.match(source, /box-sizing:\s*border-box/)
})

test('merchant workspace keeps a compact operational focus and vignette strip', () => {
  const wxml = read('pages/merchant/index.wxml')
  const wxss = read('pages/merchant/index.wxss')

  assert.match(wxml, /merchant-focus/)
  assert.match(wxml, /merchant-vignette-strip/)
  assert.match(wxml, /scenes\/merchant-(?:cooking|orders|purchase)\.webp/)
  assert.match(wxss, /@media\s*\(max-width:\s*360px\)/)
})

test('shared responsive layer covers every non-tab form workflow', () => {
  const app = read('app.wxss')
  const shared = read('styles/warm-kitchen-responsive.wxss')

  assert.match(app, /warm-kitchen-responsive\.wxss/)
  assert.match(shared, /\.warm-page--form/)
  assert.match(shared, /max-width:\s*359px/)
  assert.match(shared, /min-width:\s*420px/)
})
