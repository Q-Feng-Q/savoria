const test = require('node:test');
const assert = require('node:assert/strict');
const { buildHomeSummary, buildFeaturedDishes } = require('../utils/home-highlights');

const dishes = [
  { id: 'dish-a', name: 'Tomato Egg', category: 'Home', status: 'on', tasteTags: ['Light'] },
  { id: 'dish-b', name: 'Rib Rice', category: 'Staple', status: 'on', tasteTags: ['Classic'] },
  { id: 'dish-c', name: 'Tofu Soup', category: 'Soup', status: 'on', tasteTags: ['Fresh'] },
  { id: 'dish-hidden', name: 'Hidden Dish', category: 'Soup', status: 'off', tasteTags: ['Hidden'] }
];

test('buildHomeSummary counts available dishes, today orders, and current meal state', () => {
  const summary = buildHomeSummary({
    dishes,
    orders: [
      {
        id: 'o1',
        mealDate: '2026-06-16',
        mealType: 'lunch',
        status: 'pending',
        dishes: [{ dishId: 'dish-a', count: 2 }, { dishId: 'dish-b', count: 1 }]
      },
      {
        id: 'o2',
        mealDate: '2026-06-16',
        mealType: 'dinner',
        status: 'cancelled',
        dishes: [{ dishId: 'dish-c', count: 4 }]
      },
      {
        id: 'o3',
        mealDate: '2026-06-15',
        mealType: 'lunch',
        status: 'pending',
        dishes: [{ dishId: 'dish-b', count: 1 }]
      }
    ],
    currentDate: '2026-06-16',
    currentMealType: 'lunch',
    mealOverview: {
      lunch: { cartCount: 2, orderCount: 3, peopleCount: 4 },
      dinner: { cartCount: 1, orderCount: 0, peopleCount: 2 }
    }
  });

  assert.equal(summary.availableDishCount, 3);
  assert.equal(summary.todayOrderCount, 1);
  assert.equal(summary.todayOrderDishCount, 3);
  assert.equal(summary.currentMealCartCount, 2);
  assert.equal(summary.currentMealOrderCount, 3);
  assert.equal(summary.currentMealPeopleCount, 4);
});

test('buildFeaturedDishes ranks on-sale dishes by sold count then selected count', () => {
  const featured = buildFeaturedDishes({
    dishes,
    currentMealType: 'lunch',
    limit: 3,
    getSoldCount: (dishId) => {
      if (dishId === 'dish-b') return 5;
      if (dishId === 'dish-a') return 2;
      return 0;
    },
    getSelectedCount: (dishId, mealType) => {
      if (dishId === 'dish-c' && mealType === 'lunch') return 2;
      return 0;
    }
  });

  assert.deepEqual(featured.map((dish) => dish.id), ['dish-b', 'dish-a', 'dish-c']);
  assert.equal(featured[0].featuredReasonKey, 'sold');
  assert.equal(featured[1].soldCount, 2);
  assert.equal(featured[2].selectedCount, 2);
  assert.equal(featured[2].featuredReasonKey, 'selected');
});
