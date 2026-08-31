const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..')
const repositoryRoot = path.resolve(frontendRoot, '..')
const outputPath = path.join(frontendRoot, 'tests', 'fixtures', 'delivery-service-contracts.json')
const backendOutputPath = path.join(repositoryRoot, 'backend', 'src', 'test', 'resources', 'contracts', 'mini-program-api-contract.json')
const pageOperations = require('../tests/fixtures/delivery-page-operations.json')

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

const decodeXml = (value) => value
  .replaceAll('&quot;', '"')
  .replaceAll('&amp;', '&')
  .replaceAll('&lt;', '<')
  .replaceAll('&gt;', '>')

const controllerIndex = () => {
  const controllerRoot = path.join(repositoryRoot, 'backend', 'src', 'main', 'java')
  const files = []
  const visit = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const absolute = path.join(directory, entry.name)
      if (entry.isDirectory()) visit(absolute)
      else if (entry.name.endsWith('Controller.java')) files.push(absolute)
    }
  }
  visit(controllerRoot)

  const routes = []
  for (const file of files) {
    const source = fs.readFileSync(file, 'utf8')
    const className = source.match(/public\s+class\s+(\w+)/)?.[1]
    const packageName = source.match(/package\s+([\w.]+);/)?.[1]
    const classPrefix = source.slice(0, source.indexOf(`public class ${className}`))
      .match(/@RequestMapping\(\s*"([^"]*)"\s*\)/)?.[1] || ''
    const lines = source.split(/\r?\n/)
    let pending = null
    for (const line of lines) {
      const mapping = line.match(/@(Get|Post|Put|Delete)Mapping(?:\(([^)]*)\))?/)
      if (mapping) {
        const suffix = mapping[2]?.match(/(?:^|\b(?:path|value)\s*=\s*)"([^"]*)"/)?.[1] || ''
        pending = { method: mapping[1].toUpperCase(), suffix }
      }
      if (!pending || !line.includes('public ')) continue
      const methodName = line.match(/\bpublic\s+[^=;{]+?\s+(\w+)\s*\(/)?.[1]
      if (!methodName) continue
      const route = `${classPrefix}${pending.suffix}`.replace(/\/+/g, '/') || '/'
      routes.push({ ...pending, route, controllerClass: className,
        controllerFqcn: `${packageName}.${className}`, controllerMethod: methodName })
      pending = null
    }
  }
  return routes
}

const parameterNames = (fn) => {
  const source = fn.toString().trim()
  const parenthesized = source.match(/^[^(]*\(([^)]*)\)/)
  const singleArrow = source.match(/^(?:async\s+)?([a-zA-Z_$][\w$]*)\s*=>/)
  const raw = parenthesized ? parenthesized[1] : (singleArrow ? singleArrow[1] : '')
  return raw.split(',').map((name) => name.split('=')[0].trim()).filter(Boolean)
}

const argumentFor = (name, index) => {
  const clean = name.replace(/^_/, '')
  if (/filePath/i.test(clean)) return 'D:/contract/image.png'
  if (/templateIds/i.test(clean)) return [901, 902]
  if (/featured/i.test(clean)) return true
  if (/(^id$|Id$)/.test(clean)) return 901 + index
  if (/^(params|query)$/.test(clean)) {
    return { page: 2, pageSize: 3, keyword: '番茄 牛腩', date: '2026-08-31', includePending: true }
  }
  if (/^(payload|data)$/.test(clean)) return { contractField: 'contract-value' }
  return `contract-${clean || index}`
}

const normalizeRoute = (pathname, names, args) => {
  let route = pathname.split('?')[0]
  names.forEach((name, index) => {
    const clean = name.replace(/^_/, '')
    if (/(^id$|Id$)/.test(clean)) route = route.replace(`/${args[index]}`, `/{${clean}}`)
  })
  return route
}

const scopeFor = (pathname) => {
  if (pathname.startsWith('/api/merchant')) return 'merchant'
  if (pathname.startsWith('/api/family')) return 'family'
  if (pathname.startsWith('/api/auth') || pathname.startsWith('/api/public')) return 'public'
  return 'account'
}

async function main() {
const routes = controllerIndex()
const contracts = []

for (const [serviceName, factory] of Object.entries(factories)) {
  const captured = []
  const service = factory({
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

  for (const [functionName, fn] of Object.entries(service)) {
    captured.length = 0
    const names = parameterNames(fn)
    const args = names.map(argumentFor)
    await fn.apply(service, args)
    if (captured.length !== 1) throw new Error(`${serviceName}.${functionName} captured ${captured.length} calls`)
    const call = captured[0]
    const pathname = call.kind === 'request'
      ? call.pathname
      : new URL(call.options.url).pathname
    const route = normalizeRoute(pathname, names, args)
    const backendRoute = route.replace(/^\/api(?=\/)/, '')
    const method = call.kind === 'upload' ? 'POST' : call.options.method
    const handlers = routes.filter((candidate) => candidate.method === method && candidate.route === backendRoute)
    if (handlers.length !== 1) {
      throw new Error(`${serviceName}.${functionName}: ${method} ${backendRoute} matched ${handlers.length} controllers`)
    }
    const handler = handlers[0]
    contracts.push({
      key: `${serviceName}.${functionName}`,
      service: serviceName,
      function: functionName,
      kind: call.kind,
      args,
      method,
      path: pathname,
      route,
      backendRoute,
      controllerClass: handler.controllerClass,
      controllerFqcn: handler.controllerFqcn,
      controllerMethod: handler.controllerMethod,
      scope: scopeFor(route),
      ...(call.kind === 'request' && Object.hasOwn(call.options, 'data') ? { data: call.options.data } : {})
    })
  }
}

contracts.sort((left, right) => left.key.localeCompare(right.key))
fs.writeFileSync(outputPath, `${JSON.stringify(contracts, null, 2)}\n`)
const backendContracts = contracts.map((contract) => ({
  operation: contract.key,
  pages: Object.entries(pageOperations)
    .filter(([, operations]) => operations.includes(contract.key))
    .map(([page]) => page)
    .concat(Object.values(pageOperations).some((operations) => operations.includes(contract.key))
      ? [] : [`shared/runtime/${contract.service}`]),
  method: contract.method,
  backendRoute: contract.backendRoute,
  controllerFqcn: contract.controllerFqcn,
  controllerMethod: contract.controllerMethod
}))
fs.mkdirSync(path.dirname(backendOutputPath), { recursive: true })
fs.writeFileSync(backendOutputPath, `${JSON.stringify(backendContracts, null, 2)}\n`)
console.log(`wrote ${contracts.length} service contracts to ${path.relative(repositoryRoot, outputPath)}`)
console.log(`wrote ${backendContracts.length} backend contracts to ${path.relative(repositoryRoot, backendOutputPath)}`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
