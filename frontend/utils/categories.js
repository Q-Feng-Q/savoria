const DEFAULT_DISH_CATEGORIES = ['早餐', '家常菜', '主食', '汤', '凉菜', '甜品', '饮品', '其他'];

function mergeDishCategories(predefined = DEFAULT_DISH_CATEGORIES, dishes = []) {
  const categories = [...predefined];

  dishes.forEach((dish) => {
    if (dish.category && !categories.includes(dish.category)) {
      categories.push(dish.category);
    }
  });

  return categories;
}

function findCategoryIndex(categories, category) {
  const index = categories.indexOf(category);
  return index >= 0 ? index : Math.max(0, categories.indexOf('其他'));
}

module.exports = {
  DEFAULT_DISH_CATEGORIES,
  mergeDishCategories,
  findCategoryIndex
};
