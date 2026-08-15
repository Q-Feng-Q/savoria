function normalizeKeyword(keyword = '') {
  return String(keyword).trim();
}

function hasKeyword(dish, keyword) {
  if (!keyword) return true;
  const ingredientHit = (dish.ingredients || []).some((ingredient) => String(ingredient.name || '').includes(keyword));
  return String(dish.name || '').includes(keyword) || ingredientHit;
}

function buildDishList({
  dishes = [],
  category = '全部',
  keyword = '',
  activeMealType,
  getSelectedCount = () => 0,
  getSoldCount = () => 0
}) {
  const query = normalizeKeyword(keyword);

  return dishes
    .filter((dish) => dish.status === 'on')
    .filter((dish) => category === '全部' || dish.category === category)
    .filter((dish) => hasKeyword(dish, query))
    .map((dish) => {
      const selectedCount = Number(getSelectedCount(dish.id, activeMealType) || 0);
      const soldCount = Number(getSoldCount(dish.id) || 0);
      return {
        ...dish,
        selectedCount,
        hasSelected: selectedCount > 0,
        selectedText: selectedCount > 0 ? `已选 ${selectedCount} 份` : '',
        soldCount,
        soldText: `销量 ${soldCount}`
      };
    });
}

function buildDishSections(dishes = [], categoryNames = []) {
  const names = categoryNames.length
    ? categoryNames
    : Array.from(new Set(dishes.map((dish) => dish.category)));

  return names
    .map((name) => ({
      name,
      dishes: dishes.filter((dish) => dish.category === name)
    }))
    .filter((section) => section.dishes.length > 0);
}

function pickRandomDish(dishes = [], randomSource = Math.random) {
  if (!dishes.length) return null;
  const index = Math.min(dishes.length - 1, Math.floor(randomSource() * dishes.length));
  return dishes[index];
}

module.exports = {
  buildDishList,
  buildDishSections,
  pickRandomDish
};
