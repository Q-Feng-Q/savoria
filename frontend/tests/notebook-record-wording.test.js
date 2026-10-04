const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const notebookPages = path.resolve(__dirname, '../pages/notebook')

function wxmlFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(directory, entry.name)
    return entry.isDirectory() ? wxmlFiles(file) : file.endsWith('.wxml') ? [file] : []
  })
}

test('notebook uses record wording instead of accounting measure words', () => {
  const home = fs.readFileSync(path.join(notebookPages, 'home/index.wxml'), 'utf8')
  assert.match(home, /\{\{group\.records\.length\}\}\s*条记录/)
  assert.match(home, /＋ 新增记录/)

  for (const file of wxmlFiles(notebookPages)) {
    assert.doesNotMatch(fs.readFileSync(file, 'utf8'), /笔/, file)
  }
})
