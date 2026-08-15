const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const { resolveNotificationScope }=require('../utils/api-runtime');

test('normal and unbound users use the account notification scope',()=>{
  assert.equal(resolveNotificationScope({roleTemplate:'member',familyId:2}),'account');
  assert.equal(resolveNotificationScope({roleTemplate:'member',familyId:null}),'account');
});

test('notification center marks the same account aggregation as read',()=>{
  const source=fs.readFileSync(path.join(__dirname,'../pages/account/notifications/index.js'),'utf8');
  assert.match(source,/receiverScope:\s*'account'/);
  assert.match(source,/family:\s*'家庭动态'/);
});

