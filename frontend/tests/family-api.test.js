const test = require('node:test');
const assert = require('node:assert/strict');
const { loadFamilyBundle } = require('../utils/family-api');

test('loadFamilyBundle uses home directly without legacy meal slot reads', async () => {
  let getMealSlotsCalled = false;
  const homeData = {
    family: { familyId: 2, familyName: '陈家晚饭', merchantId: 1, merchantName: '食光知味' },
    member: { memberId: 10, name: '陈梅', roleTemplate: 'member' },
    serviceDate: '2026-07-02'
  };
  const bundle = await loadFamilyBundle({ family: {
    getHome: async () => homeData,
    getMealSlots: async () => { getMealSlotsCalled = true; return []; }
  } });
  assert.equal(getMealSlotsCalled, false);
  assert.equal(bundle.homeData, homeData);
  assert.equal(bundle.serviceDate, '2026-07-02');
  assert.equal('mealSlots' in bundle, false);
});

test('loadFamilyBundle accepts a home response without meal slots', async () => {
  const bundle = await loadFamilyBundle({ family: { getHome: async () => ({ serviceDate: '2026-07-22' }) } });
  assert.equal(bundle.serviceDate, '2026-07-22');
});
