const calendar = require('./notebook-calendar');

function validDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '');
  if (!match) return false;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (year < 1000 || month < 1 || month > 12 || day < 1 || day > 31) return false;
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year && date.getUTCMonth() + 1 === month
    && date.getUTCDate() === day;
}

function validateRange(from, to, maxQueryMonths) {
  if (!validDate(from) || !validDate(to) || from > to) {
    return { ok: false, touchedMonths: 0, message: '请选择有效的起止日期' };
  }
  const touchedMonths = (Number(to.slice(0, 4)) - Number(from.slice(0, 4))) * 12
    + Number(to.slice(5, 7)) - Number(from.slice(5, 7)) + 1;
  const limit = Number.isInteger(maxQueryMonths) && maxQueryMonths >= 1
    && maxQueryMonths <= 36 ? maxQueryMonths : 36;
  if (touchedMonths > limit) {
    return { ok: false, touchedMonths, message: `单次最多查看 ${limit} 个日历月` };
  }
  return { ok: true, touchedMonths, message: '' };
}

function monthKeys(from, to) {
  const range = validateRange(from, to, 36);
  if (!range.ok) return [];
  const first = from.slice(0, 7);
  return Array.from({ length: range.touchedMonths }, (_, index) =>
    calendar.shiftMonth(first, index));
}

function projectMonths(from, to, summary) {
  const counts = {};
  (summary || []).forEach((item) => {
    if (item && item.date >= from && item.date <= to) {
      counts[item.date] = Number(item.recordCount) || 0;
    }
  });
  return monthKeys(from, to).map((key) => ({
    key,
    days: calendar.monthDays(key).map((day) => ({
      ...day,
      outsideRange: day.blank || day.key < from || day.key > to,
      recordCount: day.blank ? 0 : counts[day.key] || 0,
      hasRecord: !day.blank && Boolean(counts[day.key])
    }))
  }));
}

module.exports = { validateRange, monthKeys, projectMonths };
