const test=require('node:test');const assert=require('node:assert/strict');const fs=require('node:fs');const path=require('node:path');const root=path.resolve(__dirname,'..');
test('mini account management supports switch add and remove',()=>{const app=fs.readFileSync(path.join(root,'app.json'),'utf8');const js=fs.readFileSync(path.join(root,'pages/account/account-management/index.js'),'utf8');const wxml=fs.readFileSync(path.join(root,'pages/account/account-management/index.wxml'),'utf8');assert.match(app,/pages\/account\/account-management\/index/);assert.match(js,/switchAccount/);assert.match(js,/removeAccount/);assert.match(js,/addAccount/);assert.match(wxml,/添加账号/);});

test('account page exposes current user id and identity synchronization failures without clearing local accounts',()=>{
 const js=fs.readFileSync(path.join(root,'pages/account/account-management/index.js'),'utf8');
 const wxml=fs.readFileSync(path.join(root,'pages/account/account-management/index.wxml'),'utf8');
 assert.match(js,/currentUserId/);
 assert.match(js,/syncError/);
 assert.match(js,/catch \(error\)[\s\S]*setData\(\{\s*syncError:/);
 assert.doesNotMatch(js,/catch \(error\)[\s\S]*clearAllAccounts/);
 assert.match(wxml,/\{\{currentUserId\}\}/);
 assert.match(wxml,/wx:if="\{\{syncError\}\}"/);
});

test('profile refresh persists explicit backend modes and permission codes',()=>{
 const source=fs.readFileSync(path.join(root,'pages/account/profile/index.js'),'utf8');
 assert.match(source,/\.\.\.effectiveIdentityContext/);
 assert.match(source,/permissionCodes/);
 assert.match(source,/availableModes/);
});

test('login and account controls fill their tracks without native button sizing',()=>{
 const entry=fs.readFileSync(path.join(root,'pages/auth/entry/index.wxss'),'utf8');
 const accountCss=fs.readFileSync(path.join(root,'pages/account/account-management/index.wxss'),'utf8');
 const accountWxml=fs.readFileSync(path.join(root,'pages/account/account-management/index.wxml'),'utf8');
 const accountJs=fs.readFileSync(path.join(root,'pages/account/account-management/index.js'),'utf8');
 assert.match(entry,/\.entry-actions action-button\s*\{[^}]*width:\s*100%/s);
 assert.match(accountCss,/\.identity-action,.account-reauth\s*\{[^}]*min-height:\s*88rpx/s);
 assert.match(accountWxml,/identity-action[^>]*is-disabled[^>]*aria-disabled=/s);
 assert.match(accountJs,/switchAccount\(event\)\s*\{\s*if \(this\.data\.switchingKey\) return;/s);
 assert.match(accountJs,/switchIdentity\(event\)\s*\{\s*if \(this\.data\.switchingKey\) return;/s);
});
test('profile switch entry opens account management instead of clearing the session',()=>{const source=fs.readFileSync(path.join(root,'pages/account/profile/index.js'),'utf8');const body=source.match(/switchIdentity\(\)\s*\{([\s\S]*?)\n\s*\}/)?.[1]||'';assert.match(body,/account-management/);assert.doesNotMatch(body,/clearSession/);});

test('profile and merchant hero both expose a visible account identity switch',()=>{
 const profile=fs.readFileSync(path.join(root,'pages/account/profile/index.wxml'),'utf8');
 const merchant=fs.readFileSync(path.join(root,'pages/merchant/index.wxml'),'utf8');
 const merchantJs=fs.readFileSync(path.join(root,'pages/merchant/index.js'),'utf8');
 assert.match(profile,/profile-switch-link[\s\S]*切换账号\/身份/);
 assert.match(merchant,/merchant-switch-link[\s\S]*切换账号\/身份/);
 assert.match(merchantJs,/openAccountSwitcher[\s\S]*account-management/);
});

test('profile merchant entry switches identity before opening the workbench',()=>{
 const source=fs.readFileSync(path.join(root,'pages/account/profile/index.js'),'utf8');
 const body=source.match(/openMerchant\(\)\s*\{([\s\S]*?)\n\s*\}/)?.[1]||'';
 assert.match(source,/switchAccountIdentity/);
 assert.match(body,/mode:\s*'merchant'/);
 assert.doesNotMatch(body,/navigateTo\(\{\s*url:\s*'\/pages\/merchant\/index'/);
});

test('account cards expose separate family and merchant identity actions',()=>{
 const js=fs.readFileSync(path.join(root,'pages/account/account-management/index.js'),'utf8');
 const wxml=fs.readFileSync(path.join(root,'pages/account/account-management/index.wxml'),'utf8');
 assert.match(js,/switchIdentity/);
 assert.match(js,/switchAccountIdentity/);
 assert.match(wxml,/data-mode="family"[\s\S]*进入家庭端/);
 assert.match(wxml,/data-mode="merchant"[\s\S]*进入商户端/);
 assert.match(wxml,/需重新登录/);
 assert.doesNotMatch(wxml,/ACCOUNT SWITCHER/);
});

test('account page refreshes live identities without validating a stale selected mode',()=>{
 const js=fs.readFileSync(path.join(root,'pages/account/account-management/index.js'),'utf8');
 const body=js.match(/async refreshIdentity\(\)\s*\{([\s\S]*?)\n\s*\},\n\s*refresh\(/)?.[1]||'';
 assert.match(js,/refreshAccountIdentity/);
 assert.match(body,/refreshAccountIdentity/);
 assert.doesNotMatch(body,/switchAccountIdentity/);
});

test('reauthentication can prefill the selected username',()=>{
 const entry=fs.readFileSync(path.join(root,'pages/auth/entry/index.js'),'utf8');
 assert.match(entry,/query\.username/);
 assert.match(entry,/decodeURIComponent/);
});
