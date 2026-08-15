const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')
const merchantPages = [
  'index', 'merchant-orders', 'merchant-order-detail', 'purchase', 'merchant-dishes',
  'dish-edit', 'ingredient-edit', 'merchant-families', 'merchant-family-detail', 'family-menu'
]
const pageBase = (page) => page === 'index' ? 'pages/merchant/index' : `pages/merchant/${page}/index`

test('all ten merchant pages start loading and expose retryable page loading and error states', () => {
  assert.equal(merchantPages.length, 10)
  for (const page of merchantPages) {
    const source = read(`${pageBase(page)}.js`)
    const markup = read(`${pageBase(page)}.wxml`)
    assert.match(source, /data:\s*\{[\s\S]*?phase:\s*'loading'/, `${page}: initial phase`)
    assert.match(source, /retryLoad\s*\(\)\s*\{\s*return this\.load\(\)/, `${page}: retry delegates to load`)
    assert.match(markup, /<page-state[^>]+phase="\{\{phase\}\}"[^>]+bind:retry="retryLoad"/, `${page}: shared page state`)
    assert.match(markup, /phase\s*(?:===|!==)[^>]*loading|phase="\{\{phase\}\}"/, `${page}: loading rendering`)
    assert.match(source, /phase:\s*'error'/, `${page}: error transition`)
  }
})

test('merchant list pages distinguish a successful empty result from errors', () => {
  for (const page of ['merchant-orders', 'purchase', 'merchant-dishes', 'ingredient-edit', 'merchant-families', 'merchant-family-detail', 'family-menu']) {
    const source = read(`${pageBase(page)}.js`)
    const markup = read(`${pageBase(page)}.wxml`)
    assert.match(source, /phase:\s*[^\n;]*'empty'/, `${page}: successful empty phase`)
    assert.match(markup, /phase="empty"|phase === 'empty'|phase="\{\{phase\}\}"|!\w+\.length/, `${page}: empty UI`)
  }
})

test('detail and form pages handle missing identifiers or records intentionally', () => {
  const orderDetail = read('pages/merchant/merchant-order-detail/index.js')
  const dishEdit = read('pages/merchant/dish-edit/index.js')
  const ingredientEdit = read('pages/merchant/ingredient-edit/index.js')
  assert.match(orderDetail, /if\s*\(!this\.data\.id\)[\s\S]*?phase:\s*'error'/)
  assert.match(dishEdit, /this\.data\.id\s*\?\s*runtime\.merchant\.getDishDetail/)
  assert.match(dishEdit, /if\s*\(this\.data\.id\s*&&\s*!detail\)\s*throw/)
  assert.match(dishEdit, /this\.data\.id[\s\S]*?updateDish[\s\S]*?createDish/)
  assert.match(ingredientEdit, /editId[\s\S]*?updateIngredient[\s\S]*?createIngredient/)
})

test('merchant mutations expose local busy guards instead of replacing page state', () => {
  const expectations = {
    'merchant-orders': /busyOrderMap/,
    'merchant-order-detail': /mutationBusy/,
    purchase: /purchaseBusy[\s\S]*busyItemMap/,
    'merchant-dishes': /busyDishMap/,
    'dish-edit': /saving[\s\S]*uploading/,
    'ingredient-edit': /saving[\s\S]*busyIngredientId/,
    'merchant-family-detail': /deliveryBusy[\s\S]*busyMemberId/,
    'family-menu': /saving[\s\S]*rowBusyMap/
  }
  for (const [page, pattern] of Object.entries(expectations)) assert.match(read(`${pageBase(page)}.js`), pattern, page)
})

test('every merchant load is protected by the shared merchant session guard', () => {
  for (const page of merchantPages) {
    const source = read(`${pageBase(page)}.js`)
    assert.match(source, /requireSession\(\{\s*merchantOnly:\s*true\s*\}\)/, page)
    assert.match(source, /if\s*\(!session\)\s*return/, `${page}: unauthorized exits before empty state`)
  }
  const guard = read('utils/page-api.js')
  assert.match(guard, /options\.merchantOnly[\s\S]*?redirectTo/)
})

test('missing merchant family detail becomes a successful empty state without optional reads', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-family-detail', 'index.js')
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'))
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'))
  const previousPage = global.Page
  const previousWx = global.wx
  const previousRuntime = require.cache[runtimePath]
  const previousPageApi = require.cache[pageApiPath]
  let definition
  let optionalReads = 0
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant: {
    getFamilyDetail: async () => null,
    getFamilyMenu: async () => { optionalReads += 1; return [] },
    getOrders: async () => { optionalReads += 1; return [] },
    getMemberWalletLedgers: async () => { optionalReads += 1; return [] }
  } }) } }
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage: (error, fallback) => (error && error.message) || fallback
  } }
  global.Page = (value) => { definition = value }
  global.wx = {}
  try {
    delete require.cache[require.resolve(pagePath)]
    require(pagePath)
    const page = Object.assign({}, definition, {
      data: JSON.parse(JSON.stringify(definition.data)),
      setData(update) { this.data = { ...this.data, ...update } }
    })
    page.setData({ familyId: '404' })
    await page.load()
    assert.equal(page.data.phase, 'empty')
    assert.match(page.data.pageTitle, /未找到|不存在/)
    assert.ok(page.data.pageDescription)
    assert.equal(page.data.family, null)
    assert.equal(optionalReads, 0)
  } finally {
    delete require.cache[require.resolve(pagePath)]
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath]
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath]
    global.Page = previousPage
    global.wx = previousWx
  }
})

test('merchant mutation refreshes opt into silent loading on every affected page', () => {
  const expectedSilentRefreshes = {
    'merchant-orders': 1,
    'merchant-order-detail': 1,
    purchase: 1,
    'merchant-dishes': 1,
    'ingredient-edit': 2,
    'merchant-family-detail': 2,
    'family-menu': 2
  }
  for (const [page, count] of Object.entries(expectedSilentRefreshes)) {
    const source = read(`${pageBase(page)}.js`)
    assert.match(source, /async load\(\{ silent = false \} = \{\}\)/, `${page}: load accepts explicit silent mode`)
    assert.match(source, /if \(!silent\) this\.setData\(\{ phase: 'loading'/, `${page}: initial load still owns global loading`)
    assert.equal((source.match(/await this\.load\(\{ silent: true \}\)/g) || []).length, count, `${page}: mutation refresh count`)
  }
})

test('order detail keeps ready content and local busy state while mutation refresh is pending', async () => {
  const pagePath = path.join(root, 'pages', 'merchant', 'merchant-order-detail', 'index.js')
  const runtimePath = require.resolve(path.join(root, 'utils', 'api-runtime.js'))
  const pageApiPath = require.resolve(path.join(root, 'utils', 'page-api.js'))
  const previousPage = global.Page
  const previousWx = global.wx
  const previousRuntime = require.cache[runtimePath]
  const previousPageApi = require.cache[pageApiPath]
  let definition
  let resolveRefresh
  const refresh = new Promise((resolve) => { resolveRefresh = resolve })
  require.cache[runtimePath] = { exports: { createApiRuntime: () => ({ merchant: {
    updateDeliveryFee: async () => {},
    getOrderDetail: () => refresh,
    getFamilyDetail: async () => null
  } }) } }
  require.cache[pageApiPath] = { exports: {
    requireSession: () => ({ merchantId: 3 }),
    resolveApiErrorMessage: (error, fallback) => (error && error.message) || fallback
  } }
  global.Page = (value) => { definition = value }
  global.wx = { showToast() {} }
  try {
    delete require.cache[require.resolve(pagePath)]
    require(pagePath)
    const page = Object.assign({}, definition, {
      data: { ...definition.data, id: '9', phase: 'ready', scene: { order: { rawStatus: 'PENDING' } } },
      setData(update) { this.data = { ...this.data, ...update } }
    })
    const mutation = page.saveFee()
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(page.data.phase, 'ready')
    assert.equal(page.data.mutationBusy, true)
    assert.ok(page.data.scene)
    resolveRefresh(null)
    await mutation
  } finally {
    delete require.cache[require.resolve(pagePath)]
    if (previousRuntime) require.cache[runtimePath] = previousRuntime; else delete require.cache[runtimePath]
    if (previousPageApi) require.cache[pageApiPath] = previousPageApi; else delete require.cache[pageApiPath]
    global.Page = previousPage
    global.wx = previousWx
  }
})

test('list mutations lock only the affected merchant row', () => {
  const expectations = {
    'merchant-orders': {
      guard: /if\s*\(!orderId\s*\|\|\s*this\.data\.busyOrderMap\[orderId\]\)\s*return/,
      markup: /aria-disabled="\{\{busyOrderMap\[item\.id\]\}\}"[^>]*aria-busy="\{\{busyOrderMap\[item\.id\]\}\}"/
    },
    'merchant-dishes': {
      guard: /if\s*\(!dishId\s*\|\|\s*this\.data\.busyDishMap\[dishId\]\)\s*return/,
      markup: /aria-disabled="\{\{busyDishMap\[item\.id\]\}\}"[^>]*aria-busy="\{\{busyDishMap\[item\.id\]\}\}"/
    },
    purchase: {
      guard: /if\s*\(itemId\s*!==\s*null\s*&&\s*this\.data\.busyItemMap\[itemId\]\)\s*return/,
      markup: /aria-disabled="\{\{busyItemMap\[item\.itemId\]\}\}"[^>]*aria-busy="\{\{busyItemMap\[item\.itemId\]\}\}"/
    },
    'family-menu': {
      guard: /if\s*\(rowBusyId\s*!==\s*null\s*&&\s*this\.data\.rowBusyMap\[rowBusyId\]\)\s*return/,
      markup: /aria-disabled="\{\{rowBusyMap\[item\.id\]\}\}"[^>]*aria-busy="\{\{rowBusyMap\[item\.id\]\}\}"/
    }
  }
  for (const [page, expected] of Object.entries(expectations)) {
    assert.match(read(`${pageBase(page)}.js`), expected.guard, `${page}: row-local guard`)
    assert.match(read(`${pageBase(page)}.wxml`), expected.markup, `${page}: row-local semantics`)
  }
})
