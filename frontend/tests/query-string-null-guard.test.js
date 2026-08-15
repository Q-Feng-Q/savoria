const test = require('node:test')
const assert = require('node:assert/strict')
const { createQueryString } = require('../services/_shared')

test('query builder never serializes null-like values for numeric API parameters', () => {
  assert.equal(createQueryString({ mealSlotId: null, categoryId: 'null', familyId: 'undefined' }), '')
  assert.equal(createQueryString({ mealSlotId: Number.NaN, date: '2026-07-22' }), '?date=2026-07-22')
})
