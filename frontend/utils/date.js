function pad(value) {
  return String(value).padStart(2, '0');
}

function todayText(date = new Date()) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

module.exports = {
  todayText
};
