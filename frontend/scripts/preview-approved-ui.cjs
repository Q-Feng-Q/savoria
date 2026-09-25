/* Local visual QA: render production WXML/WXSS with deterministic sample data.
   This does not replace WeChat compiler or device checks. Never used in app bundle. */
const fs=require('fs'),path=require('path'),http=require('http');
const deps=process.env.KITCHEN_NODE_DEPS || 'C:/Users/Q/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';
const {xml2js}=require(path.join(deps,'xml-js'));
const root=path.resolve(__dirname,'..');
const {buildApiHomeScene,buildApiOrdersScene,buildApiCartScene}=require('../utils/api-scenes');
const {summarizeFamilyOrders}=require('../utils/family-order-presentation');
const subpages=require('./story-subpage-fixtures.cjs'); const merchantPages=require('./merchant-story-fixtures.cjs');
const menu=[{dishId:1,name:'番茄炖牛腩',price:38,description:'酸甜浓郁 · 全家都爱',imageUrl:'/images/dish-templates/dish-001.jpg'},{dishId:2,name:'蒜蓉时蔬',price:18,description:'清爽鲜嫩',imageUrl:'/images/dish-templates/dish-002.jpg'},{dishId:3,name:'山药排骨汤',price:28,imageUrl:'/images/dish-templates/dish-003.jpg'}];
const home={serviceDate:'2026-09-12',family:{familyId:2,familyName:'老祁家',merchantName:'老祁',deliveryEnabled:true},member:{memberId:2,name:'小林'},crew:{chefName:'老祁',helperName:'阿禾',tasterName:'小林'},featuredDishes:menu.slice(0,2),dashboardCards:[]};
const orders=['PREPARING','DONE','PENDING'].map((status,i)=>({orderId:20260912001-i,status,serviceDate:'2026-09-12',expectedMealTime:'2026-09-12T'+['18:30:00','12:00:00','17:30:00'][i],totalAmount:[68,76,62][i],items:menu.map(dish=>({dishId:dish.dishId,dishName:dish.name,quantity:1,price:dish.price}))}));
const cart={cartId:1,version:1,serverDate:'2026-09-12',minimumExpectedMealTime:'2026-09-12T17:15:00',expectedMealTime:'2026-09-12T18:30:00',timeStepMinutes:15,totalAmount:56,remark:'少盐，孩子也吃',items:menu.slice(0,2).map(dish=>({itemId:dish.dishId,dishId:dish.dishId,dishName:dish.name,price:dish.price,quantity:1,currentMemberQuantity:1,available:true,selections:[]}))};
const addresses=[{addressId:1,contactName:'老祁',phone:'138****5678',address:'祁家小区 3栋 2单元 1203',isDefault:true}];
const pages={home:'pages/family/home/index',cart:'pages/ordering/cart/index',orders:'pages/ordering/orders/index'};
Object.assign(pages,subpages.paths,merchantPages.paths);
const escape=s=>String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/"/g,'&quot;');
function expr(source,scope){try{return Function('s','with(s){return ('+source+')}')(scope);}catch{return '';}}
function value(source,scope){if(typeof source!=='string')return source;const exact=source.match(/^\{\{([^{}]*)\}\}$/);if(exact)return expr(exact[1],scope);return source.replace(/\{\{([\s\S]*?)\}\}/g,(_,e)=>expr(e,scope)??'');}
function parse(file){let text=fs.readFileSync(path.join(root,file),'utf8').replace(/&/g,'&amp;').replace(/\s(wx:else|scroll-y|scroll-x|enhanced|block|password|compact|selectable)(?=[\s/>])/g,' $1=""');return xml2js('<root>'+text+'</root>',{compact:false}).elements[0].elements;}
function render(nodes,scope){let result='',chain=false;
 for(const n of nodes || []){
  if(n.type==='text'){result+=escape(value(n.text,scope)).replace(/\\n/g,'<br>');continue;}
  if(n.type!=='element')continue;
  const a=n.attributes || {};
  if('wx:if' in a){chain=Boolean(value(a['wx:if'],scope));if(!chain)continue;}
  else if('wx:elif' in a){if(chain)continue;chain=Boolean(value(a['wx:elif'],scope));if(!chain)continue;}
  else if('wx:else' in a){if(chain)continue;chain=true;}
  else chain=false;
  if(a['wx:for']){const list=value(a['wx:for'],scope)||[];list.forEach((item,index)=>{const c={...a};delete c['wx:for'];delete c['wx:if'];delete c['wx:else'];result+=render([{...n,attributes:c}],{...scope,[a['wx:for-item']||'item']:item,[a['wx:for-index']||'index']:index});});continue;}
  if(n.name==='block'){result+=render(n.elements,scope);continue;}
  if(n.name==='step-images'){
   result+=render(parse('components/step-images/index.wxml'),{editable:Boolean(value(a.editable,scope)),disabled:false,busy:false,previews:value(a.images,scope)||[]});continue;
  }
  if(['dish-row','quantity-stepper','action-button','group-menu','status-timeline','merchant-workbench-nav'].includes(n.name)){
   const props={value:0,min:0,max:99,disabled:false,loading:false,block:false,tone:'primary',loadingLabel:'处理中…'};
   for(const [k,v]of Object.entries(a))props[k]=v===''?true:value(v,scope);
   result+=render(parse('components/'+n.name+'/index.wxml'),props);continue;
  }
  if(n.name==='order-row'){result+='<div class="preview-order-host">'+render(parse('components/order-row/index.wxml'),{order:value(a.order,scope)})+'</div>';continue;}
  if(n.name==='bottom-action-bar'){const props={loading:false};for(const [k,v]of Object.entries(a))props[k]=value(v,scope);result+=render(parse('components/bottom-action-bar/index.wxml'),props);continue;}
  if(n.name==='page-state'){result+='<div class="preview-empty">'+escape(value(a.title,scope))+'</div>';continue;}
  if(n.name==='slot')continue;
  const tag={view:'div',text:'span',image:'img',picker:'div','scroll-view':'div',swiper:'div','swiper-item':'div',label:'div',textarea:'textarea',input:'input'}[n.name]||'div';
  let attrs='';
  for(const [k,v] of Object.entries(a)){if(['class','src','id','placeholder','aria-label','aria-role','value'].includes(k))attrs+=' '+k+'="'+escape(value(v,scope))+'"';}
  if(n.name==='image')attrs+=' style="object-fit:'+(a.mode==='aspectFill'?'cover':a.mode==='aspectFit'?'contain':'fill')+'"';
  if(n.name==='swiper')attrs+=' data-swiper="true"';
  if(n.name==='scroll-view' && 'scroll-x' in a)attrs+=' style="overflow-x:auto"';
  result+='<'+tag+attrs+'>'+ (tag==='img'?'':tag==='textarea'?escape(value(a.value,scope)):render(n.elements,scope))+(tag==='img'?'':'</'+tag+'>');
 }return result;}
function css(file,width,seen=new Set()){if(seen.has(file))return '';seen.add(file);return fs.readFileSync(path.join(root,file),'utf8').replace(/@import\s+"([^"]+)";/g,(_,p)=>css(path.posix.normalize(path.posix.join(path.posix.dirname(file),p)),width,seen)).replace(/(-?\d*\.?\d+)rpx/g,(_,n)=>(Number(n)*width/750)+'px').replace(/(^|[}\s])page\s*\{/g,'$1body {').replace(/:host\s*\{/g,'.preview-order-host {');}
function output(kind,width,disabled=false){
 const selectedHome={...home,family:{...home.family,deliveryEnabled:!disabled}};
 let data=kind==='home'?buildApiHomeScene(selectedHome,{orders,cart,windowWidth:width}):kind==='cart'?buildApiCartScene({homeData:selectedHome,cart,addresses,deliveryMode:'DELIVERY'}):{...buildApiOrdersScene({homeData:selectedHome,orders,menuItems:menu}),...summarizeFamilyOrders(orders,home.serviceDate),statusIndex:0,statusOptions:[{key:'all',label:'全部订单'}]};
 data={...data,phase:'ready',expandedDishIds:{},mutationBusy:false};
 if(kind==='orders')data.visibleOrders=data.orders;
 if(subpages.paths[kind])data=subpages.fixture(kind,menu);
 if(merchantPages.paths[kind])data=merchantPages.fixture(kind,menu);
 if(kind==='merchant-dish-edit'||kind==='dish-detail'){
  const dish=kind==='dish-detail'?data.scene.dish:data.dish;
  dish.cookingSteps=[{stepNo:1,title:'处理食材',content:'将食材清洗干净，切块备用。',imageUrls:Array(5).fill('/assets/brand/dish-placeholder.png')},{stepNo:2,title:'慢火炖煮',content:'煮至软烂后盛出。',imageUrls:[]}];
  data.hasCookingSteps=true;
 }
 let styles=css('app.wxss',width)+css(pages[kind]+'.wxss',width)+['order-row','bottom-action-bar','dish-row','quantity-stepper','action-button','group-menu','status-timeline'].map(c=>css('components/'+c+'/index.wxss',width)).join('')+'.menu-page{height:calc(100vh - 104px)!important;min-height:0!important}.category-rail,.dish-list-scroll{overflow-y:auto}.preview-top{font-size:16px}input{font-family:inherit}';
 if(merchantPages.paths[kind])styles=css('app.wxss',width)+css(pages[kind]+'.wxss',width)+['merchant-workbench-nav','action-button','status-timeline'].map(c=>css('components/'+c+'/index.wxss',width)).join('');
 styles=styles.replace(/(^|[\s>,])(text|view|image)(?=[\s.#:\[>+~,{])/g,(_,lead,tag)=>lead+({text:'span',view:'div',image:'img'}[tag]));
 styles+=css('components/step-images/index.wxss',width);
 const chromeCss=['home','cart','orders','menu','profile'].includes(kind)?'':'.preview-nav{display:none!important}body{padding-bottom:0!important}.bottom-action-bar{bottom:16px!important}';
 const nav=JSON.parse(fs.readFileSync(path.join(root,'app.json'))).tabBar.list.map((tab,index)=>'<div><img src="/'+tab.iconPath+'"><span>'+tab.text+'</span></div>').join('');
 return '<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>源码预览 · '+kind+'</title><style>'+styles+chromeCss+'body{margin:0;padding:44px 0 60px;font-family:"Microsoft YaHei",sans-serif}div,span,img{box-sizing:border-box;min-width:0}img{vertical-align:middle}textarea{font-family:inherit;resize:none}span{white-space:pre-line}.preview-nav{height:60px;position:fixed;bottom:0;left:0;right:0;display:flex;background:#fff2d7;z-index:100;border-top:1px solid #e9d5b0}.preview-nav>div{flex:1;text-align:center;padding:7px 0;font-size:11px;color:#806746}.preview-nav img{display:block;width:24px;height:24px;margin:0 auto 3px}.preview-top{height:44px;position:fixed;top:0;left:0;right:0;z-index:101;display:flex;align-items:center;justify-content:center;background:#fff3d7;color:#4b301d;font-size:17px}.preview-top b{position:absolute;right:12px;border:1px solid #e4d6bb;border-radius:20px;padding:3px 12px;font-size:16px}.home-featured-swiper{display:flex;overflow:hidden}.home-featured-slide{min-width:94%;flex-shrink:0}.preview-order-host{display:block}.preview-empty{padding:45px 10px;color:#90754f;text-align:center}.bottom-action-bar{bottom:70px} .story-page{min-height:calc(100vh - 104px)}</style><div class="preview-top">'+({home:'首页',orders:'订单进度',cart:'餐篮',menu:'去点餐',profile:'我的'}[kind]||(merchantPages.paths[kind]?'商户工作台':'家庭服务'))+'<b>•••　◎</b></div>'+render(parse(pages[kind]+'.wxml'),data)+'<div class="preview-nav">'+nav+'</div>';
}
const server=http.createServer((req,res)=>{
 const url=new URL(req.url,'http://localhost');
 if(url.pathname.startsWith('/assets/')||url.pathname.startsWith('/images/')){
  const base=url.pathname.startsWith('/assets/')?root:path.resolve(root,'../backend/src/main/resources/static');
  const file=path.resolve(base,'.'+decodeURIComponent(url.pathname));
  if(!file.startsWith(base+path.sep)||!fs.existsSync(file)){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',file.endsWith('.jpg')?'image/jpeg':file.endsWith('.webp')?'image/webp':'image/png');fs.createReadStream(file).pipe(res);return;
 }
 const kind=url.searchParams.get('page')||'home';
 if(!pages[kind]){res.writeHead(404);res.end();return;}
 res.setHeader('Content-Type','text/html;charset=utf-8');res.end(output(kind,Number(url.searchParams.get('width'))||375,url.searchParams.has('disabled')));
});
const port=Number(process.env.KITCHEN_PREVIEW_PORT)||5179;
server.listen(port,'127.0.0.1',()=>console.log(`Production source visual QA: http://127.0.0.1:${port}/?page=home&width=375`));
