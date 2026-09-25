const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const helper = () => require('../utils/step-images');

test('step photos accept zero to five and reject excess or unsafe paths', () => {
  const { validateStepImages } = helper();
  assert.equal(validateStepImages(), '');
  assert.equal(validateStepImages(Array(5).fill('/uploads/images/a.jpg')), '');
  assert.match(validateStepImages(Array(6).fill('/uploads/images/a.jpg')), /5/);
  for (const value of ['javascript:x', 'data:x', '//evil/a', '/x\n', 'http://evil/a']) {
    assert.ok(validateStepImages([value]), value);
  }
});

test('step photo upload keeps successes after failure and uses raw server URLs', async () => {
  const { uploadStepImages } = helper();
  const result = await uploadStepImages({ existing: ['/uploads/images/old.jpg'], files: [{tempFilePath:'a'}, {tempFilePath:'b'}, {tempFilePath:'c'}],
    upload: async p => { if (p === 'b') throw Error('failed'); return {url:`/uploads/images/${p}.jpg`, imageUrl:'http://localhost/x'}; } });
  assert.deepEqual(result.images, ['/uploads/images/old.jpg','/uploads/images/a.jpg','/uploads/images/c.jpg']);
  assert.equal(result.failed, 1);
});

test('step photo upload enforces remaining capacity and ignores stale responses', async () => {
  const { uploadStepImages } = helper();
  let calls = 0;
  const options = {existing:Array(4).fill('/uploads/images/a.jpg'), files:[{tempFilePath:'a'},{tempFilePath:'b'}], upload:async()=>{calls++;return {url:'/uploads/images/b.jpg'};}};
  await assert.rejects(uploadStepImages(options), /5/);
  assert.equal(calls,0);
  let live=true;
  const result=await uploadStepImages({...options,existing:[],isCurrent:()=>live,upload:async()=>{live=false;return {url:'/uploads/images/b.jpg'};}});
  assert.equal(result.stale,true);
  assert.deepEqual(result.images,[]);
});

test('dish and template payloads preserve step images and validate maximum', () => {
  global.Page=()=>{};
  const {buildDishPayload,validateDish}=require('../pages/merchant/dish-edit/index');
  const {buildTemplateSnapshot}=require('../utils/dish-template-change');
  const source={name:'汤',categoryId:1,basePrice:5,cookingSteps:[{content:'炖煮',imageUrls:['/uploads/images/a.jpg']}]};
  assert.deepEqual(buildDishPayload(source).cookingSteps[0].imageUrls,source.cookingSteps[0].imageUrls);
  assert.deepEqual(buildTemplateSnapshot(source).cookingSteps[0].imageUrls,source.cookingSteps[0].imageUrls);
  source.cookingSteps[0].imageUrls=Array(6).fill('/uploads/images/a.jpg');
  assert.match(validateDish(source),/5/);
  delete global.Page;
});

test('step image component is wired to editable and read-only entrances', () => {
  const root=path.resolve(__dirname,'..');
  const editor=fs.readFileSync(path.join(root,'pages/merchant/dish-edit/index.wxml'),'utf8');
  assert.match(editor, /wx:for="\{\{dish\.cookingSteps\}\}"(?:(?!<\/block>)[\s\S])*<step-images/);
  assert.doesNotMatch(editor, /wx:for="\{\{dish\.ingredients\}\}"(?:(?!<\/block>)[\s\S])*<step-images/);
  assert.match(fs.readFileSync(path.join(root,'pages/ordering/dish-detail/index.wxml'),'utf8'), /wx:if="\{\{scene\.dish\.cookingSteps\.length\}\}"/);
  for(const page of ['merchant/dish-edit','merchant/dish-template-change-edit','merchant/dish-template-detail','merchant/dish-template-change-detail','ordering/dish-detail']) {
    assert.match(fs.readFileSync(path.join(root,'pages',page,'index.wxml'),'utf8'), /<step-images/);
  }
});

function componentHarness(upload) {
  let definition, session = {userId:1,accessToken:'one'}, chosen;
  const events=[];
  const wx={chooseMedia(options){chosen=options;},showToast(){},previewImage(){}};
  vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../components/step-images/index.js'),'utf8'),{
    Component:value=>{definition=value;},wx,
    require:name=> name.endsWith('/api-runtime') ? {createApiRuntime:()=>({baseUrl:'http://localhost',files:{uploadImage:upload}})}
      : name.endsWith('/session') ? {sessionStore:{getSession:()=>session}}
      : require(path.resolve(__dirname,'../components/step-images',name))
  });
  const instance={properties:{images:Array(5).fill('/uploads/images/old.jpg'),editable:true,disabled:false},data:{busy:false},
    ...definition.methods,setData(value){Object.assign(this.data,value);},triggerEvent(type,detail){events.push({type,detail});if(type==='change')this.properties.images=detail.images;}};
  definition.lifetimes.attached.call(instance);
  return {instance,events,select:files=>chosen.success({tempFiles:files}),switchIdentity:()=>{session={userId:2,accessToken:'two'};},detach:()=>definition.lifetimes.detached.call(instance)};
}

test('component deletes a photo then allows one replacement and emits complete raw list', async () => {
  const h=componentHarness(async()=>({url:'/uploads/images/new.jpg'}));
  h.instance.remove({currentTarget:{dataset:{index:2}}});
  assert.equal(h.instance.properties.images.length,4);
  const pending=h.instance.choose();h.select([{tempFilePath:'photo'}]);await pending;
  assert.equal(h.instance.properties.images.length,5);
  assert.equal(h.instance.properties.images[4],'/uploads/images/new.jpg');
  assert.equal(h.events.at(-1).detail.busy,false);
});

for(const reason of ['switchIdentity','detach']) test(`component rejects upload response after ${reason}`,async()=>{
  let release;
  const h=componentHarness(()=>new Promise(resolve=>{release=resolve;}));h.instance.properties.images=[];
  const pending=h.instance.choose();h.select([{tempFilePath:'photo'}]);await Promise.resolve();await Promise.resolve();
  h[reason]();release({url:'/uploads/images/new.jpg'});await pending;
  assert.equal(h.events.filter(e=>e.type==='change').length,0);
});

test('both mini editors reject save and step deletion while photos upload',async()=>{
  for(const page of ['dish-edit','dish-template-change-edit']){
    let def;const old=global.Page;global.Page=value=>{def=value;};
    const file=require.resolve(`../pages/merchant/${page}/index.js`);delete require.cache[file];require(file);global.Page=old;
    const instance={...def,data:{...def.data,stepUploading:true},setData(){assert.fail('busy mutation');}};
    instance.removeCookingStep({currentTarget:{dataset:{index:0}}});
    await (page==='dish-edit'?instance.saveDish():instance.submit());
    delete require.cache[file];
  }
});
