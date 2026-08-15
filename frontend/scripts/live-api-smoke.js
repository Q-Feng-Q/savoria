const assert = require('node:assert/strict')

const baseUrl = String(process.env.KITCHEN_API_BASE || 'http://127.0.0.1:8080').replace(/\/+$/, '')
const adminUsername = process.env.KITCHEN_ADMIN_USERNAME || 'admin'
const adminPassword = process.env.KITCHEN_ADMIN_PASSWORD || '123456'

async function call(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
      ...(options.headers || {})
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  })
  const payload = await response.json()
  assert.equal(response.ok, true, `${options.method || 'GET'} ${path} returned HTTP ${response.status}`)
  assert.equal(payload.code, 0, `${options.method || 'GET'} ${path}: ${payload.message}`)
  return payload.data
}

async function main() {
  const suffix = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  const username = `smoke_${suffix}`
  const password = 'KitchenSmoke@2026'
  let userId
  let adminToken

  try {
    const settings = await call('/public/system-settings')
    assert.equal(settings.siteName, '食光知味')

    const registered = await call('/auth/register', {
      method: 'POST',
      body: { username, password, name: '接口巡检用户', mobile: '' }
    })
    userId = registered.userId
    assert.ok(registered.accessToken)

    const loggedIn = await call('/auth/login', {
      method: 'POST',
      body: { username, password }
    })
    assert.equal(loggedIn.userId, userId)

    const userToken = loggedIn.accessToken
    const headers = { 'X-User-Id': String(userId), 'X-Role-Template': 'member' }
    const context = await call('/users/me/context', { token: userToken, headers })
    assert.equal(context.userId, userId)
    await call('/users/me', { token: userToken, headers })
    await call('/family/onboarding', { token: userToken, headers })
    await call('/notifications?receiverScope=account&page=1&pageSize=5', { token: userToken, headers })

    const admin = await call('/auth/admin/login', {
      method: 'POST',
      body: { username: adminUsername, password: adminPassword }
    })
    adminToken = admin.accessToken
    const adminHeaders = {
      'X-User-Id': String(admin.userId),
      'X-Role-Template': admin.roleTemplate || 'platform_admin',
      'X-Backend-Roles': Array.from(admin.backendRoles || []).join(',')
    }
    const users = await call(`/admin/users?keyword=${encodeURIComponent(username)}`, {
      token: adminToken,
      headers: adminHeaders
    })
    assert.ok(users.some((user) => user.userId === userId), 'admin user list did not return smoke user')

    await call(`/admin/users/${userId}`, {
      method: 'DELETE',
      token: adminToken,
      headers: adminHeaders
    })
    userId = undefined

    console.log('LIVE_API_SMOKE_PASS public/auth/user/family-onboarding/notifications/admin-users')
  } finally {
    if (userId && adminToken) {
      try {
        const admin = await call('/auth/admin/login', {
          method: 'POST',
          body: { username: adminUsername, password: adminPassword }
        })
        await call(`/admin/users/${userId}`, {
          method: 'DELETE',
          token: admin.accessToken,
          headers: {
            'X-User-Id': String(admin.userId),
            'X-Role-Template': admin.roleTemplate || 'platform_admin',
            'X-Backend-Roles': Array.from(admin.backendRoles || []).join(',')
          }
        })
      } catch (cleanupError) {
        console.error(`Smoke user cleanup failed: ${cleanupError.message}`)
      }
    }
  }
}

main().catch((error) => {
  console.error(error.stack || error.message)
  process.exitCode = 1
})
