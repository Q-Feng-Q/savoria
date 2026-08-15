const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const scenes = [
  'auth-welcome.webp', 'empty-cart.webp', 'empty-order.webp', 'network-error.webp',
  'merchant-cooking.webp', 'merchant-orders.webp', 'merchant-dishes.webp',
  'merchant-purchase.webp'
]

test('the modular warm animal scene set is available', () => {
  for (const scene of scenes) {
    const file = path.join(root, 'assets', 'brand', 'scenes', scene)
    assert.ok(fs.existsSync(file), `${scene} must exist`)
    assert.ok(fs.statSync(file).size > 1024, `${scene} must be a non-empty raster asset`)
  }
})

test('local assets are never referenced from WXSS', () => {
  const wxssFiles = []
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const target = path.join(dir, entry.name)
      if (entry.isDirectory()) walk(target)
      else if (entry.name.endsWith('.wxss')) wxssFiles.push(target)
    }
  }
  walk(root)
  for (const file of wxssFiles) {
    assert.doesNotMatch(fs.readFileSync(file, 'utf8'), /url\([^)]*assets\//, path.relative(root, file))
  }
})
