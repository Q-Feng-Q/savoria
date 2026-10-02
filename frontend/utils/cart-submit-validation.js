const TARGETS = Object.freeze({
  time: '#cart-time-section',
  items: '#cart-items-section',
  fulfillment: '#cart-fulfillment-section'
});

function validateCartSubmit(state = {}) {
  const groups = Array.isArray(state.groupedItems) ? state.groupedItems : [];
  const rows = groups.flatMap((group) => (Array.isArray(group.rows) ? group.rows : []));
  const issues = [];
  const add = (message, target) => issues.push({ message, target });

  if (state.bookingEnded) add('今天已停止预约，请明天再来', TARGETS.time);
  if (!rows.length) add('请先选择至少一道菜品', TARGETS.items);
  if (typeof state.expectedMealTime !== 'string' || !state.expectedMealTime.trim()) {
    add('请选择今天的预计用餐时间', TARGETS.time);
  }
  if (rows.some((row) => row && row.available === false)) {
    add('餐篮中有不可用菜品，请减少到 0 后再提交', TARGETS.items);
  }
  if (state.deliveryMode === 'DELIVERY' && !state.currentAddress) {
    add('请选择配送地址', TARGETS.fulfillment);
  }
  if (!issues.length && state.canSubmit === false) add('请先完善下单信息', TARGETS.time);

  return { issues, target: issues.length ? issues[0].target : '' };
}

function formatCartSubmitIssues(issues = []) {
  return `请先完善以下信息：\n${issues
    .map((item, index) => `${index + 1}. ${item.message}`)
    .join('\n')}`;
}

module.exports = { TARGETS, validateCartSubmit, formatCartSubmitIssues };
