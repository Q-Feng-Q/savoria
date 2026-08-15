function buildMealStatusList({ mealTypes = [], activeMealType = 'lunch', overview = {} }) {
  return mealTypes.map((meal) => {
    const mealOverview = overview[meal.value] || {};
    return {
      ...meal,
      activeClass: activeMealType === meal.value ? 'active' : '',
      statusText: mealOverview.statusText || '未点',
      peopleText: `${Number(mealOverview.peopleCount || 1)}人`
    };
  });
}

module.exports = {
  buildMealStatusList
};
