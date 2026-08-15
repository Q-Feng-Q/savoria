const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const componentRoot = path.join(root, 'components', 'merchant-workbench-nav');

const expectedRoutes = {
  overview: '/pages/merchant/index',
  orders: '/pages/merchant/merchant-orders/index',
  dishes: '/pages/merchant/merchant-dishes/index',
  families: '/pages/merchant/merchant-families/index'
};

test('merchant workbench navigation is registered globally', () => {
  const appConfig = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
  assert.equal(
    appConfig.usingComponents['merchant-workbench-nav'],
    '/components/merchant-workbench-nav/index'
  );
});

test('merchant workbench component redirects every known inactive destination only', () => {
  const modulePath = path.join(componentRoot, 'index.js');
  const previousComponent = global.Component;
  const previousWx = global.wx;
  let definition;
  const redirects = [];

  global.Component = (value) => { definition = value; };
  global.wx = {
    redirectTo(options) { redirects.push(options); },
    navigateTo() { assert.fail('merchant workbench navigation must never call navigateTo'); }
  };

  try {
    delete require.cache[require.resolve(modulePath)];
    require(modulePath);

    assert.equal(definition.properties.active.type, String);
    for (const [key, url] of Object.entries(expectedRoutes)) {
      const active = key === 'overview' ? 'orders' : 'overview';
      const context = { data: { active } };
      definition.methods.handleNavigate.call(context, { currentTarget: { dataset: { key } } });
      assert.equal(redirects.pop().url, url);
    }

    const context = { data: { active: 'overview' } };
    definition.methods.handleNavigate.call(context, { currentTarget: { dataset: { key: 'overview' } } });
    definition.methods.handleNavigate.call(context, { currentTarget: { dataset: { key: 'unknown' } } });

    assert.deepEqual(redirects, []);
  } finally {
    delete require.cache[require.resolve(modulePath)];
    global.Component = previousComponent;
    global.wx = previousWx;
  }
});

test('merchant workbench component suppresses duplicate redirects while one is in flight', () => {
  const modulePath = path.join(componentRoot, 'index.js');
  const previousComponent = global.Component;
  const previousWx = global.wx;
  let definition;
  const redirects = [];

  global.Component = (value) => { definition = value; };
  global.wx = { redirectTo(options) { redirects.push(options); } };

  try {
    delete require.cache[require.resolve(modulePath)];
    require(modulePath);
    const context = { data: { active: 'overview' } };
    const event = { currentTarget: { dataset: { key: 'orders' } } };

    definition.methods.handleNavigate.call(context, event);
    definition.methods.handleNavigate.call(context, event);
    assert.equal(redirects.length, 1);

    redirects[0].complete();
    definition.methods.handleNavigate.call(context, event);
    assert.equal(redirects.length, 2);

    redirects[1].fail();
    definition.methods.handleNavigate.call(context, event);
    assert.equal(redirects.length, 3);
  } finally {
    delete require.cache[require.resolve(modulePath)];
    global.Component = previousComponent;
    global.wx = previousWx;
  }
});

test('merchant workbench markup exposes exactly four accessible custom controls', () => {
  const source = fs.readFileSync(path.join(componentRoot, 'index.wxml'), 'utf8');
  assert.doesNotMatch(source, /<\/?button\b/);

  const controls = (source.match(/<view(?:"[^"]*"|'[^']*'|[^>])*>/g) || [])
    .filter((tag) => /\bdata-key="[^"]+"/.test(tag));
  assert.equal(controls.length, 4);

  const keys = controls.map((tag) => tag.match(/\bdata-key="([^"]+)"/)[1]);
  assert.deepEqual(keys, Object.keys(expectedRoutes));
  for (const control of controls) {
    assert.match(control, /aria-role="button"/);
    assert.match(control, /aria-label="[^"]+"/);
    assert.match(control, /hover-class="[^"]+"/);
    assert.match(control, /class="[^"]*\{\{active === '[^']+' \? '[^']*active[^']*' : ''\}\}[^"]*"/);
  }
});

test('merchant workbench styles provide fixed safe-area navigation and 88rpx tap targets', () => {
  const markup = fs.readFileSync(path.join(componentRoot, 'index.wxml'), 'utf8');
  const source = fs.readFileSync(path.join(componentRoot, 'index.wxss'), 'utf8');
  assert.match(markup, /<view class="merchant-workbench-nav__spacer"><\/view>/);
  assert.match(source, /position\s*:\s*fixed/);
  assert.match(source, /bottom\s*:\s*0/);
  assert.match(source, /env\(safe-area-inset-bottom\)/);
  assert.match(source, /min-height\s*:\s*88rpx/);
  assert.match(
    source,
    /\.merchant-workbench-nav__spacer\s*\{[^}]*min-height\s*:\s*calc\(88rpx\s*\+\s*env\(safe-area-inset-bottom\)\)/s
  );
});
