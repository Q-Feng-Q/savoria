const test = require('node:test');
const assert = require('node:assert/strict');
const { validateRange, monthKeys, projectMonths } = require('../utils/notebook-history');

test('history range counts every touched calendar month and accepts exact days', () => {
  assert.equal(validateRange('2026-10-31', '2026-11-01', 1).ok, false);
  assert.deepEqual(validateRange('2026-10-31', '2026-11-01', 2),
    { ok: true, touchedMonths: 2, message: '' });
  assert.equal(validateRange('2028-02-29', '2028-02-29', 1).ok, true);
  assert.equal(validateRange('2026-02-29', '2026-03-01', 2).ok, false);
  assert.equal(validateRange('2026-11-02', '2026-11-01', 2).ok, false);
  assert.equal(validateRange('', '2026-11-01', 2).ok, false);
});

test('history month projection retains two-column order and clips partial months', () => {
  assert.deepEqual(monthKeys('2026-08-15', '2026-11-03'),
    ['2026-08', '2026-09', '2026-10', '2026-11']);
  const months = projectMonths('2026-10-03', '2026-11-02', [
    { date: '2026-10-03', recordCount: 2 },
    { date: '2026-11-02', recordCount: 1 }
  ]);
  assert.deepEqual(months.map((month) => month.key), ['2026-10', '2026-11']);
  const october = months[0].days.filter((day) => !day.blank);
  const november = months[1].days.filter((day) => !day.blank);
  assert.equal(october[0].outsideRange, true);
  assert.equal(october[2].hasRecord, true);
  assert.equal(october[2].recordCount, 2);
  assert.equal(november[0].outsideRange, false);
  assert.equal(november[2].outsideRange, true);
});
