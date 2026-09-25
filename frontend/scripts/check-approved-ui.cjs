const fs=require('fs'),path=require('path');
const {chromium}=require('C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
async function main(){
 const out=path.resolve(__dirname,'../../output/playwright/approved-ui');fs.mkdirSync(out,{recursive:true});
 const browser=await chromium.launch({headless:true});
 const checks=[];
 for(const width of [320,375,430]){
  const page=await browser.newPage({viewport:{width,height:width===320?667:896},deviceScaleFactor:1});
  for(const name of (process.env.KITCHEN_UI_PAGES ? process.env.KITCHEN_UI_PAGES.split(',') : ['home','cart','orders',...Object.keys(require('./story-subpage-fixtures.cjs').paths),...Object.keys(require('./merchant-story-fixtures.cjs').paths)])){
   await page.goto('http://127.0.0.1:5179/?page='+name+'&width='+width,{waitUntil:'networkidle'});
   const result=await page.evaluate(()=>({
    scrollWidth:document.documentElement.scrollWidth, width:innerWidth,
    brokenImages:[...document.images].filter(i=>!i.complete||i.naturalWidth===0).map(i=>i.src),
    bodyText:document.body.innerText,
    delivery:[...document.querySelectorAll('.delivery-option')].map(e=>({text:e.innerText,color:getComputedStyle(e).color,background:getComputedStyle(e).backgroundColor,width:e.getBoundingClientRect().width})),
    cards:[...document.querySelectorAll('.order-row')].map(e=>({top:e.getBoundingClientRect().top,bottom:e.getBoundingClientRect().bottom,radius:getComputedStyle(e).borderRadius}))
   }));
   if(result.scrollWidth>width)throw Error(name+' horizontal overflow at '+width);
   if(result.brokenImages.length)throw Error(name+' broken images '+result.brokenImages);
   if(/\bundefined\b|\bnull\b/.test(result.bodyText))throw Error(name+' null/undefined visible');
   for(let i=1;i<result.cards.length;i++)if(result.cards[i].top-result.cards[i-1].bottom<8)throw Error('Order card spacing too small');
   if(name==='cart'&&!result.bodyText.includes('提交订单'))throw Error('Missing submit label');
   if(name==='merchant-orders'){
    const colors=await page.locator('.family-filter').evaluate(e=>({host:getComputedStyle(e).backgroundColor,label:getComputedStyle(e.querySelector('.filter-picker')).backgroundColor}));
    if(colors.host!=='rgba(0, 0, 0, 0)'||colors.label!=='rgb(227, 232, 199)')throw Error('Family filter white background regression');
   }
   if(name==='auth-login'||name==='auth-register'){
    const label=name==='auth-login'?'登录':'创建账户';
    if(!(await page.locator('.action-button--primary').innerText()).includes(label))throw Error('Missing auth primary label');
    const fields=await page.locator('input').evaluateAll(es=>es.map(e=>({width:e.clientWidth,scroll:e.scrollWidth})));
    if(fields.some(f=>f.scroll>f.width+1))throw Error('Auth field overflow');
   }
   if(name==='menu'){
    const geometry=await page.evaluate(()=>{
     const rail=document.querySelector('.category-rail'),list=document.querySelector('.dish-list-scroll');
     const before=rail.getBoundingClientRect().top;
     list.scrollTop=150;
     return {railStayed:rail.getBoundingClientRect().top===before,scrollable:list.scrollHeight>list.clientHeight,
      glyphs:[...document.querySelectorAll('.quantity-stepper__glyph')].map(e=>({w:e.getBoundingClientRect().width,h:e.getBoundingClientRect().height})),
      controls:[...document.querySelectorAll('.dish-row__action-row')].map(e=>({w:e.clientWidth,scroll:e.scrollWidth}))};
    });
    if(!geometry.railStayed||!geometry.scrollable)throw Error('Menu independent scroll failed');
    if(geometry.glyphs.some(g=>Math.abs(g.w-width*38/750)>1||Math.abs(g.h-width*38/750)>1))throw Error('Stepper glyph size changed');
    if(geometry.controls.some(c=>c.scroll>c.w+1))throw Error('Menu price/actions overflow');
    await page.locator('.dish-list-scroll').evaluate(e=>e.scrollTop=0);
   }
   checks.push({name,width,overflow:false,brokenImages:0,cardGaps:result.cards.slice(1).map((c,i)=>Math.round(c.top-result.cards[i].bottom)),delivery:result.delivery});
   await page.screenshot({path:path.join(out,name+'-'+width+'.png'),fullPage:true});
   await page.screenshot({path:path.join(out,name+'-'+width+'-viewport.png')});
  }
  await page.close();
 }
 const page=await browser.newPage({viewport:{width:375,height:812}});
 await page.goto('http://127.0.0.1:5179/?page=cart&width=375&disabled=1',{waitUntil:'networkidle'});
 if(await page.locator('.address-picker').count())throw Error('Address still shown when delivery disabled');
 if(!(await page.locator('.delivery-option.active').innerText()).includes('自取'))throw Error('Pickup not selected');
 await page.screenshot({path:path.join(out,'cart-delivery-disabled.png'),fullPage:true});
 checks.push({name:'delivery-disabled',addressHidden:true,pickupSelected:true});
 await browser.close();
 fs.writeFileSync(path.join(out,'checks.json'),JSON.stringify(checks,null,2)+'\n');
 console.log(JSON.stringify(checks,null,2));
}
main().catch(error=>{console.error(error);process.exitCode=1;});
