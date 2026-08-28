const test = require('node:test');
const assert = require('node:assert/strict');
const { loadFamilyBundle } = require('../utils/family-api');
const { buildExpectedMealTimeOptions, buildApiCartScene } = require('../utils/api-scenes');

test('family bundle no longer fetches or requires meal slots', async () => {
  let legacyCalls = 0;
  const homeData = { serviceDate: '2026-08-28', member: { memberId: 2, name: '小林' }, family: { familyId: 3, familyName: '林家' } };
  const bundle = await loadFamilyBundle({ family: { getHome: async () => homeData, getMealSlots: async () => { legacyCalls += 1; } } });
  assert.equal(legacyCalls, 0);
  assert.equal(bundle.homeData, homeData);
  assert.equal('mealSlots' in bundle, false);
});

test('time choices come only from server minimum and remain on server date', () => {
  const cart = { serverDate: '2026-08-28', serverNow: '2026-08-28T17:02:00', minimumExpectedMealTime: '2026-08-28T17:15:00', timeStepMinutes: 15, bookingEnded: false };
  const options = buildExpectedMealTimeOptions(cart);
  assert.equal(options[0].value, '2026-08-28T17:15:00');
  assert.equal(options.at(-1).value, '2026-08-28T23:45:00');
  assert.equal(options.every((option) => option.value.startsWith('2026-08-28T')), true);
  assert.deepEqual(buildExpectedMealTimeOptions({ ...cart, bookingEnded: true }), []);
});

test('cart scene exposes aggregate quantity and collapsed member selections', () => {
  const homeData = { serviceDate: '2026-08-28', member: { memberId: 2, name: '小林' }, family: { familyId: 3, familyName: '林家' } };
  const cart = { serverDate: '2026-08-28', minimumExpectedMealTime: '2026-08-28T18:00:00', timeStepMinutes: 15,
    bookingEnded: false, expectedMealTime: '2026-08-28T18:30:00', totalAmount: 114,
    items: [{ itemId: 1, dishId: 9, dishName: '番茄牛腩', price: 38, quantity: 3, currentMemberQuantity: 1,
      currentMemberRemark: '少盐', selections: [{ memberId: 2, memberName: '小林', quantity: 1 }, { memberId: 4, memberName: '阿禾', quantity: 2 }] }] };
  const scene = buildApiCartScene({ homeData, cart });
  const row = scene.groupedItems[0].rows[0];
  assert.equal(row.totalQuantity, 3);
  assert.equal(row.myQuantity, 1);
  assert.equal(row.hasSelectionDetails, true);
  assert.equal(scene.expectedMealTimeText, '2026-08-28 18:30');
});
