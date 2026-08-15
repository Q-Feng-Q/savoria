const test = require('node:test');
const assert = require('node:assert/strict');
const { buildPurchaseList } = require('../utils/purchase');

const dishes = [
  {
    id: 'tomato-egg',
    name: '番茄炒蛋',
    baseServings: 2,
    ingredients: [
      { id: 'egg', name: '鸡蛋', quantity: 3, unit: '个', category: '蛋奶', calculationType: 'per_person' },
      { id: 'tomato', name: '番茄', quantity: 2, unit: '个', category: '蔬菜', calculationType: 'per_person' },
      { id: 'salt', name: '盐', quantity: 3, unit: '克', category: '调料', calculationType: 'no_purchase' }
    ]
  },
  {
    id: 'egg-pancake',
    name: '鸡蛋饼',
    baseServings: 1,
    ingredients: [
      { id: 'egg', name: '鸡蛋', quantity: 1, unit: '个', category: '蛋奶', calculationType: 'per_person' },
      { id: 'flour', name: '面粉', quantity: 80, unit: '克', category: '主食', calculationType: 'per_person' },
      { id: 'oil', name: '食用油', quantity: 10, unit: '毫升', category: '调料', calculationType: 'fixed' }
    ]
  }
];

test('builds a merged purchase list with meal breakdowns and excludes no-purchase items', () => {
  const orders = [
    {
      id: 'o1',
      mealDate: '2026-06-06',
      mealType: 'breakfast',
      peopleCount: 2,
      dishes: [{ dishId: 'egg-pancake', count: 1 }]
    },
    {
      id: 'o2',
      mealDate: '2026-06-06',
      mealType: 'lunch',
      peopleCount: 4,
      dishes: [{ dishId: 'tomato-egg', count: 1 }]
    }
  ];

  const list = buildPurchaseList({ orders, dishes, date: '2026-06-06' });

  assert.deepEqual(
    list.items.map((item) => ({
      id: item.ingredientId,
      total: item.totalQuantity,
      unit: item.unit,
      meals: item.sourceMealTypes,
      dishes: item.sourceDishes
    })),
    [
      {
        id: 'egg',
        total: 8,
        unit: '个',
        meals: ['早餐', '午餐'],
        dishes: ['鸡蛋饼', '番茄炒蛋']
      },
      {
        id: 'tomato',
        total: 4,
        unit: '个',
        meals: ['午餐'],
        dishes: ['番茄炒蛋']
      },
      {
        id: 'flour',
        total: 160,
        unit: '克',
        meals: ['早餐'],
        dishes: ['鸡蛋饼']
      },
      {
        id: 'oil',
        total: 10,
        unit: '毫升',
        meals: ['早餐'],
        dishes: ['鸡蛋饼']
      }
    ]
  );
});

test('filters purchase list by meal type', () => {
  const list = buildPurchaseList({
    dishes,
    date: '2026-06-06',
    mealType: 'breakfast',
    orders: [
      {
        id: 'o1',
        mealDate: '2026-06-06',
        mealType: 'breakfast',
        peopleCount: 2,
        dishes: [{ dishId: 'egg-pancake', count: 1 }]
      },
      {
        id: 'o2',
        mealDate: '2026-06-06',
        mealType: 'dinner',
        peopleCount: 2,
        dishes: [{ dishId: 'tomato-egg', count: 1 }]
      }
    ]
  });

  assert.deepEqual(list.items.map((item) => item.ingredientId), ['egg', 'flour', 'oil']);
});
