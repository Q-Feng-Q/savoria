const none = () => ({ read: false, create: false, edit: false, export: false });
const { shiftMonth } = require('./notebook-calendar');
const pad = (value) => String(value).padStart(2, '0');

function addDays(date, days) {
  const [year, month, day] = String(date).split('-').map(Number);
  const result = new Date(year, month - 1, day + days);
  return `${result.getFullYear()}-${pad(result.getMonth() + 1)}-${pad(result.getDate())}`;
}

function emptyGrant(today, timeZone) {
  return { granteeUserId: null, dataFrom: today, dataTo: today,
    dataTimeZone: timeZone || 'Asia/Shanghai', validFromDate: today,
    validToDate: addDays(today, 1), canCreate: false, canEdit: false, canExport: false };
}

function grantCapabilities(grant, now = new Date().toISOString()) {
  if (!grant || grant.status !== 'ACTIVE' || !grant.validFrom || !grant.validTo
      || Date.parse(grant.validFrom) > Date.parse(now)
      || Date.parse(grant.validTo) <= Date.parse(now)) return none();
  return { read: true, create: Boolean(grant.canCreate), edit: Boolean(grant.canEdit),
    export: Boolean(grant.canExport) };
}

function localDayInZone(instant, zone) {
    const formatter = new Intl.DateTimeFormat('en-US', { timeZone: zone,
      year: 'numeric', month: '2-digit', day: '2-digit' });
    const parts = Object.fromEntries(formatter.formatToParts(new Date(instant))
        .filter((part) => ['year', 'month', 'day'].includes(part.type))
        .map((part) => [part.type, part.value]));
    return `${parts.year}-${parts.month}-${parts.day}`;
}

function recordWithinGrant(grant, record, now = new Date().toISOString()) {
  if (!grantCapabilities(grant, now).edit || !record || !grant.dataTimeZone) return false;
  try {
    const from = localDayInZone(record.occurredFrom, grant.dataTimeZone);
    const to = localDayInZone(record.occurredTo, grant.dataTimeZone);
    return from >= grant.dataFrom && to <= grant.dataTo;
  } catch (_) { return false; }
}

function draftFromGrant(grant, validityTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone) {
  return { granteeUserId: grant.granteeUserId, dataFrom: grant.dataFrom,
    dataTo: grant.dataTo, dataTimeZone: grant.dataTimeZone,
    validFromDate: localDayInZone(grant.validFrom, validityTimeZone),
    validToDate: localDayInZone(new Date(Date.parse(grant.validTo) - 1), validityTimeZone),
    canCreate: Boolean(grant.canCreate), canEdit: Boolean(grant.canEdit),
    canExport: Boolean(grant.canExport) };
}

function validSharedRange(grant, from, to, maxMonths) {
  const touched = (Number(to.slice(0, 4)) - Number(from.slice(0, 4))) * 12
    + Number(to.slice(5, 7)) - Number(from.slice(5, 7)) + 1;
  return Boolean(grant && from >= grant.dataFrom && to <= grant.dataTo && from <= to
    && touched >= 1 && touched <= maxMonths);
}

function boundedGrantRange(grant, maxMonths) {
  const from = `${shiftMonth(grant.dataTo.slice(0, 7), 1 - maxMonths)}-01`;
  return { from: from > grant.dataFrom ? from : grant.dataFrom, to: grant.dataTo };
}

function localStart(date) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error('请选择有效日期');
  const [year, month, day] = date.split('-').map(Number);
  const value = new Date(year, month - 1, day);
  if (value.getFullYear() !== year || value.getMonth() !== month - 1 || value.getDate() !== day) {
    throw new Error('请选择有效日期');
  }
  const offsetMinutes = -value.getTimezoneOffset();
  const sign = offsetMinutes < 0 ? '-' : '+';
  const absolute = Math.abs(offsetMinutes);
  return `${date}T00:00:00${sign}${pad(Math.floor(absolute / 60))}:${pad(absolute % 60)}`;
}

function grantPayload(draft, maxMonths) {
  const from = String(draft.dataFrom || '');
  const to = String(draft.dataTo || '');
  localStart(from); localStart(to);
  const touched = (Number(to.slice(0, 4)) - Number(from.slice(0, 4))) * 12
    + Number(to.slice(5, 7)) - Number(from.slice(5, 7)) + 1;
  if (from > to || touched > maxMonths || touched < 1) throw new Error('记录范围超出允许月份');
  if (!draft.granteeUserId || !draft.dataTimeZone || !draft.validFromDate || !draft.validToDate) {
    throw new Error('请完整选择联系人和授权日期');
  }
  const validFrom = localStart(draft.validFromDate);
  const validTo = localStart(addDays(draft.validToDate, 1));
  if (Date.parse(validFrom) >= Date.parse(validTo)) throw new Error('授权结束日期须晚于开始日期');
  return { granteeUserId: Number(draft.granteeUserId), dataFrom: from, dataTo: to,
    dataTimeZone: draft.dataTimeZone, validFrom, validTo,
    canCreate: Boolean(draft.canCreate), canEdit: Boolean(draft.canEdit),
    canExport: Boolean(draft.canExport) };
}

module.exports = { emptyGrant, grantCapabilities, grantPayload, recordWithinGrant,
  boundedGrantRange, validSharedRange, draftFromGrant };
