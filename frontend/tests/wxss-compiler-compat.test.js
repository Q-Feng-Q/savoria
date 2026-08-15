const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')

const listWxss = (directory) => fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
  const fullPath = path.join(directory, entry.name)
  if (entry.isDirectory()) return listWxss(fullPath)
  return entry.name.endsWith('.wxss') ? [fullPath] : []
})

test('global WXSS avoids selectors unsupported by the WeChat compiler', () => {
  const foundation = fs.readFileSync(path.join(root, 'styles/warm-kitchen-foundation.wxss'), 'utf8')

  assert.doesNotMatch(foundation, /@media\s*\(prefers-reduced-motion:/)
  assert.doesNotMatch(foundation, /\*::(?:before|after)/)
})

test('production WXSS does not use universal selectors', () => {
  for (const file of listWxss(root)) {
    const source = fs.readFileSync(file, 'utf8')
    assert.doesNotMatch(source, /(?:^|,|\s)\*(?=\s*[,.:{])|[.#\w-]+\s+\*(?=\s*[,.:{])/, path.relative(root, file))
  }
})

test('profile page uses class-only selectors accepted by the WeChat component compiler', () => {
  const styles = fs.readFileSync(path.join(root, 'pages/account/profile/index.wxss'), 'utf8')
  const template = fs.readFileSync(path.join(root, 'pages/account/profile/index.wxml'), 'utf8')

  assert.doesNotMatch(styles, /\s+text(?=[:\s,{])/)
  assert.match(template, /class="warm-profile-portrait__welcome-kicker"/)
  assert.match(template, /class="warm-profile-portrait__welcome-title"/)
  assert.match(template, /class="profile-family-summary__name"/)
  assert.match(template, /class="profile-family-summary__meta"/)
})
