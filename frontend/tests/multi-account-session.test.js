const test=require('node:test');
const assert=require('node:assert/strict');
const {createSessionStore}=require('../utils/session');
function storage(){const m=new Map();return{get:k=>m.has(k)?m.get(k):null,set:(k,v)=>m.set(k,v),remove:k=>m.delete(k)};}

test('session store saves multiple accounts without duplicating the same user',()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:1,username:'alice',accessToken:'a'});
 store.setSession({userId:2,username:'bob',accessToken:'b'});
 store.setSession({userId:1,username:'alice',accessToken:'a2'});
 assert.deepEqual(store.listAccounts().map(x=>x.userId),[1,2]);
 assert.equal(store.getSession().accessToken,'a2');
});

test('switching and removing accounts updates the active session',()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:1,username:'alice',accessToken:'a'});
 store.setSession({userId:2,username:'bob',accessToken:'b'});
 store.switchAccount(1);
 assert.equal(store.getSession().username,'alice');
 store.removeAccount(1);
 assert.equal(store.getSession().username,'bob');
 assert.equal(store.listAccounts().length,1);
});

test('clearing current session does not erase saved account list unless requested',()=>{
 const store=createSessionStore({storage:storage()});store.setSession({userId:1,username:'alice',accessToken:'a'});
 store.clearSession();assert.equal(store.getSession(),null);assert.equal(store.listAccounts().length,1);
 store.clearAllAccounts();assert.equal(store.listAccounts().length,0);
});

test('one account exposes family and merchant modes without duplicate records',()=>{
 let tick=100;
 const store=createSessionStore({storage:storage(),now:()=>++tick});
 store.setSession({userId:1,username:'alice',accessToken:'a',familyId:2,merchantId:3});
 store.setSession({userId:1,username:'alice',accessToken:'a2',familyId:2,merchantId:3});
 assert.equal(store.listAccounts().length,1);
 assert.deepEqual(store.listAccounts()[0].availableModes,['family','merchant']);
 assert.equal(store.getSession().activeMode,'family');
});

test('activating an identity updates active session and recent account ordering',()=>{
 let tick=200;
 const store=createSessionStore({storage:storage(),now:()=>++tick});
 store.setSession({userId:1,username:'alice',accessToken:'a',familyId:2,merchantId:3});
 store.setSession({userId:2,username:'bob',accessToken:'b',familyId:4});
 const active=store.activateAccount(1,'merchant');
 assert.equal(active.activeMode,'merchant');
 assert.equal(store.getSession().userId,1);
 assert.deepEqual(store.listAccounts().map(item=>item.userId),[1,2]);
});

test('invalid identity cannot be activated and expired account remains visible',()=>{
 const store=createSessionStore({storage:storage()});
 store.setSession({userId:1,username:'alice',accessToken:'a',familyId:2});
 assert.equal(store.activateAccount(1,'merchant'),null);
 store.markRequiresLogin(1,true);
 assert.equal(store.listAccounts()[0].requiresLogin,true);
 assert.equal(store.getSession().requiresLogin,true);
});
