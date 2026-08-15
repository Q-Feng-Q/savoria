const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('fresh warm cartoon visual system exposes semantic design tokens', () => {
  const tokens = read('styles/warm-kitchen-tokens.wxss')
  const app = read('styles/warm-kitchen-foundation.wxss')

  for (const token of [
    '--warm-bg', '--warm-sage', '--warm-peach', '--warm-muted',
    '--warm-page-gap', '--warm-motion-enter'
  ]) assert.match(tokens, new RegExp(token))

  assert.match(app, /\.warm-page/)
  assert.match(app, /\.warm-section-head/)
  assert.match(app, /\.warm-management-hero/)
  assert.match(app, /\.warm-primary-action/)
})

test('family home uses a cartoon story hero and task-led service dock', () => {
  const wxml = read('pages/family/home/index.wxml')
  const wxss = read('pages/family/home/index.wxss')

  assert.match(wxml, /home-story/)
  assert.match(wxml, /home-story__mascot/)
  assert.match(wxml, /today-board/)
  assert.match(wxml, /service-dock/)
  assert.match(wxss, /var\(--sk-wheat\)/)
  assert.match(wxss, /home-rise/)
})

test('ordering and profile surfaces share the redesigned hierarchy', () => {
  const menu = read('pages/ordering/menu/index.wxml')
  const orders = read('pages/ordering/orders/index.wxml')
  const profile = read('pages/account/profile/index.wxml')
  const merchant = read('pages/merchant/index.wxml')

  assert.match(menu, /menu-intro/)
  assert.match(orders, /orders-overview/)
  assert.match(profile, /profile-dashboard/)
  assert.match(merchant, /merchant-overview/)
  assert.match(merchant, /merchant-focus/)
})

test('shared rows use the warm paper interaction language', () => {
  const dish = read('components/dish-row/index.wxss')
  const order = read('components/order-row/index.wxss')
  const action = read('components/action-button/index.wxss')

  assert.match(dish, /var\(--sk-paper\)/)
  assert.match(order, /var\(--sk-paper\)/)
  assert.match(action, /var\(--sk-accent\)/)
})
