const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.join(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('orders page exposes warm overview, status summary and responsive list structure', () => {
  const wxml = read('pages/ordering/orders/index.wxml')
  const wxss = read('pages/ordering/orders/index.wxss')

  for (const name of ['orders-overview', 'orders-overview__metrics', 'orders-filter', 'orders-status-summary', 'orders-list-scroll']) {
    assert.match(wxml, new RegExp(name))
  }
  assert.match(wxml, /order-row/)
  assert.match(wxss, /orders-list-scroll/)
  assert.match(wxss, /@media\s*\(max-height:\s*620px\)/)
  assert.match(wxss, /@media\s*\(orientation:\s*landscape\)/)
})
