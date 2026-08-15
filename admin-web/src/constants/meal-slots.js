export const MEAL_SLOT_OPTIONS = [
  { value: 10, label: '早餐' },
  { value: 20, label: '午餐' },
  { value: 30, label: '晚餐' }
];

export const MEAL_SLOT_FILTER_OPTIONS = [
  { value: '', label: '全天' },
  ...MEAL_SLOT_OPTIONS
];

export function getMealSlotLabel(mealSlotId) {
  const current = MEAL_SLOT_OPTIONS.find((item) => Number(item.value) === Number(mealSlotId));
  return current ? current.label : `餐次 ${mealSlotId}`;
}
