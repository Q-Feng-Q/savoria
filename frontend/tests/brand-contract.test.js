const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.join(__dirname, '..');
const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));

function collectVisibleSource(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'tests' || entry.name === 'assets') return [];
      return collectVisibleSource(full);
    }
    return /\.(js|json|wxml)$/.test(entry.name) ? [full] : [];
  });
}

test('user-visible mini program chrome uses 食光知味', () => {
  assert.equal(app.window.navigationBarTitleText, '食光知味');

  const visibleText = collectVisibleSource(root)
    .map((file) => fs.readFileSync(file, 'utf8'))
    .join('\n');

  assert.equal(/灶间私厨|家庭厨房/.test(visibleText), false);
});

test('all configured mini program pages exist', () => {
  for (const page of app.pages) {
    assert.equal(fs.existsSync(path.join(root, `${page}.js`)), true, page);
    assert.equal(fs.existsSync(path.join(root, `${page}.wxml`)), true, page);
  }
});

test('warm animal brand assets are available to production pages', () => {
  const requiredAssets = [
    'animal-kitchen-hero.webp',
    'animal-mascot-trio.webp',
    'dish-placeholder.png',
  ];

  for (const name of requiredAssets) {
    const file = path.join(root, 'assets', 'brand', name);
    assert.equal(fs.existsSync(file), true, name);
    assert.ok(fs.statSync(file).size > 0, name);
  }
});

test('shared mini program UI components are globally registered', () => {
  const components = {
    'page-state': '/components/page-state/index',
    'brand-scene': '/components/brand-scene/index',
    'action-button': '/components/action-button/index',
    'filter-bar': '/components/filter-bar/index',
    'group-menu': '/components/group-menu/index',
    'quantity-stepper': '/components/quantity-stepper/index',
    'dish-row': '/components/dish-row/index',
    'order-row': '/components/order-row/index',
    'purchase-row': '/components/purchase-row/index',
    'notification-row': '/components/notification-row/index',
    'status-timeline': '/components/status-timeline/index',
    'bottom-action-bar': '/components/bottom-action-bar/index',
    'merchant-workbench-nav': '/components/merchant-workbench-nav/index'
  };

  assert.deepEqual(app.usingComponents, components);

  for (const target of Object.values(components)) {
    const base = path.join(root, target.replace(/^\//, ''));
    for (const extension of ['js', 'json', 'wxml', 'wxss']) {
      assert.equal(fs.existsSync(`${base}.${extension}`), true, `${target}.${extension}`);
    }
  }
});

test('family portal uses warm animal composition without dashboard card soup', () => {
  const homeWxml = fs.readFileSync(path.join(root, 'pages', 'family', 'home', 'index.wxml'), 'utf8');
  const entryWxml = fs.readFileSync(path.join(root, 'pages', 'auth', 'entry', 'index.wxml'), 'utf8');

  assert.match(homeWxml, /animal-kitchen-hero\.webp/);
  assert.match(homeWxml, /home-story/);
  assert.match(entryWxml, /brand-scene/);
  assert.doesNotMatch(homeWxml, /status-grid|flow-panel/);
});

test('family ordering journey reuses shared rows and action bars', () => {
  const menu = fs.readFileSync(path.join(root, 'pages', 'ordering', 'menu', 'index.wxml'), 'utf8');
  const cart = fs.readFileSync(path.join(root, 'pages', 'ordering', 'cart', 'index.wxml'), 'utf8');
  const orders = fs.readFileSync(path.join(root, 'pages', 'ordering', 'orders', 'index.wxml'), 'utf8');
  const detail = fs.readFileSync(path.join(root, 'pages', 'ordering', 'order-detail', 'index.wxml'), 'utf8');
  const profile = fs.readFileSync(path.join(root, 'pages', 'account', 'profile', 'index.wxml'), 'utf8');

  assert.match(menu, /dish-row/);
  assert.match(cart, /bottom-action-bar/);
  assert.match(orders, /order-row/);
  assert.match(detail, /status-timeline/);
  assert.match(profile, /group-menu/);
});
