const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..')
const manifestPath = path.join(__dirname, 'fixtures', 'delivery-audit-manifest.js')
const baselinePath = path.join(__dirname, 'fixtures', 'delivery-baseline.json')

test('delivery audit manifest exists before coverage checks run', () => {
  assert.ok(fs.existsSync(manifestPath), 'create tests/fixtures/delivery-audit-manifest.js')
})

test('delivery baseline records exact frontend and backend test identities', () => {
  assert.ok(fs.existsSync(baselinePath), 'create tests/fixtures/delivery-baseline.json')
  if (!fs.existsSync(baselinePath)) return
  const baseline = JSON.parse(fs.readFileSync(baselinePath, 'utf8'))
  assert.equal(baseline.frontend.length, 348)
  assert.equal(baseline.backend.length, 285)
  assert.equal(baseline.backend.filter(({ status }) => status === 'skipped').length, 12)
  assert.equal(new Set(baseline.frontend.map(({ id }) => id)).size, 348)
  assert.equal(new Set(baseline.backend.map(({ id }) => id)).size, 285)
})

if (fs.existsSync(manifestPath)) {
  const manifest = require(manifestPath)

  const normalize = (values) => [...values].sort()
  const registeredPages = JSON.parse(
    fs.readFileSync(path.join(frontendRoot, 'app.json'), 'utf8')
  ).pages
  const implementedPages = []

  const visit = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const absolute = path.join(directory, entry.name)
      if (entry.isDirectory()) visit(absolute)
      if (entry.isFile() && entry.name === 'index.js') {
        implementedPages.push(path.relative(frontendRoot, absolute).replace(/\\/g, '/').replace(/\.js$/, ''))
      }
    }
  }
  visit(path.join(frontendRoot, 'pages'))

  test('delivery manifest covers every registered and implemented page', () => {
    const manifestPages = Object.keys(manifest.pages)
    assert.deepEqual(normalize(manifestPages), normalize(registeredPages))
    assert.deepEqual(normalize(implementedPages), normalize(registeredPages))
  })

  test('every registered page has a complete mini program file quartet', () => {
    for (const page of registeredPages) {
      for (const extension of ['.js', '.json', '.wxml', '.wxss']) {
        assert.ok(fs.existsSync(path.join(frontendRoot, `${page}${extension}`)), `${page}${extension}`)
      }
    }
  })

  test('delivery manifest covers every service module', () => {
    const serviceFiles = fs.readdirSync(path.join(frontendRoot, 'services'))
      .filter((file) => file.endsWith('.js'))
      .sort()
    assert.deepEqual(serviceFiles, normalize(manifest.serviceModules))
  })

  test('every page declares audit behavior and operation metadata', () => {
    const allowedScopes = new Set(['public', 'account', 'family', 'merchant'])
    for (const [page, metadata] of Object.entries(manifest.pages)) {
      assert.equal(typeof metadata.domain, 'string', `${page}: domain`)
      for (const key of ['async', 'list', 'form', 'refresh']) {
        assert.equal(typeof metadata[key], 'boolean', `${page}: ${key}`)
      }
      assert.ok(allowedScopes.has(metadata.tenantScope), `${page}: tenantScope`)
      assert.ok(Array.isArray(metadata.operations), `${page}: operations`)
      for (const operation of metadata.operations) {
        assert.ok(manifest.operations[operation], `${page}: unknown operation ${operation}`)
      }
    }
  })

  test('backend mini program route boundary is explicit', () => {
    assert.ok(Array.isArray(manifest.backendRouteBoundary.includePrefixes))
    assert.ok(manifest.backendRouteBoundary.includePrefixes.includes('/api/family'))
    assert.ok(manifest.backendRouteBoundary.includePrefixes.includes('/api/merchant'))
    assert.ok(Array.isArray(manifest.backendRouteBoundary.exclusions))
    assert.ok(manifest.backendRouteBoundary.exclusions.some(({ route }) => route === '/api/admin/**'))
    for (const exclusion of manifest.backendRouteBoundary.exclusions) {
      assert.ok(exclusion.reason && exclusion.reason.trim(), `${exclusion.route}: exclusion reason`)
    }
  })
}
