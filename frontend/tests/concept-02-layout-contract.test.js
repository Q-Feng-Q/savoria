const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('family home implements concept 02 poster hierarchy', () => {
  const wxml = read('pages/family/home/index.wxml')
  const wxss = read('pages/family/home/index.wxss')

  for (const name of ['design-home-hero', 'home-role-strip', 'home-table-summary', 'home-recommendation', 'home-service-list']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.match(wxss, /min-height:\s*620rpx/)
  assert.match(wxss, /linear-gradient\(90deg/)
})

test('menu implements category rail and editorial dish list from concept 02', () => {
  const wxml = read('pages/ordering/menu/index.wxml')
  const dish = read('components/dish-row/index.wxss')

  for (const name of ['design-menu-header', 'design-menu-search', 'menu-category-layout', 'design-cart-dock']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.match(dish, /border-bottom/)
  assert.match(dish, /background:\s*transparent/)
})

test('profile implements animal banner family summary and linear settings groups', () => {
  const wxml = read('pages/account/profile/index.wxml')
  const wxss = read('pages/account/profile/index.wxss')

  for (const name of ['design-profile-banner', 'profile-family-summary', 'profile-settings-group']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.match(wxss, /profile-banner__art/)
})

test('every registered page receives the concept 02 page shell', () => {
  const app = JSON.parse(read('app.json'))
  const shared = read('styles/warm-kitchen-foundation.wxss')

  assert.match(read('app.wxss'), /warm-kitchen-foundation\.wxss/)
  assert.ok(app.pages.length >= 29)
  assert.match(shared, /\.warm-page/)
  assert.match(read('styles/warm-kitchen-responsive.wxss'), /safe-area-inset-bottom/)
  assert.match(read('styles/warm-kitchen-tokens.wxss'), /--warm-paper/)
})
