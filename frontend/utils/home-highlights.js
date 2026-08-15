function countOrderItems(dishes = []) {
  return dishes.reduce((sum, item) => sum + Number(item.count || 0), 0);
}

function isActiveOrder(order, currentDate) {
  return order.mealDate === currentDate && order.status !== 'cancelled';
}

function buildHomeSummary({
  dishes = [],
  orders = [],
  currentDate,
  currentMealType = 'lunch',
  mealOverview = {}
}) {
  const availableDishCount = dishes.filter((dish) => dish.status === 'on').length;
  const todaysOrders = orders.filter((order) => isActiveOrder(order, currentDate));
  const currentMeal = mealOverview[currentMealType] || {};

  return {
    availableDishCount,
    todayOrderCount: todaysOrders.length,
    todayOrderDishCount: todaysOrders.reduce((sum, order) => sum + countOrderItems(order.dishes), 0),
    currentMealCartCount: Number(currentMeal.cartCount || 0),
    currentMealOrderCount: Number(currentMeal.orderCount || 0),
    currentMealPeopleCount: Number(currentMeal.peopleCount || 0)
  };
}

function buildFeaturedDishes({
  dishes = [],
  currentMealType = 'lunch',
  limit = 3,
  getSoldCount = () => 0,
  getSelectedCount = () => 0
}) {
  return dishes
    .filter((dish) => dish.status === 'on')
    .map((dish) => {
      const soldCount = Number(getSoldCount(dish.id) || 0);
      const selectedCount = Number(getSelectedCount(dish.id, currentMealType) || 0);

      return {
        ...dish,
        soldCount,
        selectedCount,
        hasSelected: selectedCount > 0,
        featuredReasonKey: soldCount > 0 ? 'sold' : (selectedCount > 0 ? 'selected' : 'fresh')
      };
    })
    .sort((left, right) => {
      if (right.soldCount !== left.soldCount) return right.soldCount - left.soldCount;
      if (right.selectedCount !== left.selectedCount) return right.selectedCount - left.selectedCount;
      return String(left.name || '').localeCompare(String(right.name || ''));
    })
    .slice(0, limit);
}

module.exports = {
  buildHomeSummary,
  buildFeaturedDishes
};
