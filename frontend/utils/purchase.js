const MEAL_LABELS = {
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐'
};

const CATEGORY_ORDER = ['蛋奶', '蔬菜', '肉类', '主食', '调料', '干货', '其他'];

function roundQuantity(value) {
  return Math.round((Number(value) || 0) * 100) / 100;
}

function uniqueSorted(values) {
  return Array.from(new Set(values));
}

function categoryRank(category) {
  const index = CATEGORY_ORDER.indexOf(category);
  return index >= 0 ? index : CATEGORY_ORDER.length;
}

function calculateQuantity(ingredient, order, dishItem, dish) {
  const count = Number(dishItem.count || 1);
  const baseQuantity = Number(ingredient.quantity || 0);

  if (ingredient.calculationType === 'fixed') {
    return baseQuantity * count;
  }

  const baseServings = Number(dish.baseServings || 1);
  const peopleCount = Number(order.peopleCount || baseServings);
  return (baseQuantity * peopleCount * count) / baseServings;
}

function buildPurchaseList({ orders = [], dishes = [], date, mealType }) {
  const dishMap = new Map(dishes.map((dish) => [dish.id, dish]));
  const rows = new Map();

  orders
    .filter((order) => !date || order.mealDate === date)
    .filter((order) => !mealType || order.mealType === mealType)
    .filter((order) => order.status !== 'cancelled')
    .forEach((order) => {
      (order.dishes || []).forEach((dishItem) => {
        const dish = dishMap.get(dishItem.dishId);
        if (!dish) return;

        (dish.ingredients || []).forEach((ingredient) => {
          if (ingredient.calculationType === 'no_purchase') return;

          const key = `${ingredient.id}__${ingredient.unit}`;
          const current = rows.get(key) || {
            ingredientId: ingredient.id,
            ingredientName: ingredient.name,
            category: ingredient.category || '其他',
            totalQuantity: 0,
            unit: ingredient.unit,
            sourceMealTypes: [],
            sourceDishes: [],
            sourceOrders: [],
            breakdown: []
          };
          const quantity = calculateQuantity(ingredient, order, dishItem, dish);

          current.totalQuantity += quantity;
          current.sourceMealTypes.push(MEAL_LABELS[order.mealType] || order.mealType);
          current.sourceDishes.push(dish.name);
          current.sourceOrders.push(order.id);
          current.breakdown.push({
            orderId: order.id,
            mealType: order.mealType,
            mealLabel: MEAL_LABELS[order.mealType] || order.mealType,
            dishName: dish.name,
            quantity: roundQuantity(quantity),
            unit: ingredient.unit
          });

          rows.set(key, current);
        });
      });
    });

  const items = Array.from(rows.values()).map((item) => ({
    ...item,
    totalQuantity: roundQuantity(item.totalQuantity),
    sourceMealTypes: uniqueSorted(item.sourceMealTypes),
    sourceDishes: uniqueSorted(item.sourceDishes),
    sourceOrders: uniqueSorted(item.sourceOrders)
  }));

  items.sort((a, b) => {
    const categoryDelta = categoryRank(a.category) - categoryRank(b.category);
    if (categoryDelta !== 0) return categoryDelta;
    return a.ingredientName.localeCompare(b.ingredientName, 'zh-Hans-CN');
  });

  return {
    date,
    mealType: mealType || 'all',
    items
  };
}

module.exports = {
  MEAL_LABELS,
  buildPurchaseList,
  calculateQuantity,
  roundQuantity
};
