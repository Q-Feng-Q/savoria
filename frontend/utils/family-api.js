async function loadFamilyBundle(runtime, options = {}) {
  const homeData = await runtime.family.getHome();

  return {
    homeData,
    serviceDate: options.date || homeData.serviceDate
  };
}

module.exports = {
  loadFamilyBundle
};
