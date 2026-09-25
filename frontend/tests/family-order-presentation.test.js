const test = require('node:test');
const assert = require('node:assert/strict');
const { summarizeFamilyOrders, presentOrder, presentCartTime, filterFamilyOrders } = require('../utils/family-order-presentation');

test('family summaries preserve backend states and count only the server month', () => {
  const orders = ['PENDING','CONFIRMED','PREPARING','READY','DONE','CANCELLED'].map((status, i) => ({ orderId:i, status, serviceDate:'2026-09-12' }));
  orders.push({status:'DONE',serviceDate:'2026-08-12'});
  assert.deepEqual(summarizeFamilyOrders(orders,'2026-09-12'), {monthlyCount:6,pendingCount:1,preparingCount:3,completedCount:2});
  assert.equal(filterFamilyOrders(orders,'preparing').length,3);
  assert.equal(filterFamilyOrders(orders,'all').length,7);
});
test('order presentation retains snapshots and uses available real images', () => {
  const source = {orderId:2,status:'READY',serviceDate:'2026-09-12',expectedMealTime:'2026-09-12T18:30:00',totalAmount:68,items:[{dishId:3,dishName:'旧菜名',quantity:2}]};
  const row=presentOrder(source,[{dishId:3,name:'新菜名',imageUrl:'/uploads/3.jpg'}],'http://localhost:8080');
  assert.equal(row.timeText,'18:30');
  assert.equal(row.status,'READY');
  assert.equal(row.dishCount,1);
  assert.equal(row.quantity,2);
  assert.equal(row.previewImages[0].url,'http://localhost:8080/uploads/3.jpg');
  assert.equal(row.amountText,'¥68.00');
  assert.equal(source.items[0].dishName,'旧菜名');
  assert.equal(presentOrder({...source,expectedMealTime:null}).timeText,'时间未记录');
});
test('expired cart time cannot be shown as todays selected reservation', () => {
  const cart={serverDate:'2026-09-12',minimumExpectedMealTime:'2026-09-12T17:15:00',expectedMealTime:'2026-08-29T18:45:00'};
  assert.equal(presentCartTime(cart).valid,false);
  assert.equal(presentCartTime(cart).expectedMealClockText,'待选择');
  assert.equal(presentCartTime({...cart,expectedMealTime:'2026-09-12T18:30:00'}).valid,true);
  assert.equal(presentCartTime({...cart,expectedMealTime:'2026-09-12T16:30:00'}).valid,false);
});
