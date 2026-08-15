const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const appConfig = JSON.parse(
  fs.readFileSync(path.join(__dirname, '..', 'app.json'), 'utf8')
);

test('app config exposes separate home and menu pages in the main tab flow', () => {
  assert.equal(appConfig.pages[0], 'pages/auth/entry/index');
  assert.equal(appConfig.pages.includes('pages/family/home/index'), true);
  assert.equal(appConfig.pages.includes('pages/ordering/menu/index'), true);

  assert.deepEqual(
    appConfig.tabBar.list.map((item) => ({
      pagePath: item.pagePath,
      text: item.text
    })),
    [
      { pagePath: 'pages/family/home/index', text: '首页' },
      { pagePath: 'pages/ordering/menu/index', text: '去点菜' },
      { pagePath: 'pages/ordering/orders/index', text: '订单进度' },
      { pagePath: 'pages/account/profile/index', text: '我的' }
    ]
  );
});

