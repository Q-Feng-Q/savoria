const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('family home implements approved illustrated hierarchy without the old oversized poster', () => {
  const wxml = read('pages/family/home/index.wxml')
  const wxss = read('pages/family/home/index.wxss')

  for (const name of ['home-brand-header', 'home-family-banner', 'home-time-row', 'home-recommendation', 'home-service-list']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.doesNotMatch(wxml, /warm-home-poster|当前餐次/)
  assert.match(wxss, /grid-template-columns:repeat\(4,minmax\(0,1fr\)\)/)
})

test('family home exposes the approved warm dining dashboard sections', () => {
  const wxml = read('pages/family/home/index.wxml')
  for (const name of ['home-brand-header', 'home-family-overview', 'home-order-metrics', 'home-featured-swiper']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.match(wxml, /订单状态/)
  assert.match(wxml, /今天预计开饭时间/)
})

test('menu implements category rail and editorial dish list from concept 02', () => {
  const wxml = read('pages/ordering/menu/index.wxml')
  const dish = read('components/dish-row/index.wxss')

  for (const name of ['design-menu-header', 'design-menu-search', 'menu-category-layout']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.match(dish, /border-bottom/)
  assert.match(dish, /background:\s*transparent/)
  assert.doesNotMatch(wxml, /design-cart-dock|bottom-action-bar|primaryText="去餐篮"/)
})

test('family home opens the shared cart through the dedicated tab', () => {
  const source = read('pages/family/home/index.js')

  assert.match(source, /openQuickEntry[\s\S]*?wx\.switchTab\(\{ url \}\)/)
  assert.equal(source.includes("wx.navigateTo({ url: '/pages/ordering/cart/index' })"), false)
})

test('ordering menu category rail is interactive and labels the selected category', () => {
  const wxml = read('pages/ordering/menu/index.wxml')
  const source = read('pages/ordering/menu/index.js')

  assert.match(wxml, /data-key="\{\{item\.key\}\}"/)
  assert.match(wxml, /bindtap="selectCategory"/)
  assert.match(wxml, /\{\{activeCategoryLabel\}\}/)
  assert.doesNotMatch(wxml, /\{\{crew\.chefRecommendationLabel\}\}/)
  assert.match(source, /selectCategory\(event\)/)
})

test('ordering menu keeps the category rail fixed beside an independently scrolling dish list', () => {
  const wxml = read('pages/ordering/menu/index.wxml')
  const wxss = read('pages/ordering/menu/index.wxss')

  assert.match(wxml, /class="category-rail"[^>]*scroll-y[^>]*scroll-into-view="\{\{categoryScrollIntoView\}\}"/)
  assert.match(wxml, /class="dish-list dish-list-scroll"[^>]*scroll-y[^>]*bindscroll="handleDishScroll"/)
  assert.match(wxml, /scroll-into-view="\{\{dishScrollIntoView\}\}"/)
  assert.match(wxml, /id="\{\{item\.anchorId\}\}"[^>]*class="menu-category-section"/)
  assert.match(wxss, /\.menu-page\s*\{[^}]*height:\s*100vh[^}]*display:\s*flex[^}]*overflow:\s*hidden/s)
  assert.match(wxss, /\.menu-category-layout\s*\{[^}]*flex:\s*1[^}]*min-height:\s*0/s)
  assert.match(wxss, /\.category-rail,.dish-list-scroll\s*\{[^}]*height:\s*100%/s)
  assert.match(wxss, /@media\s*\(max-height:\s*620px\)/)
  assert.match(wxss, /@media\s*\(orientation:\s*landscape\)/)
})

test('profile implements animal banner family summary and linear settings groups', () => {
  const wxml = read('pages/account/profile/index.wxml')
  const wxss = read('pages/account/profile/index.wxss')

  for (const name of ['design-profile-banner', 'profile-family-summary', 'profile-settings-group']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.match(wxss, /profile-story-art/)
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
