const test = require('node:test');
const assert = require('node:assert/strict');
const { buildMealStatusList } = require('../utils/meal-status');

const mealTypes = [
  { value: 'breakfast', label: '早餐', time: '7:30' },
  { value: 'lunch', label: '午餐', time: '12:00' },
  { value: 'dinner', label: '晚餐', time: '18:30' }
];

test('buildMealStatusList adds active class, status text and people text', () => {
  const result = buildMealStatusList({
    mealTypes,
    activeMealType: 'lunch',
    overview: {
      breakfast: { statusText: '未点', peopleCount: 2 },
      lunch: { statusText: '已选 3', peopleCount: 4 },
      dinner: { statusText: '已下单 2', peopleCount: 2 }
    }
  });

  assert.equal(result[0].activeClass, '');
  assert.equal(result[1].activeClass, 'active');
  assert.equal(result[1].statusText, '已选 3');
  assert.equal(result[1].peopleText, '4人');
});
