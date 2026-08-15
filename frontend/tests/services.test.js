const test = require('node:test');
const assert = require('node:assert/strict');

const { createAuthService } = require('../services/auth');
const { createFamilyService } = require('../services/family');
const { createCartService } = require('../services/cart');
const { createOrdersService } = require('../services/orders');
const { createNotificationsService } = require('../services/notifications');
const { createMerchantService } = require('../services/merchant');
const { createPurchaseService } = require('../services/purchase');
const { createFilesService } = require('../services/files');
const { createUserService } = require('../services/user');
const { createSystemService } = require('../services/system');
const { toImageUrl } = require('../utils/image-url');

test('toImageUrl preserves absolute urls and prefixes relative urls', () => {
  assert.equal(toImageUrl('http://127.0.0.1:8080', ''), '');
  assert.equal(
    toImageUrl('http://127.0.0.1:8080', '/uploads/images/a.png'),
    'http://127.0.0.1:8080/uploads/images/a.png'
  );
  assert.equal(
    toImageUrl('http://127.0.0.1:8080', 'https://cdn.example.com/a.png'),
    'https://cdn.example.com/a.png'
  );
});

test('auth service calls login endpoints and unwraps data', async () => {
  const calls = [];
  const service = createAuthService({
    request: async (pathname, options) => {
      calls.push({ pathname, options });
      return {
        code: 0,
        message: 'ok',
        data: { accessToken: 'signed-jwt-token' }
      };
    }
  });

  const data = await service.wechatLogin({ code: 'wx-login-code' });
  await service.portalLogin({ username: 'any-user', password: 'secret' });
  await service.userLogin({ username: 'user', password: '123456' });
  await service.adminLogin({ username: 'admin', password: '123456' });
  await service.logout();
  await service.logoutAll();

  assert.equal(calls[0].pathname, '/api/auth/wechat/login');
  assert.equal(calls[0].options.method, 'POST');
  assert.deepEqual(calls[0].options.data, { code: 'wx-login-code' });
  assert.equal(calls[1].pathname, '/api/auth/login');
  assert.deepEqual(calls[1].options.data, { username: 'any-user', password: 'secret' });
  assert.equal(calls[2].pathname, '/api/auth/login');
  assert.deepEqual(calls[2].options.data, { username: 'user', password: '123456' });
  assert.equal(calls[3].pathname, '/api/auth/admin/login');
  assert.equal(calls[3].options.method, 'POST');
  assert.deepEqual(calls[3].options.data, { username: 'admin', password: '123456' });
  assert.equal(calls[4].pathname, '/api/auth/logout');
  assert.equal(calls[5].pathname, '/api/auth/logout-all');
  assert.equal(data.accessToken, 'signed-jwt-token');
});

test('public system settings endpoint is available before login', async () => {
  const calls = [];
  const system = createSystemService({ request: async (pathname, options) => {
    calls.push({ pathname, options }); return { code: 0, data: { siteName: '食光知味' } };
  }});
  assert.equal((await system.getPublicSettings()).siteName, '食光知味');
  assert.equal(calls[0].pathname, '/api/public/system-settings');
  assert.equal(calls[0].options.method, 'GET');
});

test('family and cart services target stable family endpoints', async () => {
  const calls = [];
  const request = async (pathname, options) => {
    calls.push({ pathname, options });
    return { code: 0, message: 'ok', data: [] };
  };

  const family = createFamilyService({ request });
  const cart = createCartService({ request });

  await family.getHome();
  await family.getMealSlots();
  await family.getMenuItems({ categoryId: 3, keyword: '番茄', serviceDate: '2026-07-02', mealSlotId: 20 });
  await family.getDishDetail(100);
  await family.getAddresses();
  await family.createAddress({ contactName: '陈梅' });
  await family.updateAddress(1, { contactName: '陈浩' });
  await family.setDefaultAddress(1);
  await family.getWalletLedgers();
  await family.directInvite({ userIdentifier: 'member@example.com' });
  await family.getMyInvitations();
  await family.acceptInvitation(12);
  await family.rejectInvitation(13, { reason: '暂不加入' });
  await family.getJoinApplications();
  await family.approveJoinApplication(14);
  await family.rejectJoinApplication(15, { reason: '信息不符' });
  await family.transferOwner({ targetUserId: 9 });
  await family.dissolveFamily();
  await cart.getCart({ mealSlotId: 20, date: '2026-07-02' });
  await cart.addItem({ dishId: 100, quantity: 1 });
  await cart.updateItem(9, { quantity: 2 });
  await cart.deleteItem(9);
  await cart.updateRemark({ mealSlotId: 20, date: '2026-07-02', remark: '不要香菜' });

  assert.equal(calls[0].pathname, '/api/family/home');
  assert.equal(calls[1].pathname, '/api/family/meal-slots');
  assert.match(calls[2].pathname, /^\/api\/family\/menu\?/);
  assert.equal(calls[3].pathname, '/api/family/menu/100');
  assert.equal(calls[9].pathname, '/api/family/invitations/direct');
  assert.equal(calls[10].pathname, '/api/family/invitations/me');
  assert.equal(calls[11].pathname, '/api/family/invitations/12/accept');
  assert.equal(calls[12].pathname, '/api/family/invitations/13/reject');
  assert.equal(calls[13].pathname, '/api/family/join-applications');
  assert.equal(calls[16].pathname, '/api/family/owner');
  assert.equal(calls[17].pathname, '/api/family/current');
  assert.equal(calls[18].pathname, '/api/family/cart?mealSlotId=20&date=2026-07-02');
  assert.equal(calls[22].pathname, '/api/family/cart/remark');
  assert.match(calls[2].pathname, /categoryId=3/);
  assert.equal(calls[18].options.data.mealSlotId, 20);
});

test('user service matches the unified identity endpoints', async () => {
  const calls = [];
  const user = createUserService({ request: async (pathname, options) => {
    calls.push({ pathname, options });
    return { code: 0, data: pathname.endsWith('/context')
      ? { userId: 2, merchantId: 2, availableModes: ['family', 'merchant'], permissionCodes: ['MERCHANT_ADMIN'] }
      : {} };
  }});
  const context = await user.getContext();
  await user.getProfile();
  await user.updateProfile({ nickname: '小食光' });
  await user.changeUsername({ username: 'xiaoshiguang' });
  await user.changePassword({ oldPassword: 'a', newPassword: 'b' });
  await user.sendEmailCode({ email: 'a@example.com' });
  await user.bindEmail({ email: 'a@example.com', code: '123456' });
  await user.bindWechat({ code: 'wx-code' });
  await user.unbindWechat();
  await user.requestCancellation({ password: 'secret' });
  await user.cancelCancellation();
  assert.deepEqual(context, {
    userId: 2,
    merchantId: 2,
    availableModes: ['family', 'merchant'],
    permissionCodes: ['MERCHANT_ADMIN']
  });
  assert.deepEqual(calls.map((item) => `${item.options.method} ${item.pathname}`), [
    'GET /api/users/me/context', 'GET /api/users/me', 'PUT /api/users/me',
    'PUT /api/users/me/username', 'PUT /api/users/me/password',
    'POST /api/users/me/email/code', 'PUT /api/users/me/email',
    'POST /api/users/me/wechat', 'DELETE /api/users/me/wechat',
    'POST /api/users/me/cancellation', 'DELETE /api/users/me/cancellation'
  ]);
});

test('orders and notifications services target stable order endpoints', async () => {
  const calls = [];
  const request = async (pathname, options) => {
    calls.push({ pathname, options });
    return { code: 0, message: 'ok', data: {} };
  };

  const orders = createOrdersService({ request });
  const notifications = createNotificationsService({ request });

  await orders.submitOrder({ date: '2026-07-02', mealSlotId: 20 });
  await orders.listOrders();
  await orders.getOrderDetail(88);
  await orders.cancelOrder(88, { reason: '临时不吃了' });
  await notifications.getNotifications({ receiverScope: 'family', readStatus: 'all', page: 1, pageSize: 20 });
  await notifications.markRead(5);
  await notifications.markAllRead({ receiverScope: 'family' });

  assert.equal(calls[0].pathname, '/api/family/orders');
  assert.equal(calls[1].pathname, '/api/family/orders');
  assert.equal(calls[2].pathname, '/api/family/orders/88');
  assert.equal(calls[3].pathname, '/api/family/orders/88/cancel');
  assert.match(calls[4].pathname, /^\/api\/notifications\?/);
  assert.equal(calls[5].pathname, '/api/notifications/5/read');
  assert.equal(calls[6].pathname, '/api/notifications/read-all');
});

test('merchant, purchase and file services target stable merchant endpoints', async () => {
  const calls = [];
  const request = async (pathname, options) => {
    calls.push({ pathname, options });
    return { code: 0, message: 'ok', data: [] };
  };

  const merchant = createMerchantService({ request });
  const purchase = createPurchaseService({ request });
  const files = createFilesService({
    baseUrl: 'http://127.0.0.1:8080',
    getSession: () => ({
      accessToken: 'token-1',
      userId: 1,
      merchantId: 2,
      roleTemplate: 'merchant_admin',
      backendRoles: ['merchant_admin']
    }),
    upload: async (options) => ({
      statusCode: 200,
      data: JSON.stringify({
        code: 0,
        message: 'ok',
        data: { url: '/uploads/images/1.png' }
      }),
      _captured: options
    })
  });

  await merchant.getDishes();
  await merchant.getDishDetail(100);
  await merchant.createDish({ name: '番茄炒蛋' });
  await merchant.updateDish(100, { name: '番茄炒蛋' });
  await merchant.updateCookingSteps(100, []);
  await merchant.getDishCategories();
  await merchant.getDishReviews();
  await merchant.getDishReviewDetail(7);
  await merchant.withdrawDishReview(7);
  await merchant.createDishCategory({ name: '热菜' });
  await merchant.updateDishCategory(3, { name: '热菜' });
  await merchant.deleteDishCategory(3);
  await merchant.getOrders();
  await merchant.getOrderDetail(8);
  await merchant.confirmOrder(8);
  await merchant.rejectOrder(8);
  await merchant.cancelOrder(8, { reason: '停单' });
  await merchant.updateDeliveryFee(8, { deliveryFee: 8 });
  await merchant.advanceOrderStatus(8, { status: 'PREPARING' });
  await merchant.getFamilyMenu(2);
  await merchant.saveFamilyMenu(2, { items: [] });
  await merchant.copyFamilyMenu(2, { sourceFamilyId: 3 });
  await merchant.getFamilies();
  await merchant.getFamilyDetail(2);
  await merchant.updateFamilyProfile(2, { familyName: '陈家晚饭' });
  await merchant.updateFamilyDeliveryPolicy(2, { deliveryEnabled: true, deliveryFeeDefault: 6, deliveryFree: false });
  await merchant.getIngredients();
  await merchant.createIngredient({ name: '番茄', category: '蔬菜', unit: '个' });
  await merchant.updateIngredient(6, { name: '番茄', category: '蔬菜', unit: '个' });
  await merchant.deleteIngredient(6);
  await merchant.getMemberWalletLedgers(10);
  await merchant.adjustMemberBalance(10, { type: 'MANUAL_CREDIT', amount: 20, remark: '后台加款' });
  await purchase.getSummary({ date: '2026-07-02', includePending: true });
  await purchase.getTempItems({ date: '2026-07-02' });
  await purchase.getByFamily({ familyId: 2, date: '2026-07-02', includePending: true });
  await purchase.toggleChecked(6, { checked: true });
  await purchase.createTempItem({ ingredientName: '番茄酱' });
  await purchase.deleteTempItem(7);
  await purchase.getCopyText({ date: '2026-07-02', mealSlotId: 20 });
  const uploaded = await files.uploadImage('D:/fake/path.png');

  assert.equal(calls[0].pathname, '/api/merchant/dishes');
  assert.equal(calls[6].pathname, '/api/merchant/dish-reviews');
  assert.equal(calls[7].pathname, '/api/merchant/dish-reviews/7');
  assert.equal(calls[8].pathname, '/api/merchant/dish-reviews/7/withdraw');
  assert.equal(calls[19].pathname, '/api/merchant/families/2/menu');
  assert.equal(calls[22].pathname, '/api/merchant/families');
  assert.equal(calls[23].pathname, '/api/merchant/families/2');
  assert.equal(calls[24].pathname, '/api/merchant/families/2/profile');
  assert.equal(calls[25].pathname, '/api/merchant/families/2/delivery-policy');
  assert.equal(calls[26].pathname, '/api/merchant/ingredients');
  assert.equal(calls[27].pathname, '/api/merchant/ingredients');
  assert.equal(calls[28].pathname, '/api/merchant/ingredients/6');
  assert.equal(calls[29].pathname, '/api/merchant/ingredients/6');
  assert.equal(calls[30].pathname, '/api/merchant/members/10/wallet/ledgers');
  assert.equal(calls[31].pathname, '/api/merchant/members/10/wallet/adjust');
  assert.equal(calls[14].options.data, undefined);
  assert.match(calls[33].pathname, /^\/api\/merchant\/purchases\/temp-items\?date=2026-07-02$/);
  assert.match(calls[34].pathname, /^\/api\/merchant\/purchases\/by-family\?/);
  assert.match(calls[38].pathname, /^\/api\/merchant\/purchases\/copy-text\?/);
  assert.equal(uploaded.url, '/uploads/images/1.png');
  assert.equal(uploaded.imageUrl, 'http://127.0.0.1:8080/uploads/images/1.png');
});

test('merchant status update uses the dedicated endpoint and exact payload', async () => {
  const calls = [];
  const merchant = createMerchantService({ request: async (pathname, options) => { calls.push({ pathname, options }); return { code: 0, data: null }; } });
  await merchant.updateDishStatus(8, { status: 'INACTIVE' });
  assert.deepEqual(calls, [{ pathname: '/api/merchant/dishes/8/status', options: { method: 'PUT', data: { status: 'INACTIVE' } } }]);
});
