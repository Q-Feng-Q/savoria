const { chromium } = require('C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
async function main() {
  const out = path.resolve(__dirname, '../../output/playwright/admin-workspaces');
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    for (const platform of [false, true]) {
      const page = await browser.newPage();
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.addInitScript(({platform}) => localStorage.setItem('family_kitchen_admin_session', JSON.stringify({ token: 'ui-fixture', username: '界面测试', roleTemplate: platform ? 'platform_admin' : 'merchant_admin', merchantId: platform ? null : 1 })), { platform });
      await page.route(url => url.pathname.startsWith('/api/'), route => route.fulfill({ json: { code: 0, data: route.request().url().includes('notifications') || route.request().url().includes('change-requests') ? { items: [], total: 0, page: 1, pageSize: 20 } : [] } }));
      for (const width of [1440, 390]) {
        await page.setViewportSize({ width, height: 960 });
        await page.goto('http://127.0.0.1:5182' + (platform ? '/dish-template-change-reviews' : '/dishes'), { waitUntil: 'networkidle' });
        if (await page.locator('.sidebar-link').count() !== 5) throw Error('Incorrect navigation count');
        if (!await page.locator('.workspace-tabs').isVisible()) throw Error('Workspace tabs missing');
        const geometry = await page.evaluate(() => ({ overflow: document.documentElement.scrollWidth > innerWidth, broken: [...document.images].filter(i => !i.complete || !i.naturalWidth).map(i => i.src) }));
        if (geometry.overflow || geometry.broken.length) throw Error(JSON.stringify({ platform, width, ...geometry }));
        if (platform && !await page.locator('select').filter({has: page.locator('option', {hasText: '全部商户'})}).count()) throw Error('Merchant dropdown missing');
        if (!platform) {
          await page.getByRole('link', { name: '食材字典', exact: true }).click();
          await page.waitForURL('**/ingredients');
          await page.locator('.workspace-tabs a.is-current').filter({ hasText: '食材字典' }).waitFor();
        }
        await page.screenshot({ path: path.join(out, `${platform ? 'platform' : 'merchant'}-${width}.png`), fullPage: true });
        console.log(JSON.stringify({ platform, width, navigation: 5, overflow: false, brokenImages: 0 }));
      }
      if (errors.length) throw Error(errors.join('\n'));
      await page.close();
    }
  } finally { await browser.close(); }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
