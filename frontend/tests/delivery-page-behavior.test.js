const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const manifest = require('./fixtures/delivery-audit-manifest')
const root = path.resolve(__dirname, '..')

const readPageScript = (page) => fs.readFileSync(path.join(root, `${page}.js`), 'utf8')

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

