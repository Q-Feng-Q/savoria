const test=require('node:test');
const assert=require('node:assert/strict');
const {buildProfileMenuGroups}=require('../utils/profile-menu');
const {isMerchantSession}=require('../utils/page-api');

test('explicit permission codes drive merchant and family admin actions',()=>{
 const keys=buildProfileMenuGroups({familyId:10,merchantId:20,permissionCodes:['FAMILY_MEMBER','FAMILY_ADMIN','MERCHANT_ADMIN']}).flatMap(g=>g.items.map(i=>i.key));
 assert.ok(keys.includes('familyManagement'));
 assert.ok(keys.includes('merchant'));
 assert.equal(isMerchantSession({activeMode:'merchant',merchantId:20,permissionCodes:['MERCHANT_ADMIN']}),true);
});

test('an explicit empty permission list blocks stale legacy merchant grants',()=>{
 const keys=buildProfileMenuGroups({familyId:10,merchantId:20,merchantRole:'MERCHANT_ADMIN',permissionCodes:[]}).flatMap(g=>g.items.map(i=>i.key));
 assert.ok(!keys.includes('merchant'));
 assert.equal(isMerchantSession({activeMode:'merchant',merchantId:20,roleTemplate:'merchant_admin',permissionCodes:[]}),false);
});

test('joint family and merchant owner sees family and merchant actions',()=>{
 const keys=buildProfileMenuGroups({familyId:10,familyRole:'OWNER',merchantId:20,merchantRole:'MERCHANT_ADMIN'}).flatMap(g=>g.items.map(i=>i.key));
 assert.ok(keys.includes('familyManagement'));
 assert.ok(keys.includes('merchant'));
 assert.ok(keys.includes('orders'));
});

test('ordinary family member sees family services but not management workspaces',()=>{
 const keys=buildProfileMenuGroups({familyId:10,familyRole:'MEMBER',merchantId:null}).flatMap(g=>g.items.map(i=>i.key));
 assert.ok(keys.includes('orders'));
 assert.ok(!keys.includes('familyManagement'));
 assert.ok(!keys.includes('merchant'));
});
