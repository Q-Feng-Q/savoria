const test = require('node:test');
const assert = require('node:assert/strict');
const { MAX_SELECTED_EVENTS, reconcileSelection, assignSelectionTones,
  visibleSelectedEvents, mergeEventMonths, calendarHeartSources, groupTimelineRecords } =
  require('../utils/notebook-overview');

const events = [
  { id: 1, name: '生理期', category: '身体', starred: true },
  { id: 2, name: '体重变化', category: '身体', starred: false },
  { id: 3, name: '纪念日', category: '生活', starred: true }
];

test('unmodified selection follows current starred events, including none', () => {
  assert.deepEqual(reconcileSelection(events, [], false), [1, 3]);
  assert.deepEqual(reconcileSelection(events.map((event) => ({ ...event, starred: false })),
    [1, 3], false), []);
  assert.deepEqual(reconcileSelection([{ ...events[0], starred: false }, events[1]], [], false), []);
});

test('manual selection keeps explicit empty and prunes removed events', () => {
  assert.deepEqual(reconcileSelection(events, [], true), []);
  assert.deepEqual(reconcileSelection(events, [2, 3, 99], true), [2, 3]);
  assert.deepEqual(visibleSelectedEvents(events.filter((event) => event.category === '身体'),
    [1, 3]).map((event) => event.id), [1]);
});

test('selection is capped at five and keeps distinct tones stable while toggling', () => {
  const many = Array.from({ length: 7 }, (_, index) =>
    ({ id: index + 1, name: `事件${index + 1}`, starred: true }));
  assert.equal(MAX_SELECTED_EVENTS, 5);
  assert.deepEqual(reconcileSelection(many, [], false), [1, 2, 3, 4, 5]);
  assert.deepEqual(reconcileSelection(many, [2, 3, 4, 5, 6, 7], true), [2, 3, 4, 5, 6]);
  const initial = assignSelectionTones([1, 2, 3, 4, 5]);
  assert.deepEqual(Object.values(initial), [1, 2, 3, 4, 5]);
  assert.deepEqual(assignSelectionTones([1, 3, 4, 5, 6], initial),
    { 1: 1, 3: 3, 4: 4, 5: 5, 6: 2 });
  const withoutFirst = assignSelectionTones([3, 4, 5], initial);
  assert.equal(withoutFirst[3], 1, 'the first visible selection always uses red');
  assert.deepEqual(new Set(Object.values(withoutFirst)).size, 3);
});

test('monthly aggregation sums days and labels records by source event', () => {
  const combined = mergeEventMonths([
    { event: events[0], summary: [{ date: '2026-10-03', recordCount: 2 }],
      records: [{ id: 9, occurredFrom: '2026-10-03T09:00:00Z' }] },
    { event: events[2], summary: [{ date: '2026-10-03', recordCount: 1 }],
      records: [{ id: 8, occurredFrom: '2026-10-03T08:00:00Z' }] }
  ], { 1: 1, 3: 2 });
  assert.equal(combined.summary['2026-10-03'], 3);
  assert.deepEqual(combined.dayTones['2026-10-03'], [1, 2]);
  assert.deepEqual(combined.records.map((record) => [record.id, record.eventName]),
    [[8, '纪念日'], [9, '生理期']]);
});

test('calendar hearts use event tones and retain orange for an empty selected day', () => {
  const counts = { '2026-10-03': 1, '2026-10-04': 1 };
  const dayTones = { '2026-10-03': [1], '2026-10-04': [2] };
  const hearts = calendarHeartSources(counts, dayTones, '2026-10-06');
  assert.match(hearts['2026-10-03'], /outline-tone-1\.png$/);
  assert.match(hearts['2026-10-04'], /outline-tone-2\.png$/);
  assert.match(hearts['2026-10-06'], /notebook-heart-filled\.png$/);
  assert.match(calendarHeartSources(counts, dayTones, '2026-10-04')['2026-10-04'],
    /filled-tone-2\.png$/);
});

test('time flow groups records from the same day in newest-first order', () => {
  const groups = groupTimelineRecords([
    { id: 1, occurredFrom: '2026-10-03T09:00:00' },
    { id: 2, occurredFrom: '2026-10-04T08:00:00' },
    { id: 3, occurredFrom: '2026-10-04T10:00:00' }
  ]);
  assert.deepEqual(groups.map((group) => [group.date, group.records.map((record) => record.id)]),
    [['2026-10-04', [3, 2]], ['2026-10-03', [1]]]);
});
