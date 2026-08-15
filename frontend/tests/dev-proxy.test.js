const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

const { createProxyServer, rewriteProxyPath } = require('../dev-proxy');

function listen(server) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => resolve(server.address()));
  });
}

function close(server) {
  return new Promise((resolve) => server.close(resolve));
}

test('rewriteProxyPath removes only the leading /api prefix and keeps the query', () => {
  assert.equal(rewriteProxyPath('/api/family/home?date=2026-07-11'), '/family/home?date=2026-07-11');
  assert.equal(rewriteProxyPath('/api'), '/');
  assert.equal(rewriteProxyPath('/health'), null);
});

test('proxy forwards method, rewritten path, body and upstream response', async (t) => {
  let captured = null;
  const upstream = http.createServer((request, response) => {
    const chunks = [];
    request.on('data', (chunk) => chunks.push(chunk));
    request.on('end', () => {
      captured = {
        method: request.method,
        url: request.url,
        body: Buffer.concat(chunks).toString('utf8')
      };
      response.writeHead(201, { 'content-type': 'application/json', 'x-upstream': 'kitchen' });
      response.end(JSON.stringify({ code: 0, data: { forwarded: true } }));
    });
  });
  const upstreamAddress = await listen(upstream);
  const proxy = createProxyServer({ target: `http://127.0.0.1:${upstreamAddress.port}` });
  const proxyAddress = await listen(proxy);

  t.after(async () => {
    await close(proxy);
    await close(upstream);
  });

  const response = await fetch(`http://127.0.0.1:${proxyAddress.port}/api/family/orders?meal=dinner`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ dishId: 12 })
  });

  assert.equal(response.status, 201);
  assert.equal(response.headers.get('x-upstream'), 'kitchen');
  assert.deepEqual(await response.json(), { code: 0, data: { forwarded: true } });
  assert.deepEqual(captured, {
    method: 'POST',
    url: '/family/orders?meal=dinner',
    body: '{"dishId":12}'
  });
});
