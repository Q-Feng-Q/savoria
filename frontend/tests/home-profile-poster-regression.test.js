const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('home poster keeps its copy in normal flow above a separately layered scene', () => {
  const wxml = read('pages/family/home/index.wxml')
  const wxss = read('pages/family/home/index.wxss')

  assert.match(wxml, /warm-home-poster/)
  assert.match(wxml, /warm-home-poster__copy/)
  assert.match(wxml, /warm-home-poster__scene/)
  assert.match(wxss, /\.warm-home-poster\s*\{[^}]*height:\s*620rpx/)
  assert.match(wxss, /\.warm-home-poster__copy\s*\{[^}]*position:\s*relative[^}]*z-index:\s*3/)
  assert.match(wxss, /\.warm-home-poster__scene\s*\{[^}]*z-index:\s*1/)
})

test('profile portrait remains visible for both bound and unbound accounts', () => {
  const wxml = read('pages/account/profile/index.wxml')
  const wxss = read('pages/account/profile/index.wxss')

  assert.match(wxml, /warm-profile-portrait/)
  assert.match(wxml, /warm-profile-portrait__welcome/)
  assert.match(wxml, /isUnbound \? '先认识一下新家'/)
  assert.match(wxss, /\.warm-profile-portrait\s*\{[^}]*min-height:\s*440rpx/)
  assert.match(wxss, /\.warm-profile-portrait__art\s*\{[^}]*z-index:\s*1/)
  assert.match(wxss, /\.warm-profile-portrait__identity\s*\{[^}]*z-index:\s*3/)
})
