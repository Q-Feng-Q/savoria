// Isolated browser fixtures: all API requests are intercepted, never sent to the business backend.
const { chromium } = require('C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
async function main() {
  const out = path.resolve(__dirname, '../../output/playwright/feedback-admin');
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  try {
    for (const width of [1440, 390]) {
      const page = await browser.newPage({ viewport: { width, height: 960 } });
      const errors = []; page.on('pageerror', error => errors.push(error.message));
      await page.addInitScript(() => localStorage.setItem('family_kitchen_admin_session', JSON.stringify({ accessToken: 'isolated-ui-fixture', userId: 1, roleTemplate: 'platform_admin', backendRoles: ['PLATFORM_ADMIN'] })));
      let detail = { feedbackId: 1, ownerUserId: 2, ownerName: '小林', type: 'BUG', status: 'OPEN', content: '餐篮修改菜品数量后，合计金额没有及时变化。\n重新进入页面后恢复正常。', reply: '', version: 0, createdAt: '2026-09-21T12:30:00', images: [], history: [] };
      await page.route(url => url.pathname.startsWith('/api/'), async route => {
        const req = route.request(), url = new URL(req.url()); let data = {};
        if (url.pathname === '/api/admin/feedback/1') {
          if (req.method() === 'PUT') { const body = req.postDataJSON(); detail = { ...detail, ...body, version: detail.version + 1, history: [{ historyId: 1, adminId: 1, fromStatus: 'OPEN', toStatus: body.status, reply: body.reply, createdAt: '2026-09-21T13:00:00' }] }; }
          data = detail;
        } else if (url.pathname === '/api/admin/feedback') data = { items: [detail], total: 1, page: 1, pageSize: 20 };
        else if (url.pathname.includes('notifications')) data = { items: [], total: 0 };
        await route.fulfill({ json: { code: 0, data } });
      });
      await page.goto('http://127.0.0.1:5182/platform-feedback', { waitUntil: 'networkidle' });
      await page.getByRole('button', { name: '查看', exact: true }).click();
      await page.locator('.feedback-inspector').waitFor();
      await page.locator('.feedback-form select').selectOption('RESOLVED');
      await page.locator('.feedback-form textarea').fill('已修复数量变化后的金额刷新，请更新后重试。');
      await page.getByRole('button', { name: '保存处理结果', exact: true }).click();
      await page.getByText('处理结果已保存', { exact: true }).waitFor();
      if (!(await page.locator('.feedback-history').innerText()).includes('已解决')) throw Error('Audit history missing');
      if (!(await page.locator('.feedback-inspector').innerText()).includes('小林')) throw Error('Submitter missing after save');
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth);
      if (overflow || errors.length) throw Error(JSON.stringify({ width, overflow, errors }));
      await page.screenshot({ path: path.join(out, `feedback-${width}.png`), fullPage: true });
      console.log(JSON.stringify({ width, overflow: false, saveAndHistory: true, fixtureOnly: true }));
      await page.close();
    }
  } finally { await browser.close(); }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
