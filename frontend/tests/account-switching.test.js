const test=require('node:test');
const assert=require('node:assert/strict');
const {createSessionStore}=require('../utils/session');
const {modesFromContext,switchAccountIdentity,refreshAccountIdentity}=require('../utils/account-switching');

function storage(){const m=new Map();return{get:k=>m.has(k)?m.get(k):null,set:(k,v)=>m.set(k,v),remove:k=>m.delete(k)};}

test('switch validates target context before activating merchant identity',async()=>{
 const store=createSessionStore({storage:storage(),now:()=>100});
 store.setSession({userId:1,username:'alice',accessToken:'a',familyId:10});
 store.setSession({userId:2,username:'bob',accessToken:'b',familyId:20,merchantId:30});
 store.activateAccount(1,'family');
 let tokenDuringValidation='';
 const result=await switchAccountIdentity({userId:2,mode:'merchant'}, {
  sessionStore:store,
  loadContext:async()=>{tokenDuringValidation=store.getToken();return{userId:2,familyId:20,familyRole:'MEMBER',merchantId:30,merchantRole:'MERCHANT_ADMIN',platformRoles:[]};}
 });
 assert.equal(tokenDuringValidation,'b');
 assert.equal(result.destination,'/pages/merchant/index');
 assert.equal(store.getSession().userId,2);
 assert.equal(store.getSession().activeMode,'merchant');
});

test('same account can switch from merchant identity back to family identity',async()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:2,username:'bob',accessToken:'b',familyId:20,merchantId:30,activeMode:'merchant'});
 const result=await switchAccountIdentity({userId:2,mode:'family'}, {
  sessionStore:store,
  loadContext:async()=>({userId:2,familyId:20,familyRole:'OWNER',merchantId:30,merchantRole:'MERCHANT_ADMIN',platformRoles:[]})
 });
 assert.equal(result.destination,'/pages/family/home/index');
 assert.equal(store.getSession().activeMode,'family');
});

test('realtime context can reveal a newly approved merchant identity',async()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:2,username:'bob',accessToken:'b',familyId:20,activeMode:'family'});
 const result=await switchAccountIdentity({userId:2,mode:'merchant'}, {
  sessionStore:store,
  loadContext:async()=>({userId:2,familyId:20,familyRole:'OWNER',merchantId:30,merchantRole:'MERCHANT_ADMIN',platformRoles:[]})
 });
 assert.equal(result.destination,'/pages/merchant/index');
 assert.deepEqual(store.getSession().availableModes,['family','merchant']);
 assert.equal(store.getSession().activeMode,'merchant');
});

test('identity refresh discovers merchant-only access even when stale cache selected family',async()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:2,username:'bob',accessToken:'b',activeMode:'family'});
 const session=await refreshAccountIdentity({userId:2}, {
  sessionStore:store,
  loadContext:async()=>({userId:2,familyId:null,familyRole:null,merchantId:30,merchantRole:'MERCHANT_ADMIN',platformRoles:[]})
 });
 assert.deepEqual(session.availableModes,['merchant']);
 assert.equal(session.activeMode,'merchant');
 assert.equal(store.getAccount(2).merchantId,30);
});

test('network failure restores previously active account',async()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:1,username:'alice',accessToken:'a',familyId:10});
 store.setSession({userId:2,username:'bob',accessToken:'b',merchantId:30});
 store.activateAccount(1,'family');
 await assert.rejects(()=>switchAccountIdentity({userId:2,mode:'merchant'}, {
  sessionStore:store,loadContext:async()=>{throw new Error('offline');}
 }),/offline/);
 assert.equal(store.getSession().userId,1);
 assert.equal(store.getToken(),'a');
});

test('expired target is marked for login without replacing current account',async()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:1,username:'alice',accessToken:'a',familyId:10});
 store.setSession({userId:2,username:'bob',accessToken:'b',merchantId:30});
 store.activateAccount(1,'family');
 await assert.rejects(()=>switchAccountIdentity({userId:2,mode:'merchant'}, {
  sessionStore:store,loadContext:async()=>{const error=new Error('expired');error.code=40101;throw error;}
 }),error=>error&&error.requiresLogin===true);
 assert.equal(store.getSession().userId,1);
 assert.equal(store.getAccount(2).requiresLogin,true);
});

test('platform-only administrator is rejected from mini program',async()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:1,username:'alice',accessToken:'a',familyId:10});
 store.setSession({userId:9,username:'root',accessToken:'p'});
 store.activateAccount(1,'family');
 await assert.rejects(()=>switchAccountIdentity({userId:9,mode:'family'}, {
  sessionStore:store,
  loadContext:async()=>({userId:9,familyId:null,merchantId:null,merchantRole:null,platformRoles:['PLATFORM_ADMIN']})
 }),error=>error&&error.platformAdmin===true);
 assert.equal(store.getSession().userId,1);
});

test('explicit backend modes take precedence over legacy role inference',()=>{
 assert.deepEqual(modesFromContext({familyId:20,merchantId:30,merchantRole:null,availableModes:['merchant']}),['merchant']);
 assert.deepEqual(modesFromContext({familyId:20,merchantId:30,merchantRole:'MERCHANT_ADMIN'}),['family','merchant']);
});

test('identity refresh persists explicit permission codes from the backend',async()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:2,username:'bob',accessToken:'b',familyId:20});
 const session=await refreshAccountIdentity({userId:2},{
  sessionStore:store,
  loadContext:async()=>({userId:2,familyId:20,merchantId:30,availableModes:['family','merchant'],permissionCodes:['FAMILY_MEMBER','MERCHANT_ADMIN']})
 });
 assert.deepEqual(session.availableModes,['family','merchant']);
 assert.deepEqual(session.permissionCodes,['FAMILY_MEMBER','MERCHANT_ADMIN']);
});
