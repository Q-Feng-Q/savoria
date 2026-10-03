function pad(value) { return String(value).padStart(2, '0'); }
function dateKey(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
function monthKey(date) { return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`; }
function monthBounds(month) {
  const [year, number] = String(month).split('-').map(Number);
  if (!Number.isInteger(year) || !Number.isInteger(number) || number < 1 || number > 12) {
    throw new Error('无效月份');
  }
  return { from: `${year}-${pad(number)}-01`, to: dateKey(new Date(year, number, 0)) };
}
function shiftMonth(month, offset) {
  const [year, number] = String(month).split('-').map(Number);
  return monthKey(new Date(year, number - 1 + offset, 1));
}
function monthDays(month) {
  const { from, to } = monthBounds(month);
  const [year, number] = from.split('-').map(Number);
  const firstDay = new Date(year, number - 1, 1).getDay();
  const mondayOffset = (firstDay + 6) % 7;
  const count = Number(to.slice(-2));
  return Array.from({ length: mondayOffset + count }, (_, index) => {
    const day = index - mondayOffset + 1;
    return day < 1 ? { key: `blank-${index}`, blank: true } :
      { key: `${month}-${pad(day)}`, day, blank: false };
  });
}
function timeZone() {
  try { return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'; }
  catch (_) { return 'Asia/Shanghai'; }
}
module.exports = { dateKey, monthKey, monthBounds, shiftMonth, monthDays, timeZone };
