const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const merchantRoots = [
  'merchant-page', 'orders-page', 'detail-page', 'purchase-page', 'dishes-page',
  'editor-page', 'ingredient-page', 'families-page', 'family-detail-page', 'family-menu-page'
]

test('shared merchant shell is fluid, phone bounded, and overflow safe', () => {
  const css = read('styles/warm-kitchen-responsive.wxss')
  for (const rootClass of merchantRoots) assert.match(css, new RegExp(`\\.${rootClass}\\b`), rootClass)
  assert.match(css, /\.merchant-page[\s\S]*?width:\s*100%[\s\S]*?max-width:\s*720px/)
  assert.match(css, /overflow-x:\s*hidden/)
  assert.match(css, /overflow-wrap:\s*anywhere/)
  assert.match(css, /min-width:\s*0/)
})

test('merchant breakpoints cover compact, large phone, wide phone, and landscape', () => {
  const css = read('styles/warm-kitchen-responsive.wxss')
  assert.match(css, /@media\s*\(max-width:\s*359px\)/)
  assert.match(css, /@media\s*\(min-width:\s*420px\)/)
  assert.match(css, /@media\s*\(min-width:\s*480px\)/)
  assert.match(css, /@media\s*\(orientation:\s*landscape\)/)
  assert.match(css, /grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  assert.doesNotMatch(css, /max-width:\s*\d+rpx/)
})

test('ingredient editor uses a real two-column workspace at wide and landscape sizes', () => {
  const markup = read('pages/merchant/ingredient-edit/index.wxml')
  const css = read('styles/warm-kitchen-responsive.wxss')
  assert.match(markup, /<view wx:else class="ingredient-workspace">[\s\S]*?class="form-section"[\s\S]*?class="form-section editor-section"/)
  const wide = css.slice(css.search(/@media\s*\(min-width:\s*480px\)/), css.search(/@media\s*\(max-height:/))
  const landscape = css.slice(css.search(/@media\s*\(orientation:\s*landscape\)/), css.search(/\.warm-page, \.page, text/))
  for (const rules of [wide, landscape]) {
    assert.match(rules, /\.ingredient-workspace\s*\{[^}]*display:\s*grid[^}]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/)
  }
})

test('all merchant root navigation mounts after the page content so its spacer stays last', () => {
  const roots = {
    index: 'overview',
    'merchant-orders': 'orders',
    'merchant-dishes': 'dishes',
    'merchant-families': 'families'
  }
  for (const [page, active] of Object.entries(roots)) {
    const file = page === 'index' ? 'pages/merchant/index.wxml' : `pages/merchant/${page}/index.wxml`
    const markup = read(file).trim()
    assert.match(markup, new RegExp(`</view>\\s*<merchant-workbench-nav\\s+active="${active}"\\s*/>$`), page)
  }
})

test('merchant fixed actions and navigation reserve safe area without shrinking controls', () => {
  const responsive = read('styles/warm-kitchen-responsive.wxss')
  const foundation = read('styles/warm-kitchen-foundation.wxss')
  const nav = read('components/merchant-workbench-nav/index.wxss')
  assert.match(responsive, /safe-action-bar[\s\S]*?env\(safe-area-inset-bottom\)/)
  assert.match(nav, /merchant-workbench-nav__spacer[\s\S]*?env\(safe-area-inset-bottom\)/)
  assert.match(nav, /merchant-workbench-nav__bar[\s\S]*?env\(safe-area-inset-bottom\)/)
  assert.match(foundation, /\.ui-button[\s\S]*?min-height:\s*88rpx/)
  for (const page of ['dish-edit', 'ingredient-edit', 'merchant-order-detail']) {
    const styles = read(`pages/merchant/${page}/index.wxss`)
    const rule = styles.match(/\.safe-action-bar\s*\{([^}]*)\}/)
    assert.ok(rule, `${page}: fixed action rule`)
    for (const property of [/width:\s*100%/, /max-width:\s*720px/, /left:\s*50%/, /right:\s*auto/, /translateX\(-50%\)/]) {
      assert.match(rule[1], property, page)
    }
    const pageRule = styles.match(/\.(?:editor|ingredient|detail)-page\s*\{([^}]*)\}/)
    assert.ok(pageRule, `${page}: page reserve rule`)
    assert.match(pageRule[1], /padding-bottom:\s*calc\([^)]*env\(safe-area-inset-bottom\)/, page)
  }
  assert.match(nav, /merchant-workbench-nav__inner[^}]*max-width:\s*720px[^}]*margin:\s*0 auto/)
})

test('compact merchant rules never hide business or status imagery', () => {
  const css = read('styles/warm-kitchen-responsive.wxss')
  const compactStart = css.search(/@media\s*\(max-width:\s*359px\)/)
  const compactEnd = css.indexOf('@media', compactStart + 1)
  assert.notEqual(compactStart, -1, 'compact merchant media query is required')
  const compact = css.slice(compactStart, compactEnd)
  const hiddenSelectors = [...compact.matchAll(/([^{}]+)\{[^{}]*display:\s*none[^{}]*\}/g)]
    .map((match) => match[1]).filter((selector) => selector.includes('.merchant-'))
  for (const selector of hiddenSelectors) {
    assert.match(selector, /\.merchant-[\w-]+-decorative\b/, selector)
    assert.doesNotMatch(selector, /status|business|purchase-art|dish-thumb/)
  }
})

test('compact overview hides the real decorative vignette and stacks support stats', () => {
  const markup = read('pages/merchant/index.wxml')
  const css = read('styles/warm-kitchen-responsive.wxss')
  assert.match(markup, /merchant-scene-piece--decorative/)
  const compactStart = css.search(/@media\s*\(max-width:\s*359px\)/)
  const compactEnd = css.indexOf('@media', compactStart + 1)
  const compact = css.slice(compactStart, compactEnd)
  assert.match(compact, /\.merchant-scene-piece--decorative\s*\{[^}]*display:\s*none/)
  assert.match(compact, /\.merchant-helper-stats\s*\{[^}]*grid-template-columns:\s*1fr/)
})

test('dish names and descriptions wrap instead of truncating business text', () => {
  const css = read('pages/merchant/merchant-dishes/index.wxss')
  for (const selector of ['dish-name', 'dish-description']) {
    const rule = css.match(new RegExp(`\\.${selector}\\s*\\{([^}]*)\\}`))
    assert.ok(rule, selector)
    assert.doesNotMatch(rule[1], /white-space:\s*nowrap|text-overflow:\s*ellipsis|overflow:\s*hidden/, selector)
    assert.match(rule[1], /overflow-wrap:\s*anywhere/, selector)
  }
})

test('empty-state animal has no infinite decorative motion', () => {
  const css = read('components/page-state/index.wxss')
  assert.doesNotMatch(css, /\.page-state__animal\s*\{[^}]*animation:/)
  assert.doesNotMatch(css, /@keyframes\s+animal-state-float/)
})

test('system dish import actions remain accessible and responsive across merchant sizes', () => {
  const dishes = read('pages/merchant/merchant-dishes/index.wxss')
  const templates = read('pages/merchant/dish-templates/index.wxss')
  assert.match(dishes, /\.page-actions\s*\{[^}]*display:\s*flex[^}]*flex-wrap:\s*wrap/)
  assert.match(dishes, /\.(?:primary-action|secondary-action)[^\{]*\{[^}]*min-height:\s*88rpx/)
  assert.match(dishes, /\.secondary-action\s*\{[^}]*min-height:\s*88rpx/)
  const compact = dishes.match(/@media\s*\(max-width:\s*320px\)\s*\{([\s\S]*)\}\s*$/)
  assert.ok(compact, 'compact dish actions breakpoint')
  assert.match(compact[1], /\.page-actions\s*\{[^}]*width:\s*100%[^}]*flex-direction:\s*column/)
  assert.match(compact[1], /\.(?:primary-action|secondary-action)[^\{]*\{[^}]*width:\s*100%/)
  assert.match(dishes, /@media\s*\(orientation:\s*landscape\)[\s\S]*?\.page-actions\s*\{[^}]*flex-wrap:\s*nowrap/)
  assert.match(templates, /\.import-all-action\s*\{[^}]*min-height:\s*88rpx/)
  assert.match(templates, /\.selection-bar\s*\{[^}]*env\(safe-area-inset-bottom\)/)
  assert.match(templates, /\.template-page\s*\{[^}]*env\(safe-area-inset-bottom\)/)
})
