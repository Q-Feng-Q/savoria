// Local fixture-only verification. Every /api request is intercepted.
const {chromium}=require('C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs'),path=require('node:path');
const assert=require('node:assert/strict');
const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=','base64');
async function main(){
 const out=path.resolve(__dirname,'../../output/playwright/step-images');fs.mkdirSync(out,{recursive:true});
 const browser=await chromium.launch({headless:true});
 try{
  for(const width of [320,375,430]){
   const page=await browser.newPage({viewport:{width,height:900}});
   for(const name of ['merchant-dish-edit','dish-detail']){
    await page.goto(`http://127.0.0.1:5183/?page=${name}&width=${width}`,{waitUntil:'networkidle'});
    assert.equal(await page.locator('.step-gallery__image').count(),5);
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,`${name} overflow ${width}`);
    await page.screenshot({path:path.join(out,`mini-${name}-${width}.png`),fullPage:true});
    console.log(JSON.stringify({surface:'mini-source',name,width,photos:5,overflow:false}));
   }await page.close();
  }
  for(const width of [1440,390]){
   const page=await browser.newPage({viewport:{width,height:1000}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.addInitScript(()=>localStorage.setItem('family_kitchen_admin_session',JSON.stringify({accessToken:'isolated-fixture',userId:2,merchantId:1,roleTemplate:'merchant_admin',backendRoles:['MERCHANT_ADMIN'],merchantAdminScopes:['merchant']})));
   const dishes=[1,2].map(dishId=>({dishId,name:`测试菜品${dishId}`,categoryId:1,price:20,status:'ACTIVE',ingredients:[],cookingSteps:[{stepNo:1,title:'准备',content:'清洗并切块',imageUrls:['/uploads/images/old.png']},{stepNo:2,title:'炖煮',content:'小火慢炖',imageUrls:[]}]}));
   let saved,uploads=0,hold=false,release;
   await page.route(url=>url.pathname.startsWith('/api/'),async route=>{
    const req=route.request(),url=new URL(req.url());let data={};
    if(url.pathname.startsWith('/api/uploads/')){await route.fulfill({body:png,contentType:'image/png'});return;}
    if(url.pathname==='/api/files/images')data={url:`/uploads/images/new-${++uploads}.png`};
    else if(url.pathname==='/api/merchant/dishes')data=dishes;
    else if(/^\/api\/merchant\/dishes\/\d+$/.test(url.pathname)){
     if(req.method()==='PUT'){saved=req.postDataJSON();data={};}
     else {if(hold){hold=false;await new Promise(resolve=>{release=resolve;});}data=dishes[Number(url.pathname.split('/').pop())-1];}
    }else if(url.pathname==='/api/merchant/dish-categories')data=[{categoryId:1,name:'家常菜'}];
    else if(url.pathname==='/api/merchant/ingredients')data=[];
    else if(url.pathname.includes('notifications'))data={items:[],total:0};
    await route.fulfill({json:{code:0,data}});
   });
   await page.goto('http://127.0.0.1:5184/dishes',{waitUntil:'networkidle'});
   await page.getByRole('button',{name:'编辑',exact:true}).first().click();
   await page.locator('.step-images__preview').first().waitFor();
   const first=page.locator('.step-images').first();
   await first.locator('input[type=file]').setInputFiles([1,2,3,4].map(i=>({name:`photo${i}.png`,mimeType:'image/png',buffer:png})));
   await page.waitForFunction(()=>document.querySelectorAll('.step-images')[0]?.querySelectorAll('.step-images__preview').length===5);
   assert.equal(await first.locator('input[type=file]').count(),0);
   await first.getByRole('button',{name:'删除',exact:true}).first().click();
   await first.locator('input[type=file]').setInputFiles({name:'replace.png',mimeType:'image/png',buffer:png});
   await page.waitForFunction(()=>document.querySelectorAll('.step-images')[0]?.querySelectorAll('.step-images__preview').length===5);
   await first.locator('.step-images__preview').first().click();
   await page.getByRole('dialog',{name:'步骤图片预览'}).waitFor();await page.getByRole('button',{name:'关闭预览 ×'}).click();
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,`web overflow ${width}`);
   await page.screenshot({path:path.join(out,`web-editor-${width}.png`),fullPage:true});
   await page.getByRole('button',{name:'保存菜品',exact:true}).click();
   await page.waitForFunction(()=>!document.querySelector('.stack-form button[type=submit]')?.disabled);
   assert.equal(saved.cookingSteps[0].imageUrls.length,5);assert.deepEqual(saved.cookingSteps[1].imageUrls,[]);
   // Force a stale dish response after a second editor has loaded.
   hold=true;await page.getByRole('button',{name:'编辑',exact:true}).first().click();
   await page.getByText('正在加载菜品详情…').waitFor();
   await page.getByRole('button',{name:'编辑',exact:true}).nth(1).click();
   await page.locator('.stack-form input').first().waitFor();
   assert.equal(await page.locator('.stack-form input').first().inputValue(),'测试菜品2');
   release();await page.waitForLoadState('networkidle');
   assert.equal(await page.locator('.stack-form input').first().inputValue(),'测试菜品2');
   assert.deepEqual(errors,[]);
   console.log(JSON.stringify({surface:'web',width,uploadDeletePreviewSave:true,staleEditorGuard:true,fixtureOnly:true}));await page.close();
  }
 }finally{await browser.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
