const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('home poster keeps its copy in normal flow above a separately layered scene', () => {
  const wxml = read('pages/family/home/index.wxml')
  const wxss = read('pages/family/home/index.wxss') + read('styles/approved-storybook.wxss')

  assert.match(wxml, /story-brand__copy/)
  assert.match(wxml, /story-art__atlas/)
  assert.match(wxss, /\.story-brand\s*\{[^}]*height:\s*220rpx/)
  assert.match(wxss, /\.story-brand__copy\s*\{[^}]*position:\s*relative/)
  assert.match(wxss, /\.story-art\s*\{[^}]*position:\s*absolute[^}]*pointer-events:\s*none/)
})

test('profile portrait remains visible for both bound and unbound accounts', () => {
  const wxml = read('pages/account/profile/index.wxml')
  const wxss = read('pages/account/profile/index.wxss')

  assert.match(wxml, /warm-profile-portrait/)
  assert.match(wxml, /warm-profile-portrait__welcome/)
  assert.match(wxml, /isUnbound \? '从这里，认识新家'/)
  assert.match(wxml, /approved-story-atlas\.jpg/)
  assert.match(wxss, /\.profile-story-art\s*\{[^}]*height:\s*248rpx/)
  assert.match(wxss, /\.profile-banner__identity\s*\{[^}]*position:\s*relative/)
})
