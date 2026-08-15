const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

test('new responsive system covers compact large landscape and safe areas', () => {
  const css = read('styles/warm-kitchen-responsive.wxss')
  assert.match(css, /max-width:\s*359px/)
  assert.match(css, /min-width:\s*420px/)
  assert.match(css, /orientation:\s*landscape/)
  assert.match(css, /safe-area-inset-bottom/)
  assert.match(css, /overflow-wrap:\s*anywhere/)
  assert.match(css, /max-height:/)
})

test('global shell prevents horizontal overflow and reserves fixed actions', () => {
  const css = read('styles/warm-kitchen-foundation.wxss')
  assert.match(css, /overflow-x:\s*hidden/)
  assert.match(css, /warm-fixed-action-space/)
  assert.match(css, /min-width:\s*0/)
  assert.match(css, /44px|88rpx/)
})
