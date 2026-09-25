/* Deterministic data for visual QA only; never part of the Mini Program bundle. */
const {buildProfileMenuGroups}=require('../utils/profile-menu');
const paths={
 'feedback-create':'pages/account/feedback-create/index','feedback-list':'pages/account/feedback-list/index','feedback-detail':'pages/account/feedback-detail/index',
 menu:'pages/ordering/menu/index',profile:'pages/account/profile/index',
 'dish-detail':'pages/ordering/dish-detail/index','profile-edit':'pages/account/profile-edit/index',
 'account-security':'pages/account/account-security/index','account-management':'pages/account/account-management/index',
 notifications:'pages/account/notifications/index','family-management':'pages/family/family-management/index',
 addresses:'pages/family/addresses/index','address-edit':'pages/family/address-edit/index',
 wallet:'pages/family/wallet/index','wallet-ledger':'pages/family/wallet-ledger/index','family-start':'pages/family/family-start/index',
 'order-detail':'pages/ordering/order-detail/index'
};
function fixture(kind,menu){
 const context={family:{name:'老祁家'},member:{name:'小林'}};
 const common={phase:'ready',loading:false,saving:false,mutationBusy:false,context};
 const dishes=menu.map((d,i)=>({...d,id:d.dishId,displayImageUrl:d.imageUrl,priceText:'¥'+d.price,description:d.description||'慢火炖煮，暖心暖胃',selectedByCurrentMemberCount:i===0?1:0,featured:i===0}));
 const tx={id:1,title:'家庭订单结算',note:'订单 #20260912001',amountText:'−¥68.00',amountClass:'expense',statusText:'已完成',createdAt:'2026-09-12 18:30'};
 const address={id:1,contactName:'老祁',phone:'138****5678',address:'祁家小区 3栋 2单元 1203',isDefault:true,tagText:'默认地址'};
 const scenes={
 'feedback-create':{type:'BUG',content:'在餐篮修改数量后，希望能更清楚地看到更新后的总价。\n操作步骤：进入餐篮，点击加号，再查看合计。',choosing:false,uncertain:false,images:Array.from({length:6},(_,i)=>({key:String(i),path:menu[i%3].imageUrl,state:i===1?'failed':'ready',error:i===1?'网络暂时不可用，请重试':''}))},
 'feedback-list':{total:3,page:1,error:'',items:['OPEN','PROCESSING','RESOLVED'].map((status,i)=>({feedbackId:i+1,typeLabel:i===1?'功能建议':'BUG 反馈',statusLabel:['待处理','处理中','已解决'][i],content:['餐篮数量更新后，合计金额没有及时变化。','希望可以在点菜页面更方便地查看滋补介绍。','订单详情的长备注显示不完整。'][i],timeLabel:'2026-09-21 12:30'}))},
 'feedback-detail':{error:'',detail:{typeLabel:'BUG 反馈',statusLabel:'已解决',timeLabel:'2026-09-21 12:30',content:'在餐篮修改数量后，合计金额没有及时变化。\n重新进入页面后恢复正常。',reply:'感谢你的反馈，已修复数量变化后的金额刷新问题。请更新到最新版后重试。'},images:[{imageId:'one',path:menu[0].imageUrl}]},
 menu:{crew:{helperLabel:'阿禾帮厨'},currentDate:'9月12日',currentMemberName:'小林',expectedMealTimeText:'今天 18:30',searchKeyword:'',activeCategoryLabel:'全部好菜',resultSummaryText:'共 6 道',categoryOptions:[{key:'hot',label:'家常热菜',activeClass:'active'},{key:'green',label:'时令蔬菜'},{key:'soup',label:'暖心汤羹'}],menuSections:[{key:'hot',anchorId:'hot',label:'家常热菜',cards:[dishes[0],{...dishes[0],id:4,name:'红烧肉'}]},{key:'green',anchorId:'green',label:'时令蔬菜',cards:[dishes[1],{...dishes[1],id:5,name:'清炒小白菜'}]},{key:'soup',anchorId:'soup',label:'暖心汤羹',cards:[dishes[2],{...dishes[2],id:6,name:'冬瓜排骨汤'}]}]},
 profile:{accountProfile:{nickname:'小林',username:'xiaolin',mobile:'138****5678',emailVerified:true},avatarInitial:'林',sessionLabel:'家庭成员',isUnbound:false,crew:{tasterWelcomeLabel:'小林试吃员，欢迎回家'},summaryCards:[{value:2}],menuGroups:buildProfileMenuGroups({familyId:2,familyRole:'OWNER'})},
 'dish-detail':{scene:{dish:{...dishes[0],category:'家常热菜',finalPrice:38,familyName:'老祁家',mealLabel:'今天 18:30',selectedCount:3,myQuantity:1,tasteTags:['慢炖','酸甜'],ingredients:[{id:1,name:'牛腩',quantity:300,unit:'克'},{id:2,name:'番茄',quantity:2,unit:'个'}]}}},
 'profile-edit':{avatarInitial:'林',mobileBindingEnabled:true,form:{nickname:'小林',avatarUrl:'',mobile:'13800138000'}},
 'account-security':{profile:{maskedEmail:'xi***@example.com',wechatBound:true},mobileBindingEnabled:false,emailBindingEnabled:true,wechatBindingEnabled:true,email:'',emailCode:'',currentPassword:'',newPassword:''},
 'account-management':{currentUserId:2,switchingKey:'',accounts:[{userId:2,avatarInitial:'林',displayName:'小林',username:'xiaolin',roleLabel:'家庭负责人 · 商户负责人',isCurrent:true,hasFamilyMode:true,hasMerchantMode:true,familyUsing:true}]},
 notifications:{actorType:'account',unreadCount:1,unreadItems:[{id:1,title:'晚餐已开始制作',categoryLabel:'订单消息',content:'主厨已经接单，正用心准备你们的晚餐。',createdAt:'2026-09-12 17:30'}],readItems:[]},
 'family-management':{isOwner:true,isAdmin:true,info:{familyName:'老祁家',note:'少盐少油，孩子也吃',merchantName:'老祁私厨',deliveryEnabled:true,deliveryFree:true},defaultAddress:{contactName:'老祁',contactPhone:'138****5678',addressText:address.address},ownerLabel:'当前负责人：小林',ownerOptions:[{label:'阿禾',id:3}],invitations:[],applications:[],selectedOwner:null,editingProfile:false},
 addresses:{addresses:[address,{...address,id:2,contactName:'小林',address:'星河路 18号 2栋 502',isDefault:false,tagText:'常用'}],busyAddressMap:{}},
 'address-edit':{id:1,form:{contactName:'老祁',phone:'13800138000',address:address.address,isDefault:true}},
 wallet:{balanceCards:[{key:'a',label:'可用余额',value:'¥268.00',note:'全家共享'},{key:'b',label:'冻结金额',value:'¥68.00',note:'订单预留'},{key:'c',label:'家庭总额',value:'¥336.00',note:'含冻结金额'}],latestTransactions:[tx]},
 'wallet-ledger':{transactions:[tx,{...tx,id:2,title:'家庭余额充值',amountText:'+¥300.00',note:'商户调整'}]},
 'family-start':{merchantMode:'INVITATION',familyName:'',invitationCode:'',merchantInvitationCode:'',familyApplication:null,joinApplication:null},
 'order-detail':{scene:{order:{statusLabel:'制作中',familyName:'老祁家',mealLabel:'今天 18:30',deliveryMode:'delivery',orderNo:'20260912001',submitterName:'小林',note:'少盐少油'},timeline:[],items:[{id:1,dishName:'番茄炖牛腩',quantity:3,amount:'114.00',hasSelectionDetails:true}],canCancel:false},expandedDishIds:{}}
 };
 Object.assign(scenes,{'auth-login':{loading:false,form:{username:'',password:''}},'auth-register':{loading:false,form:{username:'',password:'',name:'',mobile:''}}});
 const productTypes = [{value:'',label:'全部类型'},...require('../utils/nourishment').PRODUCT_TYPES];
 if (kind === 'dish-detail') {
  const sample = {productType:'NOURISHMENT',nourishmentDescription:'银耳与莲子慢炖，口感软糯。\n这里展示商户填写的食品特点。',servingAdvice:'温热食用，按需取量。',precautions:'对相关食材过敏者请勿食用。'};
  Object.assign(scenes[kind].scene.dish, sample, {nourishmentSections:require('../utils/nourishment').buildNourishmentSections(sample)});
 }
 return {...common,productTypes,productTypeIndex:0,...scenes[kind]};
}
Object.assign(paths,{'auth-login':'pages/auth/entry/index','auth-register':'pages/auth/register/index'});
module.exports={paths,fixture};
