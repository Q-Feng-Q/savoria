function getDishDetailView(dish, role) {
  if (!dish) return null;

  const detail = {
    id: dish.id,
    name: dish.name,
    category: dish.category,
    description: dish.description,
    baseServings: dish.baseServings,
    tasteTags: dish.tasteTags || [],
    status: dish.status,
    accent: dish.accent,
    icon: dish.icon,
    ingredients: dish.ingredients || [],
    canViewCookingSteps: role === 'merchant'
  };

  if (detail.canViewCookingSteps) {
    detail.cookingSteps = dish.cookingSteps || [];
  }

  return detail;
}

module.exports = {
  getDishDetailView
};
