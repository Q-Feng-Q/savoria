function isValidMealSlotId(value) {
  if (value === null || value === undefined || value === '') return false;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) && numericValue > 0;
}

function resolveActiveMealSlotId(mealSlots = [], preferredMealSlotId = null) {
  const validMealSlots = mealSlots.filter((item) => isValidMealSlotId(item && item.mealSlotId));

  if (isValidMealSlotId(preferredMealSlotId)) {
    const matched = validMealSlots.find((item) => Number(item.mealSlotId) === Number(preferredMealSlotId));
    if (matched) {
      return matched.mealSlotId;
    }
  }

  const selected = validMealSlots.find((item) => item.selected);
  return selected ? selected.mealSlotId : ((validMealSlots[0] && validMealSlots[0].mealSlotId) || null);
}

function withSelectedMealSlots(mealSlots = [], activeMealSlotId = null) {
  const resolvedActiveMealSlotId = resolveActiveMealSlotId(mealSlots, activeMealSlotId);

  return mealSlots.map((item) => ({
    ...item,
    selected: Number(item.mealSlotId) === Number(resolvedActiveMealSlotId)
  }));
}

async function loadFamilyBundle(runtime, options = {}) {
  const homeData = await runtime.family.getHome();
  const rawMealSlots = Array.isArray(homeData.mealSlots) && homeData.mealSlots.length
    ? homeData.mealSlots
    : await runtime.family.getMealSlots();
  const sourceMealSlots = rawMealSlots.filter((item) => isValidMealSlotId(item && item.mealSlotId));
  if (!sourceMealSlots.length) {
    throw new Error('当前家庭未配置可用餐次，请联系商户配置早餐、午餐或晚餐');
  }
  const activeMealSlotId = resolveActiveMealSlotId(sourceMealSlots, options.mealSlotId);
  const mealSlots = withSelectedMealSlots(sourceMealSlots, activeMealSlotId);

  return {
    homeData: {
      ...homeData,
      mealSlots
    },
    mealSlots,
    activeMealSlotId,
    serviceDate: options.date || homeData.serviceDate
  };
}

module.exports = {
  resolveActiveMealSlotId,
  withSelectedMealSlots,
  loadFamilyBundle
};
