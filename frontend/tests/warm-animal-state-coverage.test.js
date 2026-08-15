const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const readPage = (page) => fs.readFileSync(path.join(root, `${page}.wxml`), 'utf8')

const asyncPages = [
  'pages/auth/entry/index', 'pages/family/family-start/index', 'pages/family/family-management/index',
  'pages/account/profile-edit/index', 'pages/account/account-security/index',
  'pages/account/account-management/index', 'pages/family/home/index', 'pages/ordering/menu/index',
  'pages/ordering/cart/index', 'pages/ordering/orders/index', 'pages/account/profile/index',
  'pages/account/notifications/index', 'pages/family/addresses/index',
  'pages/family/address-edit/index', 'pages/family/wallet/index',
  'pages/family/wallet-ledger/index', 'pages/merchant/index',
  'pages/merchant/merchant-families/index', 'pages/merchant/merchant-family-detail/index',
  'pages/merchant/merchant-orders/index', 'pages/merchant/merchant-order-detail/index',
  'pages/merchant/merchant-dishes/index', 'pages/merchant/family-menu/index',
  'pages/merchant/purchase/index', 'pages/ordering/order-detail/index',
  'pages/ordering/dish-detail/index', 'pages/merchant/dish-edit/index',
  'pages/merchant/ingredient-edit/index'
]

const listPages = [
  'pages/family/family-management/index', 'pages/ordering/menu/index', 'pages/ordering/cart/index',
  'pages/ordering/orders/index', 'pages/account/notifications/index', 'pages/family/addresses/index',
  'pages/family/wallet-ledger/index', 'pages/merchant/merchant-families/index',
  'pages/merchant/merchant-family-detail/index', 'pages/merchant/merchant-orders/index',
  'pages/merchant/merchant-dishes/index', 'pages/merchant/family-menu/index',
  'pages/merchant/purchase/index'
]

const formPages = [
  'pages/auth/entry/index', 'pages/auth/register/index', 'pages/family/family-start/index',
  'pages/account/profile-edit/index', 'pages/account/account-security/index',
  'pages/family/address-edit/index', 'pages/ordering/cart/index',
  'pages/merchant/merchant-order-detail/index', 'pages/merchant/family-menu/index',
  'pages/merchant/dish-edit/index', 'pages/merchant/ingredient-edit/index'
]

test('async pages expose loading error and retry state affordances', () => {
  for (const page of asyncPages) assert.match(readPage(page), /page-state|warm-state/, page)
})

test('list pages expose an empty state', () => {
  for (const page of listPages) assert.match(readPage(page), /empty|空|暂无|<page-state[^>]+phase="\{\{phase\}\}"/, page)
})

test('form pages expose guarded submit feedback', () => {
  for (const page of formPages) assert.match(readPage(page), /loading|submitting|提交|保存|登录|注册|下单/, page)
})
