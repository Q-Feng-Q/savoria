const paths={
 'merchant-home':'pages/merchant/index',
 'merchant-dishes':'pages/merchant/merchant-dishes/index',
 'merchant-orders':'pages/merchant/merchant-orders/index',
 'merchant-families':'pages/merchant/merchant-families/index',
 'merchant-edit':'pages/merchant/merchant-profile-edit/index',
 'merchant-dish-edit':'pages/merchant/dish-edit/index',
 'merchant-family-menu':'pages/merchant/family-menu/index'
};
function fixture(kind,menu){
 const regionStates=Object.fromEntries(['profile','orders','families','purchase','menu'].map(k=>[k,{phase:'ready'}]));
 const order={id:1,familyName:'老祁家',rawStatus:'PREPARING',statusLabel:'制作中',mealLabel:'今天 18:30',deliveryMode:'delivery',itemCountText:'共3道菜 · 4份',chargeSummaryText:'家庭钱包 · ¥114.00',nextActionText:'制作完成'};
 const scenes={
 'merchant-home':{todayLabel:'9月12日',context:{merchant:{name:'老祁私厨'}},regionStates,todayCommand:{activeOrderCount:3},familyCount:8,todayPurchaseItemCount:12},
 'merchant-dishes':{scope:'available',query:'',statusFilter:'all',ingredientCount:28,reviewEnabled:true,selectedDishIds:[],allVisibleSelected:false,busyDishMap:{},syncBusyDishMap:{},filteredDishRows:menu.map((d,i)=>({...d,id:d.dishId,category:i===1?'时令蔬菜':'家常热菜',statusText:'已上架',priceText:'¥'+d.price,featuredLabel:i===0?'已推荐':'推荐',toggleLabel:'下架',featuredDisabled:false,templateImported:i===0}))},
 'merchant-orders':{visibleOrders:[order],orderGroups:[{serviceDate:'2026-09-12',orders:[order,{...order,id:2,familyName:'林家小院',rawStatus:'PENDING',statusLabel:'待确认',nextActionText:'确认接单'}]}],familyOptions:[{label:'全部家庭'}],familyIndex:0,statusIndex:0,statusOptions:[{value:'all',label:'全部'},{value:'PENDING',label:'待确认'},{value:'PREPARING',label:'制作中'},{value:'DONE',label:'已完成'}],purchaseItemCount:12,purchaseSourceText:'按已确认订单汇总',busyOrderMap:{}},
 'merchant-families':{familyCount:2,families:[{id:1,name:'老祁家',serviceStateText:'服务中',memberCountText:'4位成员',addressCountText:'2个地址',defaultAddressText:'祁家小区 3栋 2单元 1203',deliveryText:'支持配送 · 免配送费',menuStatus:'ready',menuStatusText:'已启用 12 道菜'},{id:2,name:'林家小院',serviceStateText:'服务中',memberCountText:'3位成员',addressCountText:'1个地址',defaultAddressText:'星河路 18号 502',deliveryText:'到店自取',menuStatus:'ready',menuStatusText:'已启用 8 道菜'}]},
 'merchant-edit':{editable:true,form:{name:'老祁私厨',contactName:'老祁',contactPhone:'13800138000'}},
 'merchant-dish-edit':{editTitle:'编辑菜品',dish:{name:'番茄炖牛腩',description:'慢火炖煮，酸甜浓郁',basePrice:38,imageUrl:menu[0].imageUrl,ingredients:[{id:1,name:'牛腩',quantity:300,unit:'克',calculationType:'按份'}],cookingSteps:[]},dishCategories:['家常热菜'],categoryIndex:0,selectedCategoryLabel:'家常热菜',ingredients:[],selectedIngredientName:'选择食材',selectedCalculationLabel:'按份计算',calculationTypes:[],statusLabel:'已上架',hasCookingSteps:false},
 'merchant-family-menu':{currentFamilyName:'老祁家',familyOptions:[{label:'老祁家'}],familyIndex:0,sourceFamilyOptions:[],selectedCount:0,allSelected:false,selectedDishMap:{},menuRows:menu.map(d=>({id:d.dishId,name:d.name,category:'家常菜',basePriceText:'¥'+d.price,enabledText:'已启用',enabled:true,price:d.price}))}
 };
 const ingredients=[{id:1,ingredientName:'牛腩',quantity:300,unit:'克',key:'beef',text:'300 克'},{id:2,ingredientName:'番茄',quantity:2,unit:'个',key:'tomato',text:'2 个'}];
 Object.assign(scenes,{
  'merchant-templates':{keyword:'',categories:[{categoryId:0,name:'全部'},{categoryId:1,name:'家常热菜'},{categoryId:2,name:'时令蔬菜'},{categoryId:3,name:'暖心汤羹'},{categoryId:4,name:'主食点心'}],categoryId:0,imported:'',selectedIds:[1],total:3,hasMore:false,items:menu.map((d,i)=>({...d,templateId:d.dishId,categoryName:'家常菜',ingredientCount:3,tagsText:'清淡',priceText:'¥'+d.price,selected:i===0,imported:i===2}))},
  'merchant-template-detail':{detail:{...menu[0],categoryName:'家常热菜',referencePrice:38,tasteText:'酸甜浓郁',mealText:'家常菜',ingredients,imageAuthor:'系统菜库',imageLicense:'示例图片'}},
  'merchant-change-detail':{detail:{requestId:12,templateName:'番茄炖牛腩',submittedText:'2026-09-12 10:30',baseTemplateVersion:2,currentTemplateVersion:2,statusTone:'pending',statusLabel:'待审核',submitNote:'补充少盐做法与食材说明',canWithdraw:true},comparison:[{key:'name',label:'菜名',baseText:'番茄炖牛腩',targetText:'番茄炖牛腩',changed:false},{key:'description',label:'描述',baseText:'酸甜浓郁',targetText:'小火慢炖，少盐少油，适合全家一起享用。',changed:true}],baseIngredients:ingredients,targetIngredients:ingredients},
  'merchant-purchase':{date:'2026-09-12',mealTypes:[{label:'全部'}],mealIndex:0,mealLabel:'全部',sourceSummary:{text:'根据已确认订单汇总'},tempItems:[{itemId:1,ingredientName:'葱',quantity:2,unit:'把',remark:'新鲜小葱'}],busyItemMap:{},items:[{ingredientId:1,ingredientName:'牛腩',quantityText:'1.2 千克',statusText:'待采购',familyText:'2个家庭',mealText:'今日预约',dishText:'番茄炖牛腩',breakdown:[]}]},
  'merchant-order-detail':{scene:{order:{...order,orderNo:'20260912001',submitterName:'小林',note:'少盐，孩子也吃',deliveryFee:0},items:[{id:1,dishName:'番茄炖牛腩',quantity:3,amount:'114.00'}],familyDeliverySummary:'支持配送 · 免配送费',currentDefaultAddressText:'祁家小区 3栋 1203',charges:[],timeline:[{label:'订单已确认',time:'17:30',note:'主厨正在备餐'}],canAdvance:true,nextActionText:'制作完成',canCancel:true},expandedDishIds:{}}
 });

 const productTypes = require('../utils/nourishment').PRODUCT_TYPES;
 if(kind==='merchant-dish-edit') Object.assign(scenes[kind].dish,{productType:'NOURISHMENT',nourishmentDescription:'慢炖制作，介绍食材与口感特点。',servingAdvice:'温热食用。',precautions:''});
 return {phase:'ready',saving:false,uploading:false,mutationBusy:false,importing:false,importingAll:false,withdrawing:false,purchaseBusy:false,loadingMore:false,productTypeIndex:0,productTypes:kind==='merchant-dish-edit'?productTypes:[{value:'',label:'全部类型'},...productTypes],...scenes[kind]};
}
Object.assign(paths,{
 'merchant-templates':'pages/merchant/dish-templates/index',
 'merchant-template-detail':'pages/merchant/dish-template-detail/index',
 'merchant-change-detail':'pages/merchant/dish-template-change-detail/index',
 'merchant-purchase':'pages/merchant/purchase/index',
 'merchant-order-detail':'pages/merchant/merchant-order-detail/index'
});

module.exports={paths,fixture};
