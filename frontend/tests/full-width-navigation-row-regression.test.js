const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('full-width navigation rows do not use the native WeChat button box', () => {
  const home = read('pages/family/home/index.wxml')
  const profile = read('pages/account/profile/index.wxml')
  const groupMenu = read('components/group-menu/index.wxml')

  assert.doesNotMatch(home, /<button[^>]*class="[^"]*home-service-row/)
  assert.match(home, /<view[^>]*class="[^"]*home-service-row/)
  assert.doesNotMatch(profile, /<button[^>]*class="[^"]*profile-row/)
  assert.match(profile, /<view[^>]*class="[^"]*profile-row/)
  assert.doesNotMatch(groupMenu, /<button[^>]*class="[^"]*group-menu__item/)
  assert.match(groupMenu, /<view[^>]*class="[^"]*group-menu__item/)
})

test('replacement navigation rows retain button semantics and press feedback', () => {
  const home = read('pages/family/home/index.wxml')
  const profile = read('pages/account/profile/index.wxml')
  const groupMenu = read('components/group-menu/index.wxml')

  for (const source of [home, profile, groupMenu]) {
    assert.match(source, /aria-role="button"/)
    assert.match(source, /hover-class="[^"]+"/)
  }

  assert.match(groupMenu, /aria-disabled="\{\{entry\.disabled\}\}"/)
})

test('profile identity switch uses a compact custom tap target instead of a native button', () => {
  const profile = read('pages/account/profile/index.wxml')
  const styles = read('pages/account/profile/index.wxss')

  assert.doesNotMatch(profile, /<button[^>]*class="profile-switch-link"/)
  assert.match(profile, /<view[^>]*class="profile-switch-link"[^>]*aria-role="button"[^>]*hover-class="profile-switch-link--pressed"/)
  assert.match(styles, /\.profile-switch-link\{[^}]*display:inline-flex[^}]*justify-self:end[^}]*width:auto[^}]*white-space:nowrap/)
})
