const { toImageUrl } = require('./image-url');
const GROUPS = {pending:['PENDING'],preparing:['CONFIRMED','PREPARING','READY'],completed:['DONE']};
function filterFamilyOrders(orders = [], key = 'all') {
  return GROUPS[key] ? orders.filter(order => GROUPS[key].includes(order.status)) : orders;
}
function summarizeFamilyOrders(orders = [], serviceDate = '') {
  const month = String(serviceDate).slice(0,7);
  return {
    monthlyCount: month ? orders.filter(order => String(order.serviceDate || order.expectedMealTime || '').startsWith(month)).length : 0,
    pendingCount: filterFamilyOrders(orders,'pending').length,
    preparingCount: filterFamilyOrders(orders,'preparing').length,
    completedCount: filterFamilyOrders(orders,'completed').length
  };
}
function dateLabel(value) {
  const match=String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) return '日期未记录';
  const date=new Date(Number(match[1]),Number(match[2])-1,Number(match[3]));
  return Number(match[2])+'月'+Number(match[3])+'日 (周'+'日一二三四五六'[date.getDay()]+')';
}
function presentOrder(order, menuItems = [], imageBaseUrl = '') {
  const time=String(order.expectedMealTime || '').match(/[T ](\d{2}:\d{2})/);
  const items=order.items || [];
  const tone=order.status==='DONE'?'done':order.status==='READY'?'ready':['CANCELLED','REJECTED'].includes(order.status)?'muted':'pending';
  return {
    status:order.status, statusTone:tone,
    dateText:dateLabel(order.expectedMealTime || order.serviceDate),
    timeText:time?time[1]:'时间未记录',
    amountText:'¥'+Number(order.totalAmount || 0).toFixed(2),
    dishCount:new Set(items.map(item=>String(item.dishId))).size,
    quantity:items.reduce((sum,item)=>sum+Number(item.quantity || 0),0),
    previewImages:items.slice(0,3).map((item,index)=>{
      const dish=menuItems.find(dish=>String(dish.dishId)===String(item.dishId)) || {};
      const source=item.imageUrl || dish.imageUrl;
      return {key:String(item.dishId)+'-'+index,url:source?toImageUrl(imageBaseUrl,source):'/assets/brand/dish-placeholder.png'};
    })
  };
}
function presentCartTime(cart = {}, fallbackDate = '') {
  const day=cart.serverDate || fallbackDate;
  const selected=String(cart.expectedMealTime || '');
  const minimum=String(cart.minimumExpectedMealTime || '');
  const valid=Boolean(selected && (!day || selected.slice(0,10)===day) && (!minimum || selected>=minimum) && !cart.bookingEnded);
  return {valid, expectedMealDateText:dateLabel(day || selected), expectedMealClockText:valid?selected.slice(11,16):'待选择'};
}
module.exports={summarizeFamilyOrders,filterFamilyOrders,presentOrder,presentCartTime,dateLabel};
