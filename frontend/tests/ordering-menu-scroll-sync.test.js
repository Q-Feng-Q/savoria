const test = require('node:test');
const assert = require('node:assert/strict');

const pagePath = require.resolve('../pages/ordering/menu/index.js');

function loadMenuPage(wxOverrides = {}) {
  const previousPage = global.Page;
  const previousWx = global.wx;
  let definition;
  global.Page = (value) => { definition = value; };
  global.wx = {
    nextTick(callback) { callback(); },
    navigateTo() {},
    switchTab() {},
    showToast() {},
    ...wxOverrides
  };
  delete require.cache[pagePath];
  require(pagePath);
  const page = {
    ...definition,
    data: JSON.parse(JSON.stringify(definition.data)),
    setData(patch, callback) {
      Object.assign(this.data, patch);
      if (callback) callback();
    }
  };
  return {
    page,
    cleanup() {
      delete require.cache[pagePath];
      global.Page = previousPage;
      global.wx = previousWx;
    }
  };
}

function seedCategories(page) {
  page.data.categoryOptions = [
    { key: 'all', label: '全部', activeClass: 'active' },
    { key: '3', label: '热菜', activeClass: '' },
    { key: '4', label: '汤羹', activeClass: '' }
  ];
  page.data.menuSections = [
    { key: '3', label: '热菜', anchorId: 'menu-section-3', cards: [{}] },
    { key: '4', label: '汤羹', anchorId: 'menu-section-4', cards: [{}] }
  ];
}

test('type changes scroll to the first surviving category and retain its highlight', () => {
  const loaded = loadMenuPage();
  try {
    const { page } = loaded;
    page.scheduleSectionMeasurement = () => {};
    page.sceneSource = { homeData: { family: {}, member: {} }, runtime: { baseUrl: '' }, menuItems: [
      { dishId: 1, categoryId: 7, categoryName: '汤羹', name: '银耳羹', productType: 'NOURISHMENT' }
    ] };
    page.sectionOffsets = [{ key: 'old', top: 0 }];
    page.changeProductType({ detail: { value: 2 } });
    assert.equal(page.pendingScrollCategoryKey, '7');
    assert.equal(page.data.dishScrollIntoView, 'menu-section-7');
    page.handleDishScroll({ detail: { scrollTop: 0 } });
    assert.equal(page.data.activeCategoryKey, '7');
    page.sectionOffsets = [{ key: '7', top: 40 }];
    page.handleDishScroll({ detail: { scrollTop: 40 } });
    assert.equal(page.data.activeCategoryKey, '7');
    assert.equal(page.pendingScrollCategoryKey, null);
  } finally { loaded.cleanup(); }
});

test('left category taps target the right anchor without filtering the menu', () => {
  const loaded = loadMenuPage();
  try {
    const { page } = loaded;
    seedCategories(page);
    const sections = page.data.menuSections;

    page.selectCategory({ currentTarget: { dataset: { key: '4' } } });

    assert.equal(page.data.activeCategoryKey, '4');
    assert.equal(page.data.activeCategoryLabel, '汤羹');
    assert.equal(page.data.dishScrollIntoView, 'menu-section-4');
    assert.equal(page.data.categoryScrollIntoView, 'menu-category-4');
    assert.equal(page.data.dishScrollWithAnimation, true);
    assert.equal(page.data.menuSections, sections);

    page.selectCategory({ currentTarget: { dataset: { key: 'all' } } });
    assert.equal(page.data.dishScrollIntoView, 'menu-list-top');
  } finally {
    loaded.cleanup();
  }
});

test('right dish scrolling activates the section currently crossing the top threshold', () => {
  const loaded = loadMenuPage();
  try {
    const { page } = loaded;
    seedCategories(page);
    page.sectionOffsets = [
      { key: '3', top: 80 },
      { key: '4', top: 420 }
    ];

    page.handleDishScroll({ detail: { scrollTop: 100 } });
    assert.equal(page.data.activeCategoryKey, '3');
    assert.equal(page.data.categoryScrollIntoView, 'menu-category-3');

    page.handleDishScroll({ detail: { scrollTop: 450 } });
    assert.equal(page.data.activeCategoryKey, '4');
    assert.equal(page.data.categoryOptions.find((item) => item.key === '4').activeClass, 'active');
  } finally {
    loaded.cleanup();
  }
});

test('section measurement records stable offsets relative to the right scroll view', () => {
  let query;
  const loaded = loadMenuPage({
    createSelectorQuery() {
      query = {
        in() { return query; },
        select() { return { boundingClientRect() { return query; } }; },
        selectAll() { return { boundingClientRect() { return query; } }; },
        exec(callback) {
          callback([
            { top: 100 },
            [
              { top: 140, dataset: { key: '3' } },
              { top: 460, dataset: { key: '4' } }
            ]
          ]);
        }
      };
      return query;
    }
  });
  try {
    const { page } = loaded;
    page.currentDishScrollTop = 120;
    page.measureMenuSections();
    assert.deepEqual(page.sectionOffsets, [
      { key: '3', top: 160 },
      { key: '4', top: 480 }
    ]);
  } finally {
    loaded.cleanup();
  }
});

test('search requests rebuild sections and return the right list to the top', () => {
  const loaded = loadMenuPage();
  try {
    const { page } = loaded;
    let options;
    page.refreshView = (value) => { options = value; };

    page.handleSearchInput({ detail: { value: '排骨' } });
    assert.deepEqual(options, { searchKeyword: '排骨', activeCategoryKey: 'all', resetScroll: true });

    page.clearSearch();
    assert.deepEqual(options, { searchKeyword: '', activeCategoryKey: 'all', resetScroll: true });
  } finally {
    loaded.cleanup();
  }
});

test('resetting the list replaces a pending category scroll with the top anchor', () => {
  const loaded = loadMenuPage();
  try {
    const { page } = loaded;
    page.sceneSource = {
      homeData: {
        family: { familyId: 2, familyName: '林家', merchantId: 1, merchantName: '老祁' },
        member: { memberId: 10, name: '小林', roleTemplate: 'member' }
      },
      menuItems: [],
      cart: { serverDate: '2026-09-05', items: [] },
      runtime: { baseUrl: '' }
    };
    page.pendingScrollCategoryKey = '4';
    page.scheduleSectionMeasurement = () => {};

    page.refreshView({ searchKeyword: '排骨', activeCategoryKey: 'all', resetScroll: true });

    assert.equal(page.pendingScrollCategoryKey, 'all');
    assert.equal(page.data.dishScrollIntoView, 'menu-list-top');
    assert.equal(page.data.dishScrollWithAnimation, false);
  } finally {
    loaded.cleanup();
  }
});
