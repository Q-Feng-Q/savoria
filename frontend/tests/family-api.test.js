const test = require('node:test');
const assert = require('node:assert/strict');

const {
  resolveActiveMealSlotId,
  withSelectedMealSlots,
  loadFamilyBundle
} = require('../utils/family-api');

test('resolveActiveMealSlotId prefers explicit slot then selected slot', () => {
  const mealSlots = [
    { mealSlotId: 10, selected: false },
    { mealSlotId: 20, selected: true }
  ];

  assert.equal(resolveActiveMealSlotId(mealSlots, 10), 10);
  assert.equal(resolveActiveMealSlotId(mealSlots), 20);
});

test('resolveActiveMealSlotId ignores null-like and invalid meal slot ids', () => {
  const mealSlots = [
    { mealSlotId: 'null', selected: true },
    { mealSlotId: null, selected: false },
    { mealSlotId: 20, selected: false }
  ];

  assert.equal(resolveActiveMealSlotId(mealSlots, 'null'), 20);
});

test('loadFamilyBundle stops before API calls when no valid meal slot exists', async () => {
  await assert.rejects(() => loadFamilyBundle({
    family: {
      getHome: async () => ({ serviceDate: '2026-07-22', mealSlots: [{ mealSlotId: 'null', selected: true }] }),
      getMealSlots: async () => []
    }
  }), /未配置可用餐次/);
});

test('withSelectedMealSlots marks only the active slot as selected', () => {
  const result = withSelectedMealSlots([
    { mealSlotId: 10, selected: false },
    { mealSlotId: 20, selected: true }
  ], 10);

  assert.equal(result[0].selected, true);
  assert.equal(result[1].selected, false);
});

test('loadFamilyBundle reuses home meal slots and applies active selection', async () => {
  let getMealSlotsCalled = false;
  const family = {
    getHome: async () => ({
      family: { familyId: 2, familyName: '陈家晚饭', merchantId: 1, merchantName: '食光知味' },
      member: { memberId: 10, name: '陈梅', roleTemplate: 'member' },
      serviceDate: '2026-07-02',
      mealSlots: [
        { mealSlotId: 20, name: '午餐', displayTime: '12:00', selected: true },
        { mealSlotId: 30, name: '晚餐', displayTime: '18:30', selected: false }
      ]
    }),
    getMealSlots: async () => {
      getMealSlotsCalled = true;
      return [];
    }
  };

  const bundle = await loadFamilyBundle({ family }, { mealSlotId: 30 });

  assert.equal(getMealSlotsCalled, false);
  assert.equal(bundle.activeMealSlotId, 30);
  assert.equal(bundle.mealSlots[1].selected, true);
  assert.equal(bundle.homeData.mealSlots[1].selected, true);
});

test('loadFamilyBundle falls back to meal slots endpoint when home response omits them', async () => {
  const bundle = await loadFamilyBundle({
    family: {
      getHome: async () => ({
        family: { familyId: 2, familyName: '陈家晚饭', merchantId: 1, merchantName: '食光知味' },
        member: { memberId: 10, name: '陈梅', roleTemplate: 'member' },
        serviceDate: '2026-07-02'
      }),
      getMealSlots: async () => ([
        { mealSlotId: 20, name: '午餐', displayTime: '12:00' }
      ])
    }
  });

  assert.equal(bundle.activeMealSlotId, 20);
  assert.equal(bundle.mealSlots.length, 1);
  assert.equal(bundle.homeData.mealSlots[0].selected, true);
});
