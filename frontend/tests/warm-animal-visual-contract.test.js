const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

test('warm animal theme exposes the new semantic palette', () => {
  const tokens = read('styles/tokens.wxss')

  assert.match(tokens, /--sk-cream:\s*#fff4e4/i)
  assert.match(tokens, /--sk-chestnut:\s*#8a5b43/i)
  assert.match(tokens, /--sk-sage:\s*#77977d/i)
  assert.match(tokens, /--sk-peach:\s*#e98262/i)
})

test('home hero uses the generated warm animal kitchen artwork', () => {
  const wxml = read('pages/family/home/index.wxml')
  const wxss = read('pages/family/home/index.wxss')

  assert.match(wxml, /animal-kitchen-hero\.webp/)
  assert.match(wxml, /小熊主厨/)
  assert.match(wxml, /兔子帮厨/)
  assert.match(wxml, /橘猫试吃员/)
  assert.match(wxss, /animal-breathe/)
})

test('shared brand and account surfaces use the mascot trio', () => {
  const brandScene = read('components/brand-scene/index.wxml')
  const profile = read('pages/account/profile/index.wxml')
  const menu = read('pages/ordering/menu/index.wxml')

  assert.match(brandScene, /animal-mascot-trio\.webp/)
  assert.match(profile, /animal-mascot-trio\.webp/)
  assert.match(menu, /兔子帮厨/)
})

test('animal assets are never loaded from WXSS', () => {
  const wxssFiles = []
  const visit = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name)
      if (entry.isDirectory()) visit(full)
      else if (entry.name.endsWith('.wxss')) wxssFiles.push(full)
    }
  }
  visit(root)

  for (const file of wxssFiles) {
    assert.doesNotMatch(fs.readFileSync(file, 'utf8'), /url\([^)]*animal-(?:kitchen|mascot)/)
  }
})
