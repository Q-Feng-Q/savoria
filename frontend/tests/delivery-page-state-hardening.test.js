const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (page, extension) => fs.readFileSync(path.join(root, `${page}.${extension}`), 'utf8')

const hardenedPages = [
  'pages/account/notifications/index',
  'pages/account/profile-edit/index',
  'pages/family/addresses/index',
  'pages/family/address-edit/index',
  'pages/family/wallet/index',
  'pages/family/wallet-ledger/index',
  'pages/merchant/dish-template-detail/index',
  'pages/merchant/dish-templates/index',
  'pages/ordering/orders/index',
  'pages/ordering/order-detail/index',
  'pages/ordering/dish-detail/index'
]

test('data-driven pages expose retryable loading and error states instead of blank screens', () => {
  for (const page of hardenedPages) {
    const source = read(page, 'js')
    const markup = read(page, 'wxml')
    assert.match(source, /phase:\s*'loading'/, `${page}: initial loading phase`)
    assert.match(source, /phase:\s*'error'/, `${page}: error transition`)
    assert.match(source, /retryLoad\s*\(\)\s*\{\s*return this\.load\(/, `${page}: retry handler`)
    assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/, `${page}: retryable page state`)
    assert.match(markup, /wx:if="\{\{phase === 'ready'\}\}"/, `${page}: ready content gate`)
  }
})

test('high-risk page mutations reject duplicate taps and expose local busy semantics', () => {
  const expectations = {
    'pages/account/notifications/index': [/busyReadMap/, /markingAllRead/],
    'pages/account/profile-edit/index': [/if \(this\.data\.saving\) return/, /disabled="\{\{saving\}\}"/],
    'pages/family/addresses/index': [/busyAddressMap/, /aria-busy="\{\{busyAddressMap\[item\.id\]\}\}"/],
    'pages/family/address-edit/index': [/if \(this\.data\.saving\) return/, /aria-busy="\{\{saving\}\}"/],
    'pages/merchant/dish-template-detail/index': [/if \(this\.data\.importing/, /aria-busy="\{\{importing\}\}"/],
    'pages/ordering/order-detail/index': [/if \(this\.data\.mutationBusy\) return/, /disabled="\{\{mutationBusy\}\}"/]
  }
  for (const [page, patterns] of Object.entries(expectations)) {
    const combined = `${read(page, 'js')}\n${read(page, 'wxml')}`
    for (const pattern of patterns) assert.match(combined, pattern, page)
  }
})

test('detail pages reject missing identifiers before issuing API requests', () => {
  for (const page of ['pages/ordering/order-detail/index', 'pages/ordering/dish-detail/index']) {
    const source = read(page, 'js')
    assert.match(source, /if \(!this\.data\.id\)/, page)
    assert.match(source, /缺少.*编号/, page)
  }
})
