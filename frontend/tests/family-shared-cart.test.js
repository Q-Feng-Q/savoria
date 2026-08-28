const test = require('node:test');
const assert = require('node:assert/strict');
const { createCartService } = require('../services/cart');
const { createOrdersService } = require('../services/orders');

test('shared cart uses one parameterless cart and versioned absolute mutations', async () => {
  const calls = [];
  const request = async (pathname, options) => {
    calls.push({ pathname, options });
    return { code: 0, data: { cartId: 7, version: calls.length } };
  };
  const cart = createCartService({ request });
  await cart.getCart({ date: '2026-08-28', mealSlotId: 20 });
  await cart.setItemQuantity({ cartId: 7, cartVersion: 1, requestId: 'set-1', dishId: 9, quantity: 3 });
  await cart.updateExpectedMealTime({ cartId: 7, cartVersion: 2, requestId: 'time-1', expectedMealTime: '2026-08-28T18:30:00' });
  await cart.updateRemark({ cartId: 7, cartVersion: 3, requestId: 'remark-1', remark: '少盐' });

  assert.deepEqual(calls, [
    { pathname: '/api/family/cart', options: { method: 'GET' } },
    { pathname: '/api/family/cart/items', options: { method: 'PUT', data: { cartId: 7, cartVersion: 1, requestId: 'set-1', dishId: 9, quantity: 3 } } },
    { pathname: '/api/family/cart/expected-meal-time', options: { method: 'PUT', data: { cartId: 7, cartVersion: 2, requestId: 'time-1', expectedMealTime: '2026-08-28T18:30:00' } } },
    { pathname: '/api/family/cart/remark', options: { method: 'PUT', data: { cartId: 7, cartVersion: 3, requestId: 'remark-1', remark: '少盐' } } }
  ]);
});

test('order submission sends only shared-cart checkout fields', async () => {
  const calls = [];
  const orders = createOrdersService({ request: async (pathname, options) => {
    calls.push({ pathname, options }); return { code: 0, data: { orderId: 88 } };
  } });
  const payload = { cartId: 7, cartVersion: 4, requestId: 'submit-1', deliveryMode: 'PICKUP', addressId: null, remark: '' };
  await orders.submitOrder(payload);
  assert.deepEqual(calls[0], { pathname: '/api/family/orders', options: { method: 'POST', data: payload } });
  assert.equal('mealSlotId' in calls[0].options.data, false);
  assert.equal('payerMemberId' in calls[0].options.data, false);
});
