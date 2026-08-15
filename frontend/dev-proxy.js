const http = require('node:http');
const https = require('node:https');

const DEFAULT_HOST = '127.0.0.1';
const DEFAULT_PORT = 3001;
const DEFAULT_TARGET = 'http://127.0.0.1:8080';

function rewriteProxyPath(pathname = '') {
  if (pathname === '/api') return '/';
  if (!pathname.startsWith('/api/')) return null;
  return pathname.slice(4) || '/';
}

function createProxyServer(options = {}) {
  const target = new URL(options.target || DEFAULT_TARGET);
  if (target.protocol !== 'http:' && target.protocol !== 'https:') {
    throw new Error(`Unsupported proxy target protocol: ${target.protocol}`);
  }
  const transport = target.protocol === 'https:' ? https : http;

  return http.createServer((request, response) => {
    const rewrittenPath = rewriteProxyPath(request.url || '/');
    if (rewrittenPath === null) {
      response.writeHead(404, { 'content-type': 'application/json; charset=utf-8' });
      response.end(JSON.stringify({ code: 404, message: 'Only /api requests are proxied' }));
      return;
    }

    const headers = { ...request.headers, host: target.host };
    delete headers.connection;

    const upstreamRequest = transport.request({
      protocol: target.protocol,
      hostname: target.hostname,
      port: target.port || undefined,
      method: request.method,
      path: `${target.pathname.replace(/\/$/, '')}${rewrittenPath}`,
      headers
    }, (upstreamResponse) => {
      response.writeHead(upstreamResponse.statusCode || 502, upstreamResponse.headers);
      upstreamResponse.pipe(response);
    });

    upstreamRequest.on('error', (error) => {
      console.error(`[mini-api-proxy] ${request.method} ${request.url}: ${error.message}`);
      if (response.headersSent) {
        response.destroy(error);
        return;
      }
      response.writeHead(502, { 'content-type': 'application/json; charset=utf-8' });
      response.end(JSON.stringify({ code: 502, message: 'Backend service is unavailable' }));
    });

    request.pipe(upstreamRequest);
  });
}

function startProxy() {
  const host = process.env.MINI_PROXY_HOST || DEFAULT_HOST;
  const port = Number(process.env.MINI_PROXY_PORT || DEFAULT_PORT);
  const target = process.env.MINI_PROXY_TARGET || DEFAULT_TARGET;
  const server = createProxyServer({ target });
  server.listen(port, host, () => {
    console.log(`[mini-api-proxy] http://${host}:${port}/api/* -> ${target}/*`);
  });
  return server;
}

if (require.main === module) {
  startProxy();
}

module.exports = {
  createProxyServer,
  rewriteProxyPath,
  startProxy
};
