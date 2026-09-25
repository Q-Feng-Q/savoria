const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const read = file => fs.readFileSync(path.join(__dirname,'..',file),'utf8');
const load = file => import(`data:text/javascript;base64,${Buffer.from(read(file)).toString('base64')}`);

test('web step photos validate limits and recover from partial upload failure', async () => {
  const { uploadStepImages } = await load('src/utils/step-images.js');
  await assert.rejects(uploadStepImages({existing:Array(5).fill('/uploads/images/a.jpg'),files:[{}]}),/5/);
  const result=await uploadStepImages({files:[{name:'a'},{name:'b'}],upload:async f=>{if(f.name==='b')throw Error('fail');return {url:'/uploads/images/a.jpg'};}});
  assert.deepEqual(result.images,['/uploads/images/a.jpg']);assert.equal(result.failed,1);
  let live=true;
  const stale=await uploadStepImages({files:[{}],isCurrent:()=>live,upload:async()=>{live=false;return {url:'/uploads/images/a.jpg'};}});
  assert.equal(stale.stale,true);assert.deepEqual(stale.images,[]);
});

test('web template edit and snapshots retain ordered pictures', async () => {
  const {createTemplateChangeForm,buildTemplateSnapshot,validateTemplateSnapshot}=await load('src/utils/dish-template-changes.js');
  const form=createTemplateChangeForm({categoryId:1,name:'汤',cookingSteps:[{content:'煮',imageUrls:['/uploads/images/a.jpg','/uploads/images/b.jpg']}]});
  assert.deepEqual(buildTemplateSnapshot(form).cookingSteps[0].imageUrls,['/uploads/images/a.jpg','/uploads/images/b.jpg']);
  form.cookingSteps[0].imageUrls=Array(6).fill('/uploads/images/a.jpg');
  assert.match(validateTemplateSnapshot(buildTemplateSnapshot(form)),/5/);
});

test('web step galleries cover editors, template detail and review comparison', () => {
  for(const file of ['views/merchant/DishesView.vue','views/merchant/DishTemplatesView.vue','views/platform/DishTemplateDetailView.vue','components/SnapshotComparison.vue']) {
    assert.match(read('src/'+file),/<StepImages/);
  }
});

test('dish editor separates pending loads from active photo uploads and rejects stale detail', () => {
  const source=read('src/views/merchant/DishesView.vue');
  assert.match(source, /v-if="!editorLoading"/);
  assert.match(source, /generation !== editorGeneration/);
});

test('template asset URLs use the same supported prefix as the backend', async()=>{
  const {stepImageUrl}=await load('src/utils/step-images.js');
  assert.equal(stepImageUrl('/uploads/dish-template-assets/a.jpg','/api'),'/api/uploads/dish-template-assets/a.jpg');
});

test('template editor preserves stored stable step identity instead of generating another key',async()=>{
  const {createTemplateChangeForm}=await load('src/utils/dish-template-changes.js');
  assert.equal(createTemplateChangeForm({cookingSteps:[{id:123,itemKey:'admin:8:step-original',content:'煮'}]}).cookingSteps[0].itemId,'admin:8:step-original');
});

test('dish editor refuses image-only steps instead of silently dropping uploaded photos',async()=>{
  const {validateDishStepImages}=await load('src/utils/step-images.js');
  assert.match(validateDishStepImages([{content:'  ',imageUrls:['/uploads/images/a.jpg']}]),/步骤.*内容/);
  assert.equal(validateDishStepImages([{content:'煮',imageUrls:['/uploads/images/a.jpg']}]),'');
  assert.match(read('src/views/merchant/DishesView.vue'),/validateDishStepImages\(form.cookingSteps\)/);
});
