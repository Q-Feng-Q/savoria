const calendar = require('./notebook-calendar');
const MAX_SELECTED_EVENTS = 5;
const OUTLINE_HEARTS = [null,
  '/assets/ui/notebook-heart-outline-tone-1.png',
  '/assets/ui/notebook-heart-outline-tone-2.png',
  '/assets/ui/notebook-heart-outline-tone-3.png',
  '/assets/ui/notebook-heart-outline-tone-4.png',
  '/assets/ui/notebook-heart-outline-tone-5.png'];
const FILLED_HEARTS = [null,
  '/assets/ui/notebook-heart-filled-tone-1.png',
  '/assets/ui/notebook-heart-filled-tone-2.png',
  '/assets/ui/notebook-heart-filled-tone-3.png',
  '/assets/ui/notebook-heart-filled-tone-4.png',
  '/assets/ui/notebook-heart-filled-tone-5.png'];
const DEFAULT_OUTLINE_HEART = '/assets/ui/notebook-heart-outline.png';
const DEFAULT_FILLED_HEART = '/assets/ui/notebook-heart-filled.png';

function reconcileSelection(events, selectedIds, customized) {
  const available = (events || []).filter((event) => !event.archived);
  if (!customized) return available.filter((event) => event.starred)
    .slice(0, MAX_SELECTED_EVENTS).map((event) => event.id);
  const allowed = new Set(available.map((event) => event.id));
  return [...new Set(selectedIds || [])].filter((id) => allowed.has(id))
    .slice(0, MAX_SELECTED_EVENTS);
}

function assignSelectionTones(selectedIds, previous = {}) {
  const tones = {};
  const ids = [...new Set(selectedIds || [])].slice(0, MAX_SELECTED_EVENTS);
  if (!ids.length) return tones;
  tones[ids[0]] = 1;
  const used = new Set([1]);
  ids.slice(1).forEach((id) => {
    const tone = Number(previous[id]);
    if (tone >= 2 && tone <= MAX_SELECTED_EVENTS && !used.has(tone)) {
      tones[id] = tone;
      used.add(tone);
    }
  });
  ids.slice(1).forEach((id) => {
    if (tones[id]) return;
    const tone = Array.from({ length: MAX_SELECTED_EVENTS }, (_, index) => index + 1)
      .find((value) => !used.has(value));
    tones[id] = tone;
    used.add(tone);
  });
  return tones;
}

function visibleSelectedEvents(displayEvents, selectedIds) {
  const selected = new Set(selectedIds || []);
  return (displayEvents || []).filter((event) => selected.has(event.id));
}

function mergeEventMonths(results, selectedEventToneMap = {}) {
  const summary = {};
  const dayToneSets = {};
  const records = [];
  (results || []).forEach(({ event, summary: days, records: items }) => {
    (days || []).forEach((item) => {
      summary[item.date] = (summary[item.date] || 0) + (Number(item.recordCount) || 0);
      const tone = Number(selectedEventToneMap[event.id]);
      if (Number(item.recordCount) > 0 && tone >= 1 && tone <= MAX_SELECTED_EVENTS) {
        if (!dayToneSets[item.date]) dayToneSets[item.date] = new Set();
        dayToneSets[item.date].add(tone);
      }
    });
    (items || []).forEach((record) => records.push({ ...record,
      eventId: event.id, eventName: event.name }));
  });
  records.sort((left, right) => {
    const leftTime = Date.parse(left.occurredFrom) || 0;
    const rightTime = Date.parse(right.occurredFrom) || 0;
    return leftTime - rightTime || Number(left.id) - Number(right.id);
  });
  const dayTones = Object.fromEntries(Object.entries(dayToneSets)
    .map(([date, tones]) => [date, [...tones].sort((left, right) => left - right)]));
  return { summary, dayTones, records };
}

function calendarHeartSources(summary, dayTones, selectedDate) {
  const sources = {};
  Object.entries(summary || {}).forEach(([date, count]) => {
    if (!count) return;
    const tone = ((dayTones || {})[date] || [])[0];
    sources[date] = OUTLINE_HEARTS[tone] || DEFAULT_OUTLINE_HEART;
  });
  if (selectedDate) {
    const tone = ((dayTones || {})[selectedDate] || [])[0];
    sources[selectedDate] = summary && summary[selectedDate]
      ? (FILLED_HEARTS[tone] || DEFAULT_FILLED_HEART) : DEFAULT_FILLED_HEART;
  }
  return sources;
}

function groupTimelineRecords(records) {
  const ordered = [...(records || [])].sort((left, right) => {
    const leftTime = Date.parse(left.occurredFrom) || 0;
    const rightTime = Date.parse(right.occurredFrom) || 0;
    return rightTime - leftTime || Number(right.id) - Number(left.id);
  });
  const groups = [];
  ordered.forEach((record) => {
    const instant = Date.parse(record.occurredFrom);
    const date = Number.isFinite(instant) ? calendar.dateKey(new Date(instant)) : '日期待确认';
    let group = groups[groups.length - 1];
    if (!group || group.date !== date) {
      group = { date, records: [] };
      groups.push(group);
    }
    group.records.push(record);
  });
  return groups;
}

module.exports = { MAX_SELECTED_EVENTS, reconcileSelection, assignSelectionTones,
  visibleSelectedEvents, mergeEventMonths, calendarHeartSources, groupTimelineRecords };
