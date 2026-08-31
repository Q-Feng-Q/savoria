const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const manifest = require('./fixtures/delivery-audit-manifest')
const root = path.resolve(__dirname, '..')

const readPageScript = (page) => fs.readFileSync(path.join(root, `${page}.js`), 'utf8')

test('paged collection loader requests every page, preserves order and deduplicates rows', async () => {
  const { loadAllPages } = require('../utils/pagination')
  const requestedPages = []
  const result = await loadAllPages(async ({ page, pageSize }) => {
    requestedPages.push(page)
    const pages = {
      1: [{ id: 1 }, { id: 2 }],
      2: [{ id: 2 }, { id: 3 }],
      3: [{ id: 4 }]
    }
    return { items: pages[page] || [], total: 4, page, pageSize }
  }, { pageSize: 2, keyOf: (item) => item.id })

  assert.deepEqual(requestedPages, [1, 2, 3])
  assert.deepEqual(result.map(({ id }) => id), [1, 2, 3, 4])
})

test('paged collection loader also supports list-only responses', async () => {
  const { loadAllPages } = require('../utils/pagination')
  const requestedPages = []
  const result = await loadAllPages(async ({ page }) => {
    requestedPages.push(page)
    return page === 1 ? [{ id: 1 }, { id: 2 }] : [{ id: 3 }]
  }, { pageSize: 2, keyOf: (item) => item.id })

  assert.deepEqual(requestedPages, [1, 2])
  assert.deepEqual(result.map(({ id }) => id), [1, 2, 3])
})

test('notifications and wallet ledger load every backend page', () => {
  const notifications = readPageScript('pages/account/notifications/index')
  const walletLedger = readPageScript('pages/family/wallet-ledger/index')
  assert.match(notifications, /loadAllPages\(/)
  assert.match(notifications, /getNotifications\(\{[\s\S]*?page,[\s\S]*?pageSize/)
  assert.match(walletLedger, /loadAllPages\(/)
  assert.match(walletLedger, /getFamilyWalletLedgers\([^,]+,\s*\{\s*page,\s*pageSize\s*\}\)/)
  assert.match(walletLedger, /getWalletLedgers\(\{\s*page,\s*pageSize\s*\}\)/)
})

test('change request pagination deduplicates rows by request id', () => {
  const source = readPageScript('pages/merchant/dish-template-changes/index')
  assert.match(source, /mergeUniqueRows\(this\.data\.rows, next, \(item\) => item\.requestId\)/)
})

test('every refresh-sensitive page reloads from onShow', () => {
  for (const [page, metadata] of Object.entries(manifest.pages)) {
    if (!metadata.refresh) continue
    assert.match(readPageScript(page), /\bonShow\s*\([^)]*\)\s*\{/, `${page}: missing onShow refresh`)
  }
})

test('identity-scoped pages never rely on onLoad as their only refresh hook', () => {
  for (const [page, metadata] of Object.entries(manifest.pages)) {
    if (!metadata.refresh || !['family', 'merchant'].includes(metadata.tenantScope)) continue
    const source = readPageScript(page)
    assert.match(source, /\bonShow\s*\([^)]*\)\s*\{/, `${page}: stale after identity or role switch`)
  }
})
