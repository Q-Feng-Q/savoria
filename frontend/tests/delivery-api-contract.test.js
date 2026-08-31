const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const manifest = require('./fixtures/delivery-audit-manifest')
const contractsPath = path.join(__dirname, 'fixtures', 'delivery-service-contracts.json')

const factories = {
  auth: require('../services/auth').createAuthService,
  cart: require('../services/cart').createCartService,
  family: require('../services/family').createFamilyService,
  files: require('../services/files').createFilesService,
  merchant: require('../services/merchant').createMerchantService,
  notifications: require('../services/notifications').createNotificationsService,
  orders: require('../services/orders').createOrdersService,
  purchase: require('../services/purchase').createPurchaseService,
  system: require('../services/system').createSystemService,
  user: require('../services/user').createUserService
}

const buildService = (factory, captured) => factory({
  baseUrl: 'http://127.0.0.1:8080',
  getSession: () => ({ accessToken: 'contract-token', userId: 2, merchantId: 3 }),
  request: async (pathname, options) => {
    captured.push({ kind: 'request', pathname, options })
    return { code: 0, data: { contract: true } }
  },
  upload: async (options) => {
    captured.push({ kind: 'upload', options })
    return { statusCode: 200, data: JSON.stringify({ code: 0, data: { url: '/uploads/contract.png' } }) }
  }
})

const publicOperationKeys = () => Object.entries(factories).flatMap(([serviceName, factory]) => {
  const service = buildService(factory, [])
  return Object.keys(service).map((functionName) => `${serviceName}.${functionName}`)
}).sort()

test('service contract fixture exists', () => {
  assert.ok(fs.existsSync(contractsPath), 'create tests/fixtures/delivery-service-contracts.json')
})

if (fs.existsSync(contractsPath)) {
  const contracts = JSON.parse(fs.readFileSync(contractsPath, 'utf8'))
  const contractByKey = Object.fromEntries(contracts.map((contract) => [contract.key, contract]))

  test('every public service function has exactly one HTTP contract', () => {
    assert.equal(new Set(contracts.map(({ key }) => key)).size, contracts.length)
    assert.deepEqual(contracts.map(({ key }) => key).sort(), publicOperationKeys())
  })

  test('manifest operations expose controller and authorization metadata', () => {
    assert.deepEqual(Object.keys(manifest.operations).sort(), publicOperationKeys())
    for (const key of publicOperationKeys()) {
      const contract = contractByKey[key]
      assert.equal(contract.service, key.split('.')[0], `${key}: service`)
      assert.equal(contract.function, key.split('.')[1], `${key}: function`)
      assert.ok(['GET', 'POST', 'PUT', 'DELETE'].includes(contract.method), `${key}: method`)
      assert.match(contract.route, /^\/api\//, `${key}: route`)
      assert.equal(typeof contract.controllerClass, 'string', `${key}: controllerClass`)
      assert.equal(typeof contract.controllerMethod, 'string', `${key}: controllerMethod`)
      assert.ok(['public', 'account', 'family', 'merchant'].includes(contract.scope), `${key}: scope`)
      assert.ok(Array.isArray(contract.args), `${key}: args`)
    }
  })

  for (const contract of contracts) {
    test(`service contract ${contract.key}`, async () => {
      const captured = []
      const service = buildService(factories[contract.service], captured)
      const result = await service[contract.function](...contract.args)
      assert.equal(captured.length, 1)
      const call = captured[0]
      assert.equal(call.kind, contract.kind || 'request')
      if (call.kind === 'request') {
        assert.equal(call.pathname, contract.path)
        assert.equal(call.options.method, contract.method)
        if (Object.hasOwn(contract, 'data')) assert.deepEqual(call.options.data, contract.data)
        else assert.equal(Object.hasOwn(call.options, 'data'), false)
        assert.deepEqual(result, { contract: true })
      } else {
        assert.equal(call.options.url, `http://127.0.0.1:8080${contract.path}`)
        assert.equal(call.options.filePath, contract.args[0])
        assert.equal(result.url, '/uploads/contract.png')
      }
    })
  }
}
