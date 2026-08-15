(function () {
  const stateLib = window.KitchenAdminState;
  const seed = window.KITCHEN_ADMIN_DATA;

  if (!stateLib || !seed) {
    return;
  }

  let state = stateLib.createAdminState(seed);
  let currentView = 'dashboard';
  let toastTimer = null;
  const uiState = {
    menuFamilyId: state.families[0] ? state.families[0].id : '',
    purchaseFilter: 'all',
    notificationFilter: 'all'
  };

  const refs = {
    viewTitle: document.getElementById('view-title'),
    viewSubtitle: document.getElementById('view-subtitle'),
    metricGrid: document.getElementById('metric-grid'),
    contentGrid: document.getElementById('content-grid'),
    metricTemplate: document.getElementById('metric-card-template'),
    panelTemplate: document.getElementById('panel-template'),
    navItems: Array.from(document.querySelectorAll('.nav-item')),
    primaryActionButton: document.getElementById('topbar-primary'),
    secondaryActionButton: document.getElementById('topbar-secondary'),
    drawerMask: document.getElementById('drawer-mask'),
    drawer: document.getElementById('drawer'),
    drawerTitle: document.getElementById('drawer-title'),
    drawerSubtitle: document.getElementById('drawer-subtitle'),
    drawerBody: document.getElementById('drawer-body'),
    drawerFoot: document.getElementById('drawer-foot'),
    drawerClose: document.getElementById('drawer-close'),
    toast: document.getElementById('toast')
  };

  const statusLabelMap = {
    pending: '待确认',
    confirmed: '已确认',
    preparing: '备菜中',
    ready: '待交付',
    done: '已完成',
    cancelled: '已取消',
    rejected: '已拒单',
    active: '已上架',
    inactive: '已下架',
    estimated: '预估',
    manual: '手动补录'
  };

  const topbarActions = {
    dashboard: {
      primary: { label: '新增菜品', handler: openNewDishDrawer },
      secondary: { label: '进入订单', handler: function () { setView('orders'); } }
    },
    orders: {
      primary: { label: '采购预览', handler: openPurchasePreviewDrawer },
      secondary: { label: '刷新列表', handler: function () { rerender('订单列表已刷新'); } }
    },
    dishes: {
      primary: { label: '新增菜品', handler: openNewDishDrawer },
      secondary: { label: '查看菜单', handler: function () { setView('menus'); } }
    },
    ingredients: {
      primary: { label: '新增食材', handler: openNewIngredientDrawer },
      secondary: { label: '返回菜品', handler: function () { setView('dishes'); } }
    },
    families: {
      primary: { label: '家庭菜单', handler: function () { setView('menus'); } },
      secondary: { label: '查看订单', handler: function () { setView('orders'); } }
    },
    menus: {
      primary: { label: '复制菜单', handler: openFamilyMenuCopyDrawer },
      secondary: { label: '切到家庭', handler: function () { setView('families'); } }
    },
    purchase: {
      primary: { label: '补录采购', handler: openManualPurchaseDrawer },
      secondary: { label: '采购提醒', handler: function () { setView('notifications'); } }
    },
    notifications: {
      primary: { label: '全部已读', handler: markMerchantNotificationsRead },
      secondary: { label: '看采购', handler: function () { setView('purchase'); } }
    }
  };

  refs.navItems.forEach(function (item) {
    item.addEventListener('click', function () {
      setView(item.dataset.view);
    });
  });

  refs.drawerMask.addEventListener('click', closeDrawer);
  refs.drawerClose.addEventListener('click', closeDrawer);
  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
      closeDrawer();
    }
  });

  function formatCurrency(value) {
    return '¥' + Number(value || 0);
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function joinNames(list) {
    return (list || []).join('、');
  }

  function getSelectedMenuFamily() {
    return stateLib.getEntityById(state.families, uiState.menuFamilyId) || state.families[0] || null;
  }

  function getFamilyMembers(family) {
    return (family && family.members) || [];
  }

  function getMemberById(family, memberId) {
    return getFamilyMembers(family).find(function (member) {
      return member.id === memberId;
    }) || null;
  }

  function getMemberStatusText(member) {
    if (!member) return '-';
    if (member.status === 'low') return '余额偏低';
    return '正常';
  }

  function getFamilyForOrder(order) {
    return (state.families || []).find(function (family) {
      return family.name === order.familyName;
    }) || null;
  }

  function getFamilyAddresses(family) {
    return (family && family.addresses) || [];
  }

  function getNotificationActionLabel(notice) {
    if (!notice || !notice.targetView) return '';
    if (notice.targetView === 'orders') return '去看订单';
    if (notice.targetView === 'purchase') return '去看采购';
    if (notice.targetView === 'families') return '去看家庭';
    return '查看关联';
  }

  function getFamilyMenuRows(familyId) {
    return state.dishes.map(function (dish) {
      const entry = (state.familyMenus || []).find(function (item) {
        return item.familyId === familyId && item.dishId === dish.id;
      });
      return {
        id: dish.id,
        name: dish.name,
        category: dish.category,
        badge: dish.badge,
        status: dish.status,
        enabled: Boolean(entry && entry.enabled),
        finalPrice: entry ? entry.finalPrice : dish.price,
        basePrice: dish.price,
        taste: dish.taste
      };
    });
  }

  function getPurchaseItems() {
    return state.purchaseItems || [];
  }

  function getFilteredPurchaseItems() {
    const items = getPurchaseItems();
    if (uiState.purchaseFilter === 'estimated') {
      return items.filter(function (item) { return item.status === 'estimated'; });
    }
    if (uiState.purchaseFilter === 'confirmed') {
      return items.filter(function (item) { return item.status === 'confirmed'; });
    }
    if (uiState.purchaseFilter === 'manual') {
      return items.filter(function (item) { return item.manual; });
    }
    if (uiState.purchaseFilter === 'purchased') {
      return items.filter(function (item) { return item.purchased; });
    }
    return items;
  }

  function getMerchantNotifications() {
    return (state.notifications || []).filter(function (item) {
      return item.actorType === 'merchant';
    });
  }

  function getFilteredNotifications() {
    const items = getMerchantNotifications();
    if (uiState.notificationFilter === 'unread') {
      return items.filter(function (item) { return !item.read; });
    }
    if (uiState.notificationFilter === 'order') {
      return items.filter(function (item) { return item.category === 'order'; });
    }
    if (uiState.notificationFilter === 'purchase') {
      return items.filter(function (item) { return item.category === 'purchase'; });
    }
    if (uiState.notificationFilter === 'wallet') {
      return items.filter(function (item) { return item.category === 'wallet'; });
    }
    return items;
  }

  function showToast(message) {
    refs.toast.textContent = message;
    refs.toast.classList.add('is-visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      refs.toast.classList.remove('is-visible');
    }, 2200);
  }

  function openDrawer(config) {
    refs.drawerTitle.textContent = config.title || '详情';
    refs.drawerSubtitle.textContent = config.subtitle || '';
    refs.drawerBody.innerHTML = config.body || '';
    refs.drawerFoot.innerHTML = config.footer || '';
    refs.drawerMask.classList.remove('is-hidden');
    refs.drawer.classList.remove('is-hidden');
    requestAnimationFrame(function () {
      refs.drawer.classList.add('is-open');
    });
  }

  function closeDrawer() {
    refs.drawer.classList.remove('is-open');
    setTimeout(function () {
      refs.drawer.classList.add('is-hidden');
      refs.drawerMask.classList.add('is-hidden');
      refs.drawerBody.innerHTML = '';
      refs.drawerFoot.innerHTML = '';
    }, 180);
  }

  function createPanel(title, subtitle, actions) {
    const panel = refs.panelTemplate.content.firstElementChild.cloneNode(true);
    panel.querySelector('.panel-title').textContent = title;
    panel.querySelector('.panel-subtitle').textContent = subtitle || '';
    const actionsRoot = panel.querySelector('.panel-actions');
    (actions || []).forEach(function (action) {
      const button = document.createElement('button');
      button.className = 'chip-button';
      button.textContent = action.label;
      button.addEventListener('click', action.handler);
      actionsRoot.appendChild(button);
    });
    return panel;
  }

  function createStatusBadge(status) {
    const span = document.createElement('span');
    span.className = 'status-badge status-' + status;
    span.textContent = statusLabelMap[status] || status;
    return span;
  }

  function createMiniAction(label, handler, danger) {
    const button = document.createElement('button');
    button.className = 'mini-button' + (danger ? ' danger-button' : '');
    button.textContent = label;
    button.addEventListener('click', handler);
    return button;
  }

  function summarizeMetrics(viewKey) {
    const processingOrders = state.orders.filter(function (item) {
      return ['pending', 'confirmed', 'preparing', 'ready'].includes(item.status);
    });
    const pendingOrders = state.orders.filter(function (item) { return item.status === 'pending'; });
    const preparingOrders = state.orders.filter(function (item) { return item.status === 'preparing'; });
    const readyOrders = state.orders.filter(function (item) { return item.status === 'ready'; });
    const activeDishes = state.dishes.filter(function (item) { return item.status === 'active'; });
    const inactiveDishes = state.dishes.filter(function (item) { return item.status === 'inactive'; });
    const hotDishes = state.dishes.filter(function (item) { return Number(item.soldCount || 0) >= 15; });
    const referencedIngredients = state.ingredients.filter(function (item) { return !item.removable; });
    const customIngredients = state.ingredients.filter(function (item) { return item.removable; });
    const deliverableFamilies = state.families.filter(function (item) { return item.deliveryEnabled; });
    const lowBalanceFamilies = state.families.filter(function (item) { return /偏低|冻结/.test(item.balance); });
    const totalMembers = state.families.reduce(function (sum, family) {
      return sum + Number(family.memberCount || 0);
    }, 0);
    const purchaseItems = getPurchaseItems();
    const estimatedItems = purchaseItems.filter(function (item) { return item.status === 'estimated'; });
    const purchasedItems = purchaseItems.filter(function (item) { return item.purchased; });
    const manualItems = purchaseItems.filter(function (item) { return item.manual; });
    const merchantNotifications = getMerchantNotifications();
    const unreadNotifications = merchantNotifications.filter(function (item) { return !item.read; });
    const currentMenuRows = getSelectedMenuFamily() ? getFamilyMenuRows(getSelectedMenuFamily().id) : [];
    const currentEnabledMenuRows = currentMenuRows.filter(function (item) { return item.enabled; });

    const map = {
      dashboard: [
        { label: '待处理订单', value: String(processingOrders.length), note: '含待确认、备菜中与待交付订单' },
        { label: '采购待核对', value: String(estimatedItems.length), note: '预估项需要确认后再采购' },
        { label: '余额提醒家庭', value: String(lowBalanceFamilies.length), note: '建议确认订单前同步提醒' },
        { label: '未读通知', value: String(unreadNotifications.length), note: '商户侧订单与采购提醒' }
      ],
      orders: [
        { label: '全部订单', value: String(state.orders.length), note: '当前商户名下全部订单' },
        { label: '待确认', value: String(pendingOrders.length), note: '优先处理新提交订单' },
        { label: '备菜中', value: String(preparingOrders.length), note: '允许取消，但必须填写原因' },
        { label: '待交付', value: String(readyOrders.length), note: '需关注配送或自取安排' }
      ],
      dishes: [
        { label: '菜品总数', value: String(state.dishes.length), note: '包含上架与下架菜品' },
        { label: '上架菜品', value: String(activeDishes.length), note: '家庭端可直接看到' },
        { label: '下架菜品', value: String(inactiveDishes.length), note: '保留历史做法与配置' },
        { label: '热销菜品', value: String(hotDishes.length), note: '累计销量 15 份以上' }
      ],
      ingredients: [
        { label: '食材总数', value: String(state.ingredients.length), note: '商户可维护的原材料字典' },
        { label: '已被引用', value: String(referencedIngredients.length), note: '当前不可直接删除' },
        { label: '自定义食材', value: String(customIngredients.length), note: '可单独维护或删除' },
        { label: '手动补录项', value: String(manualItems.length), note: '来自采购补录操作' }
      ],
      families: [
        { label: '服务家庭', value: String(state.families.length), note: '当前商户下全部家庭' },
        { label: '可配送家庭', value: String(deliverableFamilies.length), note: '其余家庭仅支持自取' },
        { label: '成员总数', value: String(totalMembers), note: '含管理员与普通成员' },
        { label: '余额提醒', value: String(lowBalanceFamilies.length), note: '家庭维度的余额风险' }
      ],
      menus: [
        { label: '当前家庭', value: getSelectedMenuFamily() ? getSelectedMenuFamily().name : '-', note: '正在配置的家庭菜单' },
        { label: '启用菜品', value: String(currentEnabledMenuRows.length), note: '当前家庭端可见菜品数' },
        { label: '全部菜品池', value: String(currentMenuRows.length), note: '来自商户主菜品库' },
        { label: '菜单来源', value: getSelectedMenuFamily() && getSelectedMenuFamily().menuSourceFamilyId ? '复制菜单' : '独立菜单', note: '可按家庭复制后再调整' }
      ],
      purchase: [
        { label: '采购总项', value: String(purchaseItems.length), note: '今日汇总采购项目' },
        { label: '预估项', value: String(estimatedItems.length), note: '来自待确认订单的预估采购' },
        { label: '已勾选', value: String(purchasedItems.length), note: '已确认采购完成的项目' },
        { label: '手动补录', value: String(manualItems.length), note: '商户额外补充采购项' }
      ],
      notifications: [
        { label: '全部通知', value: String(merchantNotifications.length), note: '商户端通知中心消息总数' },
        { label: '未读消息', value: String(unreadNotifications.length), note: '建议优先处理未读项' },
        { label: '订单提醒', value: String(merchantNotifications.filter(function (item) { return item.category === 'order'; }).length), note: '新单与订单变更类消息' },
        { label: '采购提醒', value: String(merchantNotifications.filter(function (item) { return item.category === 'purchase'; }).length), note: '采购预估变更与核对提醒' }
      ]
    };

    return map[viewKey] || map.dashboard;
  }

  function renderMetrics(metrics) {
    refs.metricGrid.innerHTML = '';
    metrics.forEach(function (metric) {
      const node = refs.metricTemplate.content.firstElementChild.cloneNode(true);
      node.querySelector('.metric-label').textContent = metric.label;
      node.querySelector('.metric-value').textContent = metric.value;
      node.querySelector('.metric-note').textContent = metric.note;
      refs.metricGrid.appendChild(node);
    });
  }

  function renderTopbarActions(viewKey) {
    const config = topbarActions[viewKey] || topbarActions.dashboard;
    refs.primaryActionButton.textContent = config.primary.label;
    refs.secondaryActionButton.textContent = config.secondary.label;
    refs.primaryActionButton.onclick = config.primary.handler;
    refs.secondaryActionButton.onclick = config.secondary.handler;
  }

  function setView(viewKey) {
    currentView = viewKey;
    const meta = stateLib.getViewMeta(viewKey);
    refs.navItems.forEach(function (item) {
      item.classList.toggle('is-active', item.dataset.view === viewKey);
    });
    refs.viewTitle.textContent = meta.title;
    refs.viewSubtitle.textContent = meta.subtitle;
    refs.contentGrid.classList.toggle('single', !!meta.single);
    renderTopbarActions(viewKey);
    renderMetrics(summarizeMetrics(viewKey));
    renderCurrentView();
  }

  function rerender(message) {
    setView(currentView);
    if (message) {
      showToast(message);
    }
  }

  function renderCurrentView() {
    refs.contentGrid.innerHTML = '';
    if (currentView === 'dashboard') renderDashboard();
    if (currentView === 'orders') renderOrders();
    if (currentView === 'dishes') renderDishes();
    if (currentView === 'ingredients') renderIngredients();
    if (currentView === 'families') renderFamilies();
    if (currentView === 'menus') renderMenus();
    if (currentView === 'purchase') renderPurchase();
    if (currentView === 'notifications') renderNotifications();
  }

  function renderDashboard() {
    const orderPanel = createPanel(
      '待处理订单',
      '先处理待确认，再推进备菜和待交付订单。',
      [{ label: '去订单管理', handler: function () { setView('orders'); } }]
    );
    const orderBody = document.createElement('div');
    orderBody.className = 'stack-list';

    state.orders
      .filter(function (order) {
        return ['pending', 'confirmed', 'preparing'].includes(order.status);
      })
      .slice(0, 4)
      .forEach(function (order) {
        const item = document.createElement('article');
        item.className = 'list-item';
        item.innerHTML = [
          '<div class="list-row">',
          '  <div>',
          '    <div class="item-title">' + escapeHtml(order.familyName) + ' · ' + escapeHtml(order.meal) + '</div>',
          '    <div class="item-subtitle">' + escapeHtml(order.orderNo) + ' · ' + escapeHtml(order.serviceDate) + '</div>',
          '  </div>',
          '</div>',
          '<div class="item-meta">' + escapeHtml(order.itemsSummary) + '</div>',
          '<div class="item-meta">' + escapeHtml(order.memberCharge) + '</div>',
          '<div class="item-actions"></div>'
        ].join('');
        item.querySelector('.list-row').appendChild(createStatusBadge(order.status));
        const actions = item.querySelector('.item-actions');
        actions.appendChild(createMiniAction('查看详情', function () {
          openOrderDetailDrawer(order.id);
        }));
        actions.appendChild(createMiniAction('推进状态', function () {
          state = stateLib.advanceOrderStatus(state, order.id);
          rerender('订单状态已更新');
        }));
        orderBody.appendChild(item);
      });
    orderPanel.querySelector('.panel-body').appendChild(orderBody);

    const purchasePanel = createPanel(
      '采购与提醒',
      '采购项和最新通知一起看，处理起来更顺手。',
      [{ label: '去采购清单', handler: function () { setView('purchase'); } }]
    );
    const purchaseBody = document.createElement('div');
    purchaseBody.className = 'section-stack';

    const purchaseTable = document.createElement('div');
    purchaseTable.className = 'table-card';
    purchaseTable.innerHTML = [
      '<table class="table">',
      '<thead><tr><th>食材</th><th>数量</th><th>状态</th><th>来源</th></tr></thead>',
      '<tbody>',
      getPurchaseItems().slice(0, 4).map(function (item) {
        return '<tr>' +
          '<td>' + escapeHtml(item.name) + '</td>' +
          '<td>' + escapeHtml(item.quantity) + '</td>' +
          '<td data-status="' + escapeHtml(item.status) + '"></td>' +
          '<td>' + escapeHtml(item.source) + '</td>' +
        '</tr>';
      }).join(''),
      '</tbody></table>'
    ].join('');
    Array.from(purchaseTable.querySelectorAll('[data-status]')).forEach(function (cell) {
      cell.appendChild(createStatusBadge(cell.dataset.status));
    });

    const latestNotices = document.createElement('div');
    latestNotices.className = 'notice-list';
    getMerchantNotifications().slice(0, 3).forEach(function (notice) {
      const item = document.createElement('article');
      item.className = 'notice-item';
      item.innerHTML = [
        '<div class="notice-head">',
        '  <div class="item-title">' + escapeHtml(notice.title) + '</div>',
        '</div>',
        '<div class="item-meta">' + escapeHtml(notice.content) + '</div>',
        '<div class="notice-meta">' + escapeHtml(notice.createdAt) + '</div>'
      ].join('');
      item.querySelector('.notice-head').appendChild(createStatusBadge(notice.read ? 'done' : 'pending'));
      latestNotices.appendChild(item);
    });

    purchaseBody.appendChild(purchaseTable);
    purchaseBody.appendChild(latestNotices);
    purchasePanel.querySelector('.panel-body').appendChild(purchaseBody);

    refs.contentGrid.appendChild(orderPanel);
    refs.contentGrid.appendChild(purchasePanel);
  }

  function renderOrders() {
    const panel = createPanel(
      '订单列表',
      '支持查看详情、调整配送费、推进状态，以及取消订单时记录原因。',
      [{ label: '采购预览', handler: openPurchasePreviewDrawer }]
    );
    const body = document.createElement('div');
    body.className = 'table-card';
    body.innerHTML = [
      '<table class="table">',
      '<thead><tr><th>家庭</th><th>餐次 / 日期</th><th>订单号</th><th>状态</th><th>菜品摘要</th><th>金额</th><th>交付</th><th>操作</th></tr></thead>',
      '<tbody>',
      state.orders.map(function (order) {
        return '<tr data-order-id="' + escapeHtml(order.id) + '">' +
          '<td>' + escapeHtml(order.familyName) + '</td>' +
          '<td>' + escapeHtml(order.meal) + '<div class="table-meta">' + escapeHtml(order.serviceDate) + '</div></td>' +
          '<td>' + escapeHtml(order.orderNo) + '</td>' +
          '<td class="order-status-cell"></td>' +
          '<td>' + escapeHtml(order.itemsSummary) + '</td>' +
          '<td>' + formatCurrency(order.totalAmount) + '<div class="table-meta">' + escapeHtml(order.memberCharge) + '</div></td>' +
          '<td>' + escapeHtml(order.deliveryMode) + '<div class="table-meta">配送费 ' + formatCurrency(order.deliveryFee) + '</div></td>' +
          '<td class="order-actions-cell"></td>' +
        '</tr>';
      }).join(''),
      '</tbody></table>'
    ].join('');

    Array.from(body.querySelectorAll('tbody tr')).forEach(function (row) {
      const order = stateLib.getEntityById(state.orders, row.dataset.orderId);
      row.querySelector('.order-status-cell').appendChild(createStatusBadge(order.status));
      const actions = document.createElement('div');
      actions.className = 'mini-actions';
      actions.appendChild(createMiniAction('详情', function () {
        openOrderDetailDrawer(order.id);
      }));
      actions.appendChild(createMiniAction('调费用', function () {
        openOrderEditDrawer(order.id);
      }));
      actions.appendChild(createMiniAction('推进', function () {
        state = stateLib.advanceOrderStatus(state, order.id);
        rerender('订单状态已推进');
      }));
      actions.appendChild(createMiniAction('取消', function () {
        openOrderCancelDrawer(order.id);
      }, true));
      row.querySelector('.order-actions-cell').appendChild(actions);
    });

    panel.querySelector('.panel-body').appendChild(body);
    refs.contentGrid.appendChild(panel);
  }

  function renderDishes() {
    const panel = createPanel(
      '菜品列表',
      '电脑端适合录入做法、家庭适配范围和更细的口味说明。',
      [{ label: '新增菜品', handler: openNewDishDrawer }]
    );
    const body = document.createElement('div');
    body.className = 'table-card';
    body.innerHTML = [
      '<table class="table">',
      '<thead><tr><th>菜品</th><th>分类</th><th>标签</th><th>状态</th><th>价格</th><th>食材 / 做法</th><th>适配家庭</th><th>操作</th></tr></thead>',
      '<tbody>',
      state.dishes.map(function (dish) {
        return '<tr data-dish-id="' + escapeHtml(dish.id) + '">' +
          '<td><strong>' + escapeHtml(dish.name) + '</strong><div class="table-meta">累计售出 ' + escapeHtml(dish.soldCount) + ' 份</div></td>' +
          '<td>' + escapeHtml(dish.category) + '</td>' +
          '<td><span class="tag-badge">' + escapeHtml(dish.badge) + '</span></td>' +
          '<td class="dish-status-cell"></td>' +
          '<td>' + formatCurrency(dish.price) + '</td>' +
          '<td>' + escapeHtml(joinNames(dish.ingredients)) + '<div class="table-meta">' + escapeHtml(dish.steps.length) + ' 步做法</div></td>' +
          '<td>' + escapeHtml(joinNames(dish.families)) + '</td>' +
          '<td class="dish-actions-cell"></td>' +
        '</tr>';
      }).join(''),
      '</tbody></table>'
    ].join('');

    Array.from(body.querySelectorAll('tbody tr')).forEach(function (row) {
      const dish = stateLib.getEntityById(state.dishes, row.dataset.dishId);
      row.querySelector('.dish-status-cell').appendChild(createStatusBadge(dish.status));
      const actions = document.createElement('div');
      actions.className = 'mini-actions';
      actions.appendChild(createMiniAction('编辑', function () {
        openDishFormDrawer(dish.id);
      }));
      actions.appendChild(createMiniAction('做法', function () {
        openDishDetailDrawer(dish.id);
      }));
      actions.appendChild(createMiniAction(dish.status === 'active' ? '下架' : '上架', function () {
        state = stateLib.toggleDishStatus(state, dish.id);
        rerender('菜品状态已更新');
      }));
      row.querySelector('.dish-actions-cell').appendChild(actions);
    });

    panel.querySelector('.panel-body').appendChild(body);
    refs.contentGrid.appendChild(panel);
  }

  function renderIngredients() {
    const panel = createPanel(
      '食材字典',
      '已被菜品引用的食材不会允许直接删除，避免采购清单和菜谱明细失真。',
      [{ label: '新增食材', handler: openNewIngredientDrawer }]
    );
    const body = document.createElement('div');
    body.className = 'table-card';
    body.innerHTML = [
      '<table class="table">',
      '<thead><tr><th>食材</th><th>分类</th><th>单位</th><th>使用情况</th><th>操作</th></tr></thead>',
      '<tbody>',
      state.ingredients.map(function (ingredient) {
        return '<tr data-ingredient-id="' + escapeHtml(ingredient.id) + '">' +
          '<td><strong>' + escapeHtml(ingredient.name) + '</strong></td>' +
          '<td>' + escapeHtml(ingredient.category) + '</td>' +
          '<td>' + escapeHtml(ingredient.unit) + '</td>' +
          '<td>' + escapeHtml(String(ingredient.usedCount)) + ' 道菜使用<div class="table-meta">' + escapeHtml(joinNames(ingredient.usedBy) || '当前还未被菜品使用') + '</div></td>' +
          '<td class="ingredient-actions-cell"></td>' +
        '</tr>';
      }).join(''),
      '</tbody></table>'
    ].join('');

    Array.from(body.querySelectorAll('tbody tr')).forEach(function (row) {
      const ingredient = stateLib.getEntityById(state.ingredients, row.dataset.ingredientId);
      const actions = document.createElement('div');
      actions.className = 'mini-actions';
      actions.appendChild(createMiniAction('编辑', function () {
        openIngredientFormDrawer(ingredient.id);
      }));
      if (ingredient.removable) {
        actions.appendChild(createMiniAction('删除', function () {
          try {
            state = stateLib.removeIngredient(state, ingredient.id);
            rerender('食材已删除');
          } catch (error) {
            showToast('该食材已被菜品引用，不能直接删除');
          }
        }, true));
      } else {
        const note = document.createElement('span');
        note.className = 'highlight-note';
        note.textContent = '已被引用，不可删';
        actions.appendChild(note);
      }
      row.querySelector('.ingredient-actions-cell').appendChild(actions);
    });

    panel.querySelector('.panel-body').appendChild(body);
    refs.contentGrid.appendChild(panel);
  }

  function renderFamilies() {
    const panel = createPanel(
      '家庭列表',
      '保留家庭配送方式、菜单来源和成员结构，方便你在后台统一查看和调整。',
      []
    );
    const body = document.createElement('div');
    body.className = 'family-grid';

    state.families.forEach(function (family) {
      const card = document.createElement('article');
      card.className = 'list-item';
      card.innerHTML = [
        '<div class="item-title">' + escapeHtml(family.name) + '</div>',
        '<div class="item-subtitle">' + escapeHtml(family.note) + '</div>',
        '<div class="summary-pair">',
        '  <div class="summary-box"><strong>' + escapeHtml(family.memberCount) + '</strong><span>家庭成员</span></div>',
        '  <div class="summary-box"><strong>' + escapeHtml(family.menuCount) + '</strong><span>生效菜品</span></div>',
        '</div>',
        '<div class="item-meta">' + escapeHtml(family.delivery) + '</div>',
        '<div class="item-meta">' + escapeHtml(family.balance) + '</div>',
        '<div class="item-meta">' + escapeHtml(family.source) + '</div>',
        '<div class="item-actions"></div>'
      ].join('');
      const actions = card.querySelector('.item-actions');
      actions.appendChild(createMiniAction('查看详情', function () {
        openFamilyDetailDrawer(family.id);
      }));
      actions.appendChild(createMiniAction('配送设置', function () {
        openFamilyDeliveryDrawer(family.id);
      }));
      actions.appendChild(createMiniAction('成员余额', function () {
        openFamilyBalanceOverviewDrawer(family.id);
      }));
      actions.appendChild(createMiniAction('配置菜单', function () {
        uiState.menuFamilyId = family.id;
        setView('menus');
      }));
      body.appendChild(card);
    });

    panel.querySelector('.panel-body').appendChild(body);
    refs.contentGrid.appendChild(panel);
  }

  function renderMenus() {
    const family = getSelectedMenuFamily();
    const rows = family ? getFamilyMenuRows(family.id) : [];
    const panel = createPanel(
      '家庭菜单配置',
      '可按家庭启停菜品、单独改价，并复制其他家庭菜单作为模板。',
      [{ label: '复制其他家庭菜单', handler: openFamilyMenuCopyDrawer }]
    );
    const body = document.createElement('div');
    body.className = 'section-stack';
    body.innerHTML = [
      '<div class="selector-grid" id="menu-family-selector"></div>',
      '<div class="table-card">',
      '<table class="table">',
      '<thead><tr><th>菜品</th><th>分类</th><th>状态</th><th>家庭菜单状态</th><th>基础价</th><th>家庭价</th><th>操作</th></tr></thead>',
      '<tbody>',
      rows.map(function (row) {
        return '<tr data-menu-dish-id="' + escapeHtml(row.id) + '">' +
          '<td><strong>' + escapeHtml(row.name) + '</strong><div class="table-meta">' + escapeHtml(row.taste || '') + '</div></td>' +
          '<td>' + escapeHtml(row.category) + '</td>' +
          '<td class="menu-dish-status-cell"></td>' +
          '<td class="menu-enabled-status-cell"></td>' +
          '<td>' + formatCurrency(row.basePrice) + '</td>' +
          '<td>' + formatCurrency(row.finalPrice) + '</td>' +
          '<td class="menu-actions-cell"></td>' +
        '</tr>';
      }).join(''),
      '</tbody></table>',
      '</div>'
    ].join('');

    const selector = body.querySelector('#menu-family-selector');
    state.families.forEach(function (item) {
      const card = document.createElement('button');
      card.className = 'selector-card' + (item.id === family.id ? ' is-active' : '');
      card.innerHTML = [
        '<div class="selector-title">' + escapeHtml(item.name) + '</div>',
        '<div class="selector-note">' + escapeHtml(item.source) + '</div>'
      ].join('');
      card.addEventListener('click', function () {
        uiState.menuFamilyId = item.id;
        rerender();
      });
      selector.appendChild(card);
    });

    Array.from(body.querySelectorAll('tbody tr')).forEach(function (row) {
      const dish = stateLib.getEntityById(state.dishes, row.dataset.menuDishId);
      const menuRow = rows.find(function (item) { return item.id === row.dataset.menuDishId; });
      row.querySelector('.menu-dish-status-cell').appendChild(createStatusBadge(dish.status));
      row.querySelector('.menu-enabled-status-cell').appendChild(createStatusBadge(menuRow.enabled ? 'active' : 'inactive'));

      const actions = document.createElement('div');
      actions.className = 'mini-actions';
      actions.appendChild(createMiniAction(menuRow.enabled ? '停用' : '启用', function () {
        state = stateLib.toggleFamilyMenuDish(state, family.id, dish.id);
        rerender('家庭菜单已更新');
      }));
      actions.appendChild(createMiniAction('改价格', function () {
        openFamilyMenuPriceDrawer(family.id, dish.id);
      }));
      row.querySelector('.menu-actions-cell').appendChild(actions);
    });

    panel.querySelector('.panel-body').appendChild(body);
    refs.contentGrid.appendChild(panel);
  }

  function renderPurchase() {
    const panel = createPanel(
      '采购清单',
      '支持统一看汇总、勾选已买、区分预估项，并补录临时采购。',
      [{ label: '手动补录采购项', handler: openManualPurchaseDrawer }]
    );
    const body = document.createElement('div');
    body.className = 'section-stack';
    body.innerHTML = [
      '<div class="pill-group" id="purchase-filters"></div>',
      '<div class="table-card">',
      '<table class="table">',
      '<thead><tr><th>食材</th><th>数量</th><th>分类</th><th>来源</th><th>状态</th><th>采购</th><th>操作</th></tr></thead>',
      '<tbody>',
      getFilteredPurchaseItems().map(function (item) {
        return '<tr data-purchase-id="' + escapeHtml(item.id) + '">' +
          '<td><strong>' + escapeHtml(item.name) + '</strong><div class="table-meta">' + escapeHtml(item.familyName) + ' · ' + escapeHtml(item.meal) + '</div></td>' +
          '<td>' + escapeHtml(item.quantity) + '</td>' +
          '<td>' + escapeHtml(item.category) + '</td>' +
          '<td>' + escapeHtml(item.source) + '</td>' +
          '<td class="purchase-status-cell"></td>' +
          '<td class="purchase-check-cell"></td>' +
          '<td class="purchase-actions-cell"></td>' +
        '</tr>';
      }).join(''),
      '</tbody></table>',
      '</div>'
    ].join('');

    const filterRoot = body.querySelector('#purchase-filters');
    [
      ['all', '全部'],
      ['estimated', '预估'],
      ['confirmed', '已确认'],
      ['manual', '手动补录'],
      ['purchased', '已勾选']
    ].forEach(function (pair) {
      const button = document.createElement('button');
      button.className = 'pill-button' + (uiState.purchaseFilter === pair[0] ? ' is-active' : '');
      button.textContent = pair[1];
      button.addEventListener('click', function () {
        uiState.purchaseFilter = pair[0];
        rerender();
      });
      filterRoot.appendChild(button);
    });

    Array.from(body.querySelectorAll('tbody tr')).forEach(function (row) {
      const item = stateLib.getEntityById(state.purchaseItems, row.dataset.purchaseId);
      row.querySelector('.purchase-status-cell').appendChild(createStatusBadge(item.status));
      row.querySelector('.purchase-check-cell').appendChild(createStatusBadge(item.purchased ? 'done' : 'pending'));

      const actions = document.createElement('div');
      actions.className = 'mini-actions';
      actions.appendChild(createMiniAction(item.purchased ? '取消勾选' : '标记已买', function () {
        state = stateLib.togglePurchaseChecked(state, item.id);
        rerender('采购状态已更新');
      }));
      actions.appendChild(createMiniAction('查看来源', function () {
        openPurchaseDetailDrawer(item.id);
      }));
      row.querySelector('.purchase-actions-cell').appendChild(actions);
    });

    panel.querySelector('.panel-body').appendChild(body);
    refs.contentGrid.appendChild(panel);
  }

  function renderNotifications() {
    const panel = createPanel(
      '通知中心',
      '先看未读，再处理订单和采购提醒。',
      [{ label: '全部标已读', handler: markMerchantNotificationsRead }]
    );
    const body = document.createElement('div');
    body.className = 'section-stack';
    body.innerHTML = [
      '<div class="pill-group" id="notification-filters"></div>',
      '<div class="notice-list" id="notification-list"></div>'
    ].join('');

    const filterRoot = body.querySelector('#notification-filters');
    [
      ['all', '全部'],
      ['unread', '未读'],
      ['order', '订单'],
      ['purchase', '采购'],
      ['wallet', '余额']
    ].forEach(function (pair) {
      const button = document.createElement('button');
      button.className = 'pill-button' + (uiState.notificationFilter === pair[0] ? ' is-active' : '');
      button.textContent = pair[1];
      button.addEventListener('click', function () {
        uiState.notificationFilter = pair[0];
        rerender();
      });
      filterRoot.appendChild(button);
    });

    const listRoot = body.querySelector('#notification-list');
    getFilteredNotifications().forEach(function (notice) {
      const item = document.createElement('article');
      item.className = 'notice-item';
      item.innerHTML = [
        '<div class="notice-head">',
        '  <div>',
        '    <div class="item-title">' + escapeHtml(notice.title) + '</div>',
        '    <div class="notice-meta">' + escapeHtml(notice.createdAt) + ' · ' + escapeHtml(notice.category) + '</div>',
        '  </div>',
        '</div>',
        '<div class="item-meta">' + escapeHtml(notice.content) + '</div>',
        '<div class="item-actions"></div>'
      ].join('');
      item.querySelector('.notice-head').appendChild(createStatusBadge(notice.read ? 'done' : 'pending'));
      const actions = item.querySelector('.item-actions');
      actions.appendChild(createMiniAction('查看详情', function () {
        openNotificationDetailDrawer(notice.id);
      }));
      if (notice.targetView) {
        actions.appendChild(createMiniAction(getNotificationActionLabel(notice), function () {
          jumpToNotificationTarget(notice);
        }));
      }
      if (!notice.read) {
        actions.appendChild(createMiniAction('标为已读', function () {
          state = stateLib.markNotificationRead(state, notice.id);
          rerender('通知已标记为已读');
        }));
      }
      listRoot.appendChild(item);
    });

    panel.querySelector('.panel-body').appendChild(body);
    refs.contentGrid.appendChild(panel);
  }

  function markMerchantNotificationsRead() {
    state = stateLib.markAllNotificationsRead(state, 'merchant');
    rerender('商户通知已全部标记为已读');
  }

  function jumpToNotificationTarget(notice) {
    if (!notice || !notice.targetView) return;

    if (notice.targetView === 'orders' && notice.targetId) {
      closeDrawer();
      setView('orders');
      openOrderDetailDrawer(notice.targetId);
      return;
    }

    if (notice.targetView === 'purchase' && notice.targetId) {
      closeDrawer();
      setView('purchase');
      openPurchaseDetailDrawer(notice.targetId);
      return;
    }

    if (notice.targetView === 'families' && notice.targetFamilyId) {
      closeDrawer();
      setView('families');
      if (notice.targetMemberId) {
        openMemberBalanceDrawer(notice.targetFamilyId, notice.targetMemberId);
      } else {
        openFamilyDetailDrawer(notice.targetFamilyId);
      }
    }
  }

  function openPurchasePreviewDrawer() {
    openDrawer({
      title: '今日采购预览',
      subtitle: '预估项保留标记，便于确认订单前二次核对。',
      body: [
        '<div class="detail-stack">',
        '  <div class="detail-section">',
        '    <h3>采购清单</h3>',
        '    <div class="table-card">',
        '      <table class="table">',
        '        <thead><tr><th>食材</th><th>数量</th><th>状态</th><th>来源</th></tr></thead>',
        '        <tbody>',
        getPurchaseItems().map(function (item) {
          return '<tr>' +
            '<td>' + escapeHtml(item.name) + '</td>' +
            '<td>' + escapeHtml(item.quantity) + '</td>' +
            '<td data-drawer-status="' + escapeHtml(item.status) + '"></td>' +
            '<td>' + escapeHtml(item.source) + '</td>' +
          '</tr>';
        }).join(''),
        '        </tbody>',
        '      </table>',
        '    </div>',
        '  </div>',
        '</div>'
      ].join(''),
      footer: '<button class="primary-button" id="drawer-purchase-close">我知道了</button>'
    });

    Array.from(refs.drawerBody.querySelectorAll('[data-drawer-status]')).forEach(function (cell) {
      cell.appendChild(createStatusBadge(cell.dataset.drawerStatus));
    });
    refs.drawerFoot.querySelector('#drawer-purchase-close').addEventListener('click', closeDrawer);
  }

  function openPurchaseDetailDrawer(purchaseId) {
    const item = stateLib.getEntityById(state.purchaseItems, purchaseId);
    if (!item) return;

    openDrawer({
      title: item.name,
      subtitle: '采购项来源详情',
      body: [
        '<div class="detail-stack">',
        '  <div class="detail-section">',
        '    <h3>基础信息</h3>',
        '    <div class="detail-grid">',
        '      <div class="detail-row"><span class="detail-label">数量</span><span class="detail-value">' + escapeHtml(item.quantity) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">分类</span><span class="detail-value">' + escapeHtml(item.category) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">家庭</span><span class="detail-value">' + escapeHtml(item.familyName) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">餐次</span><span class="detail-value">' + escapeHtml(item.meal) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">状态</span><span class="detail-value" id="drawer-purchase-status"></span></div>',
        '      <div class="detail-row"><span class="detail-label">采购进度</span><span class="detail-value">' + (item.purchased ? '已勾选已买' : '待采购') + '</span></div>',
        '    </div>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>来源说明</h3>',
        '    <div class="detail-value">' + escapeHtml(item.source) + '</div>',
        '  </div>',
        '</div>'
      ].join(''),
      footer: '<button class="primary-button" id="drawer-purchase-detail-close">关闭</button>'
    });

    refs.drawerBody.querySelector('#drawer-purchase-status').appendChild(createStatusBadge(item.status));
    refs.drawerFoot.querySelector('#drawer-purchase-detail-close').addEventListener('click', closeDrawer);
  }

  function openManualPurchaseDrawer() {
    openDrawer({
      title: '补录采购项',
      subtitle: '把非订单产生的临时采购也记到清单里。',
      body: [
        '<form class="form-grid" id="manual-purchase-form">',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="manual-purchase-name">采购名</label>',
        '      <input id="manual-purchase-name" name="name" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="manual-purchase-quantity">数量</label>',
        '      <input id="manual-purchase-quantity" name="quantity" placeholder="例如 2 卷 / 500g" />',
        '    </div>',
        '  </div>',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="manual-purchase-category">分类</label>',
        '      <input id="manual-purchase-category" name="category" value="临时用品" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="manual-purchase-source">来源</label>',
        '      <input id="manual-purchase-source" name="source" value="商户手动补充" />',
        '    </div>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-manual-purchase-cancel">取消</button>',
        '<button class="primary-button" id="drawer-manual-purchase-save">保存采购项</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-manual-purchase-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-manual-purchase-save').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#manual-purchase-form'));
      state = stateLib.addManualPurchaseItem(state, {
        name: formData.get('name'),
        quantity: formData.get('quantity'),
        category: formData.get('category'),
        source: formData.get('source')
      });
      closeDrawer();
      rerender('采购补录项已保存');
    });
  }

  function openNotificationDetailDrawer(notificationId) {
    const notice = stateLib.getEntityById(state.notifications, notificationId);
    if (!notice) return;

    openDrawer({
      title: notice.title,
      subtitle: notice.createdAt,
      body: [
        '<div class="detail-stack">',
        '  <div class="detail-section">',
        '    <h3>消息内容</h3>',
        '    <div class="detail-value">' + escapeHtml(notice.content) + '</div>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>消息属性</h3>',
        '    <div class="detail-grid">',
        '      <div class="detail-row"><span class="detail-label">类型</span><span class="detail-value">' + escapeHtml(notice.category) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">状态</span><span class="detail-value" id="drawer-notice-status"></span></div>',
        '    </div>',
        '  </div>',
        '</div>'
      ].join(''),
      footer: [
        notice.read ? '' : '<button class="ghost-button" id="drawer-notice-read">标为已读</button>',
        notice.targetView ? '<button class="ghost-button" id="drawer-notice-jump">' + getNotificationActionLabel(notice) + '</button>' : '',
        '<button class="primary-button" id="drawer-notice-close">关闭</button>'
      ].join('')
    });

    refs.drawerBody.querySelector('#drawer-notice-status').appendChild(createStatusBadge(notice.read ? 'done' : 'pending'));
    const readButton = refs.drawerFoot.querySelector('#drawer-notice-read');
    if (readButton) {
      readButton.addEventListener('click', function () {
        state = stateLib.markNotificationRead(state, notice.id);
        closeDrawer();
        rerender('通知已标记为已读');
      });
    }
    const jumpButton = refs.drawerFoot.querySelector('#drawer-notice-jump');
    if (jumpButton) {
      jumpButton.addEventListener('click', function () {
        jumpToNotificationTarget(notice);
      });
    }
    refs.drawerFoot.querySelector('#drawer-notice-close').addEventListener('click', closeDrawer);
  }

  function openOrderDetailDrawer(orderId) {
    const order = stateLib.getEntityById(state.orders, orderId);
    if (!order) return;
    const family = getFamilyForOrder(order);
    const defaultAddress = family && getFamilyAddresses(family).find(function (item) {
      return item.isDefault;
    });
    const familyDeliverySummary = family ? family.delivery : '未匹配到家庭信息';
    const addressChangedHint = family && defaultAddress && order.deliveryMode === '配送' && defaultAddress.addressText !== order.address
      ? '当前家庭默认地址与订单快照不同，配送仍应以下单时地址为准。'
      : '';

    openDrawer({
      title: order.familyName + ' · ' + order.meal,
      subtitle: '订单号 ' + order.orderNo,
      body: [
        '<div class="detail-stack">',
        '  <div class="detail-section">',
        '    <h3>基础信息</h3>',
        '    <div class="detail-grid">',
        '      <div class="detail-row"><span class="detail-label">状态</span><span class="detail-value" id="drawer-order-status"></span></div>',
        '      <div class="detail-row"><span class="detail-label">服务日期</span><span class="detail-value">' + escapeHtml(order.serviceDate) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">配送方式</span><span class="detail-value">' + escapeHtml(order.deliveryMode) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">配送费</span><span class="detail-value">' + formatCurrency(order.deliveryFee) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">收货地址</span><span class="detail-value">' + escapeHtml(order.address) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">提交时间</span><span class="detail-value">' + escapeHtml(order.submittedAt) + '</span></div>',
        '    </div>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>菜品明细</h3>',
        '    <div class="detail-list">',
               order.items.map(function (item) {
                 return '<div class="list-item"><div class="list-row"><div class="item-title">' + escapeHtml(item.name) +
                   '</div><div class="item-title">x' + escapeHtml(item.quantity) +
                   '</div></div><div class="item-meta">' + formatCurrency(item.price) + ' / 份</div></div>';
               }).join(''),
        '    </div>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>备注与结算</h3>',
        '    <div class="detail-grid">',
        '      <div class="detail-row"><span class="detail-label">成员分摊</span><span class="detail-value">' + escapeHtml(order.memberCharge) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">订单金额</span><span class="detail-value">' + formatCurrency(order.totalAmount) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">点餐备注</span><span class="detail-value">' + escapeHtml(order.remark || '无') + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">取消原因</span><span class="detail-value">' + escapeHtml(order.cancelReason || '无') + '</span></div>',
        '    </div>',
        '  </div>',
        '</div>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-edit-order">调整配送</button>',
        '<button class="ghost-button" id="drawer-open-order-family">查看家庭</button>',
        '<button class="ghost-button" id="drawer-cancel-order">取消订单</button>',
        '<button class="primary-button" id="drawer-advance-order">推进状态</button>'
      ].join('')
    });

    refs.drawerBody.querySelector('#drawer-order-status').appendChild(createStatusBadge(order.status));
    (function injectOrderFamilyContext() {
      const stack = refs.drawerBody.querySelector('.detail-stack');
      if (!stack) return;
      const section = document.createElement('section');
      section.className = 'detail-section';
      section.innerHTML = [
        '<h3>家庭配送上下文</h3>',
        '<div class="detail-grid">',
        '  <div class="detail-row"><span class="detail-label">当前家庭规则</span><span class="detail-value">' + escapeHtml(familyDeliverySummary) + '</span></div>',
        '  <div class="detail-row"><span class="detail-label">当前默认地址</span><span class="detail-value">' + escapeHtml(defaultAddress ? defaultAddress.addressText : '暂无默认地址') + '</span></div>',
        '</div>',
        addressChangedHint ? '<div class="field-hint">' + escapeHtml(addressChangedHint) + '</div>' : ''
      ].join('');
      stack.insertBefore(section, stack.children[1] || null);
    })();
    refs.drawerFoot.querySelector('#drawer-edit-order').addEventListener('click', function () {
      openOrderEditDrawer(order.id);
    });
    const familyButton = refs.drawerFoot.querySelector('#drawer-open-order-family');
    if (familyButton) {
      familyButton.addEventListener('click', function () {
        if (family) {
          openFamilyDetailDrawer(family.id);
        }
      });
    }
    refs.drawerFoot.querySelector('#drawer-cancel-order').addEventListener('click', function () {
      openOrderCancelDrawer(order.id);
    });
    refs.drawerFoot.querySelector('#drawer-advance-order').addEventListener('click', function () {
      state = stateLib.advanceOrderStatus(state, order.id);
      closeDrawer();
      rerender('订单状态已推进');
    });
  }

  function openOrderEditDrawer(orderId) {
    const order = stateLib.getEntityById(state.orders, orderId);
    if (!order) return;
    const family = getFamilyForOrder(order);
    const defaultAddress = family && getFamilyAddresses(family).find(function (item) {
      return item.isDefault;
    });
    const familyDeliverySummary = family ? family.delivery : '未匹配到家庭信息';

    openDrawer({
      title: '调整订单信息',
      subtitle: order.orderNo + ' · 可修改配送方式、配送费和备注',
      body: [
        '<form class="form-grid" id="order-form">',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="order-delivery-mode">配送方式</label>',
        '      <select id="order-delivery-mode" name="deliveryMode">',
        '        <option value="配送"' + (order.deliveryMode === '配送' ? ' selected' : '') + '>配送</option>',
        '        <option value="自取"' + (order.deliveryMode === '自取' ? ' selected' : '') + '>自取</option>',
        '      </select>',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="order-delivery-fee">配送费</label>',
        '      <input id="order-delivery-fee" name="deliveryFee" type="number" min="0" step="1" value="' + escapeHtml(order.deliveryFee) + '" />',
        '    </div>',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="order-remark">点餐备注</label>',
        '    <textarea id="order-remark" name="remark">' + escapeHtml(order.remark || '') + '</textarea>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-close-order-edit">取消</button>',
        '<button class="primary-button" id="drawer-save-order">保存调整</button>'
      ].join('')
    });

    (function injectOrderEditContext() {
      const form = refs.drawerBody.querySelector('#order-form');
      if (!form) return;
      const section = document.createElement('section');
      section.className = 'detail-section';
      section.innerHTML = [
        '<div class="detail-grid">',
        '  <div class="detail-row"><span class="detail-label">当前家庭规则</span><span class="detail-value">' + escapeHtml(familyDeliverySummary) + '</span></div>',
        '  <div class="detail-row"><span class="detail-label">当前默认地址</span><span class="detail-value">' + escapeHtml(defaultAddress ? defaultAddress.addressText : '暂无默认地址') + '</span></div>',
        '</div>',
        '<div class="field-hint">下单时按默认配送费冻结，商户确认前可调整，多退少补。</div>'
      ].join('');
      form.insertBefore(section, form.firstChild);
    })();
    refs.drawerFoot.querySelector('#drawer-close-order-edit').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-save-order').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#order-form'));
      state = stateLib.updateOrder(state, order.id, {
        deliveryMode: formData.get('deliveryMode'),
        deliveryFee: Number(formData.get('deliveryFee') || 0),
        remark: String(formData.get('remark') || '').trim()
      });
      closeDrawer();
      rerender('订单信息已保存');
    });
  }

  function openOrderCancelDrawer(orderId) {
    const order = stateLib.getEntityById(state.orders, orderId);
    if (!order) return;

    openDrawer({
      title: '取消订单',
      subtitle: '订单取消后需保留原因，便于家庭端和通知中心查看。',
      body: [
        '<form class="form-grid" id="cancel-order-form">',
        '  <div class="form-field">',
        '    <label for="cancel-reason">取消原因</label>',
        '    <textarea id="cancel-reason" name="cancelReason" placeholder="例如：食材缺货、配送冲突、用户改期">' + escapeHtml(order.cancelReason || '') + '</textarea>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-close-order-cancel">返回</button>',
        '<button class="primary-button" id="drawer-confirm-order-cancel">确认取消</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-close-order-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-confirm-order-cancel').addEventListener('click', function () {
      const reason = refs.drawerBody.querySelector('#cancel-reason').value;
      try {
        state = stateLib.cancelOrder(state, order.id, reason);
        closeDrawer();
        rerender('订单已取消，并记录原因');
      } catch (error) {
        showToast('请先填写取消原因');
      }
    });
  }

  function openDishDetailDrawer(dishId) {
    const dish = stateLib.getEntityById(state.dishes, dishId);
    if (!dish) return;

    openDrawer({
      title: dish.name,
      subtitle: '菜品详情与商户制作流程',
      body: [
        '<div class="detail-stack">',
        '  <div class="detail-section">',
        '    <h3>基础信息</h3>',
        '    <div class="detail-grid">',
        '      <div class="detail-row"><span class="detail-label">分类</span><span class="detail-value">' + escapeHtml(dish.category) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">标签</span><span class="detail-value">' + escapeHtml(dish.badge) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">价格</span><span class="detail-value">' + formatCurrency(dish.price) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">口味</span><span class="detail-value">' + escapeHtml(dish.taste || '未填写') + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">销量</span><span class="detail-value">累计售出 ' + escapeHtml(dish.soldCount) + ' 份</span></div>',
        '      <div class="detail-row"><span class="detail-label">适配家庭</span><span class="detail-value">' + escapeHtml(joinNames(dish.families)) + '</span></div>',
        '    </div>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>原材料</h3>',
        '    <ul class="detail-bullets">' + dish.ingredients.map(function (name) {
          return '<li>' + escapeHtml(name) + '</li>';
        }).join('') + '</ul>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>商户制作流程</h3>',
        '    <ol class="detail-bullets">' + dish.steps.map(function (step) {
          return '<li>' + escapeHtml(step) + '</li>';
        }).join('') + '</ol>',
        '  </div>',
        '</div>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-dish-edit-secondary">编辑菜品</button>',
        '<button class="primary-button" id="drawer-dish-close">我知道了</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-dish-edit-secondary').addEventListener('click', function () {
      openDishFormDrawer(dish.id);
    });
    refs.drawerFoot.querySelector('#drawer-dish-close').addEventListener('click', closeDrawer);
  }

  function openNewDishDrawer() {
    openDishFormDrawer();
  }

  function openDishFormDrawer(dishId) {
    const dish = dishId ? stateLib.getEntityById(state.dishes, dishId) : null;

    openDrawer({
      title: dish ? '编辑菜品' : '新增菜品',
      subtitle: '维护价格、分类、制作流程和适配家庭。',
      body: [
        '<form class="form-grid" id="dish-form">',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="dish-name">菜品名</label>',
        '      <input id="dish-name" name="name" value="' + escapeHtml(dish ? dish.name : '') + '" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="dish-category">分类</label>',
        '      <select id="dish-category" name="category">' +
              buildOptions(['家常热菜', '清爽蔬菜', '汤品', '早餐主食'], dish ? dish.category : '') +
        '      </select>',
        '    </div>',
        '  </div>',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="dish-price">价格</label>',
        '      <input id="dish-price" name="price" type="number" min="0" step="1" value="' + escapeHtml(dish ? dish.price : 0) + '" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="dish-status">状态</label>',
        '      <select id="dish-status" name="status">' +
              buildOptions(['active', 'inactive'], dish ? dish.status : 'active', { active: '已上架', inactive: '已下架' }) +
        '      </select>',
        '    </div>',
        '  </div>',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="dish-badge">标签</label>',
        '      <input id="dish-badge" name="badge" value="' + escapeHtml(dish ? dish.badge : '') + '" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="dish-taste">口味说明</label>',
        '      <input id="dish-taste" name="taste" value="' + escapeHtml(dish ? dish.taste : '') + '" />',
        '    </div>',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="dish-ingredients">原材料</label>',
        '    <input id="dish-ingredients" name="ingredients" value="' + escapeHtml(dish ? dish.ingredients.join('、') : '') + '" />',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="dish-families">适配家庭</label>',
        '    <input id="dish-families" name="families" value="' + escapeHtml(dish ? dish.families.join('、') : '') + '" />',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="dish-steps">商户制作流程</label>',
        '    <textarea id="dish-steps" name="steps">' + escapeHtml(dish ? dish.steps.join('\n') : '') + '</textarea>',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="dish-sold">累计销量</label>',
        '    <input id="dish-sold" name="soldCount" type="number" min="0" step="1" value="' + escapeHtml(dish ? dish.soldCount : 0) + '" />',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-dish-cancel">取消</button>',
        '<button class="primary-button" id="drawer-dish-save">保存菜品</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-dish-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-dish-save').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#dish-form'));
      state = stateLib.saveDish(state, {
        id: dish ? dish.id : '',
        name: formData.get('name'),
        category: formData.get('category'),
        badge: formData.get('badge'),
        status: formData.get('status'),
        price: formData.get('price'),
        ingredients: formData.get('ingredients'),
        steps: formData.get('steps'),
        families: formData.get('families'),
        soldCount: formData.get('soldCount'),
        taste: formData.get('taste')
      });
      closeDrawer();
      rerender('菜品已保存');
    });
  }

  function openNewIngredientDrawer() {
    openIngredientFormDrawer();
  }

  function openIngredientFormDrawer(ingredientId) {
    const ingredient = ingredientId ? stateLib.getEntityById(state.ingredients, ingredientId) : null;

    openDrawer({
      title: ingredient ? '编辑食材' : '新增食材',
      subtitle: '维护单位、分类和是否允许删除。',
      body: [
        '<form class="form-grid" id="ingredient-form">',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="ingredient-name">食材名</label>',
        '      <input id="ingredient-name" name="name" value="' + escapeHtml(ingredient ? ingredient.name : '') + '" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="ingredient-category">分类</label>',
        '      <select id="ingredient-category" name="category">' +
              buildOptions(['肉类', '蔬菜', '调料', '主食'], ingredient ? ingredient.category : '') +
        '      </select>',
        '    </div>',
        '  </div>',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="ingredient-unit">单位</label>',
        '      <input id="ingredient-unit" name="unit" value="' + escapeHtml(ingredient ? ingredient.unit : '') + '" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="ingredient-used-count">引用数量</label>',
        '      <input id="ingredient-used-count" name="usedCount" type="number" min="0" step="1" value="' + escapeHtml(ingredient ? ingredient.usedCount : 0) + '" />',
        '    </div>',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="ingredient-used-by">被哪些菜品使用</label>',
        '    <input id="ingredient-used-by" name="usedBy" value="' + escapeHtml(ingredient ? ingredient.usedBy.join('、') : '') + '" />',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="ingredient-removable">是否允许删除</label>',
        '    <select id="ingredient-removable" name="removable">' +
              buildOptions(['true', 'false'], ingredient ? String(ingredient.removable) : 'true', { true: '可删除', false: '不可删除' }) +
        '    </select>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-ingredient-cancel">取消</button>',
        '<button class="primary-button" id="drawer-ingredient-save">保存食材</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-ingredient-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-ingredient-save').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#ingredient-form'));
      state = stateLib.saveIngredient(state, {
        id: ingredient ? ingredient.id : '',
        name: formData.get('name'),
        unit: formData.get('unit'),
        category: formData.get('category'),
        usedCount: formData.get('usedCount'),
        usedBy: formData.get('usedBy'),
        removable: formData.get('removable') === 'true'
      });
      closeDrawer();
      rerender('食材已保存');
    });
  }

  function openFamilyDetailDrawer(familyId) {
    const family = stateLib.getEntityById(state.families, familyId);
    if (!family) return;
    const members = getFamilyMembers(family);
    const addresses = getFamilyAddresses(family);

    openDrawer({
      title: family.name,
      subtitle: '家庭端配置概览，可直接调整配送规则和成员余额。',
      body: [
        '<div class="detail-stack">',
        '  <div class="detail-section">',
        '    <h3>配送与菜单</h3>',
        '    <div class="detail-grid">',
        '      <div class="detail-row"><span class="detail-label">配送规则</span><span class="detail-value">' + escapeHtml(family.delivery) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">配送开关</span><span class="detail-value">' + escapeHtml(family.deliveryEnabled ? '已开启配送' : '仅支持自取') + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">默认配送费</span><span class="detail-value">' + formatCurrency(family.deliveryFeeDefault || 0) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">免配送费</span><span class="detail-value">' + escapeHtml(family.deliveryFree ? '是' : '否') + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">菜单来源</span><span class="detail-value">' + escapeHtml(family.source) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">生效菜品</span><span class="detail-value">' + escapeHtml(family.menuCount) + ' 道</span></div>',
        '      <div class="detail-row"><span class="detail-label">余额提醒</span><span class="detail-value">' + escapeHtml(family.balance) + '</span></div>',
        '    </div>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>家庭成员</h3>',
        '    <div class="member-list">' + members.map(function (member) {
          return [
            '<div class="member-card">',
            '  <div>',
            '    <div class="item-title">' + escapeHtml(member.name) + '</div>',
            '    <div class="item-subtitle">' + escapeHtml(member.role) + ' · ' + escapeHtml(getMemberStatusText(member)) + '</div>',
            '  </div>',
            '  <div class="member-balance">',
            '    <span>可用 ' + formatCurrency(member.availableBalance) + '</span>',
            '    <span>冻结 ' + formatCurrency(member.frozenBalance) + '</span>',
            '  </div>',
            '  <div class="mini-actions">',
            '    <button class="mini-button" data-family-balance-member="' + escapeHtml(member.id) + '">调余额</button>',
            '    <button class="mini-button" data-family-ledger-member="' + escapeHtml(member.id) + '">看流水</button>',
            '  </div>',
            '</div>'
          ].join('');
        }).join('') + '</div>',
        '  </div>',
        '  <div class="detail-section">',
        '    <h3>地址与备注</h3>',
        '    <div class="detail-grid">',
        '      <div class="detail-row"><span class="detail-label">当前默认地址</span><span class="detail-value">' + escapeHtml(family.address) + '</span></div>',
        '      <div class="detail-row"><span class="detail-label">家庭备注</span><span class="detail-value">' + escapeHtml(family.note) + '</span></div>',
        '    </div>',
        '    <div class="member-list">' + addresses.map(function (address) {
          return [
            '<div class="member-card">',
            '  <div class="list-row">',
            '    <div>',
            '      <div class="item-title">' + escapeHtml(address.tag || '常用地址') + '</div>',
            '      <div class="item-subtitle">' + escapeHtml(address.contactName) + ' · ' + escapeHtml(address.phone) + '</div>',
            '    </div>',
            address.isDefault ? '    <span class="status-badge status-active">默认地址</span>' : '',
            '  </div>',
            '  <div class="detail-value">' + escapeHtml(address.addressText) + '</div>',
            '  <div class="mini-actions">',
            '    <button class="mini-button" data-address-edit="' + escapeHtml(address.id) + '">编辑</button>',
            (address.isDefault ? '' : '    <button class="mini-button" data-address-default="' + escapeHtml(address.id) + '">设为默认</button>'),
            '  </div>',
            '</div>'
          ].join('');
        }).join('') + '</div>',
        '  </div>',
        '</div>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-family-delivery-open">配送设置</button>',
        '<button class="ghost-button" id="drawer-family-balance-open">成员余额</button>',
        '<button class="ghost-button" id="drawer-family-address-open">地址簿</button>',
        '<button class="ghost-button" id="drawer-family-menu-open">去配菜单</button>',
        '<button class="primary-button" id="drawer-family-close">关闭</button>'
      ].join('')
    });

    refs.drawerBody.querySelectorAll('[data-family-balance-member]').forEach(function (button) {
      button.addEventListener('click', function () {
        openMemberBalanceDrawer(family.id, button.dataset.familyBalanceMember);
      });
    });
    refs.drawerBody.querySelectorAll('[data-family-ledger-member]').forEach(function (button) {
      button.addEventListener('click', function () {
        openMemberLedgerDrawer(family.id, button.dataset.familyLedgerMember);
      });
    });
    refs.drawerBody.querySelectorAll('[data-address-edit]').forEach(function (button) {
      button.addEventListener('click', function () {
        openFamilyAddressFormDrawer(family.id, button.dataset.addressEdit);
      });
    });
    refs.drawerBody.querySelectorAll('[data-address-default]').forEach(function (button) {
      button.addEventListener('click', function () {
        state = stateLib.setDefaultFamilyAddress(state, family.id, button.dataset.addressDefault);
        closeDrawer();
        rerender('默认地址已切换');
      });
    });
    refs.drawerFoot.querySelector('#drawer-family-delivery-open').addEventListener('click', function () {
      openFamilyDeliveryDrawer(family.id);
    });
    refs.drawerFoot.querySelector('#drawer-family-balance-open').addEventListener('click', function () {
      openFamilyBalanceOverviewDrawer(family.id);
    });
    refs.drawerFoot.querySelector('#drawer-family-address-open').addEventListener('click', function () {
      openFamilyAddressBookDrawer(family.id);
    });
    refs.drawerFoot.querySelector('#drawer-family-menu-open').addEventListener('click', function () {
      uiState.menuFamilyId = family.id;
      closeDrawer();
      setView('menus');
    });
    refs.drawerFoot.querySelector('#drawer-family-close').addEventListener('click', closeDrawer);
  }

  function openFamilyDeliveryDrawer(familyId) {
    const family = stateLib.getEntityById(state.families, familyId);
    if (!family) return;

    openDrawer({
      title: '家庭配送设置',
      subtitle: family.name + ' · 调整配送开关、免配送和默认配送费',
      body: [
        '<form class="form-grid" id="family-delivery-form">',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="family-delivery-enabled">配送方式</label>',
        '      <select id="family-delivery-enabled" name="deliveryEnabled">',
        '        <option value="true"' + (family.deliveryEnabled ? ' selected' : '') + '>支持配送</option>',
        '        <option value="false"' + (!family.deliveryEnabled ? ' selected' : '') + '>仅支持自取</option>',
        '      </select>',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="family-delivery-free">配送费</label>',
        '      <select id="family-delivery-free" name="deliveryFree">',
        '        <option value="false"' + (!family.deliveryFree ? ' selected' : '') + '>按默认配送费</option>',
        '        <option value="true"' + (family.deliveryFree ? ' selected' : '') + '>免配送费</option>',
        '      </select>',
        '    </div>',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="family-delivery-fee">默认配送费</label>',
        '    <input id="family-delivery-fee" name="deliveryFeeDefault" type="number" min="0" step="1" value="' + escapeHtml(family.deliveryFeeDefault || 0) + '" />',
        '    <div class="field-hint">下单时按默认配送费冻结，商户确认前可以调整，多退少补。</div>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-family-delivery-cancel">取消</button>',
        '<button class="primary-button" id="drawer-family-delivery-save">保存设置</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-family-delivery-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-family-delivery-save').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#family-delivery-form'));
      const shouldEnable = formData.get('deliveryEnabled') === 'true';
      const shouldFree = formData.get('deliveryFree') === 'true';

      if (family.deliveryEnabled !== shouldEnable) {
        state = stateLib.toggleFamilyDeliveryEnabled(state, family.id);
      }
      const latestFamily = stateLib.getEntityById(state.families, family.id);
      if (latestFamily && latestFamily.deliveryFree !== shouldFree) {
        state = stateLib.toggleFamilyDeliveryFree(state, family.id);
      }
      state = stateLib.updateFamilyDeliveryFee(state, family.id, formData.get('deliveryFeeDefault'));
      closeDrawer();
      rerender('家庭配送设置已更新');
    });
  }

  function openFamilyBalanceOverviewDrawer(familyId) {
    const family = stateLib.getEntityById(state.families, familyId);
    if (!family) return;
    const members = getFamilyMembers(family);

    openDrawer({
      title: '成员余额管理',
      subtitle: family.name + ' · 查看可用余额、冻结金额和最近流水',
      body: [
        '<div class="detail-stack">',
        members.map(function (member) {
          const latestLedger = (member.ledger || [])[0];
          return [
            '<div class="detail-section">',
            '  <div class="list-row">',
            '    <div>',
            '      <div class="item-title">' + escapeHtml(member.name) + '</div>',
            '      <div class="item-subtitle">' + escapeHtml(member.role) + ' · ' + escapeHtml(getMemberStatusText(member)) + '</div>',
            '    </div>',
            '    <div class="mini-actions">',
            '      <button class="mini-button" data-balance-adjust-member="' + escapeHtml(member.id) + '">调余额</button>',
            '      <button class="mini-button" data-ledger-open-member="' + escapeHtml(member.id) + '">看流水</button>',
            '    </div>',
            '  </div>',
            '  <div class="detail-grid">',
            '    <div class="detail-row"><span class="detail-label">可用余额</span><span class="detail-value">' + formatCurrency(member.availableBalance) + '</span></div>',
            '    <div class="detail-row"><span class="detail-label">冻结金额</span><span class="detail-value">' + formatCurrency(member.frozenBalance) + '</span></div>',
            '    <div class="detail-row"><span class="detail-label">最近流水</span><span class="detail-value">' + escapeHtml(latestLedger ? latestLedger.note : '暂无流水') + '</span></div>',
            '    <div class="detail-row"><span class="detail-label">时间</span><span class="detail-value">' + escapeHtml(latestLedger ? latestLedger.createdAt : '-') + '</span></div>',
            '  </div>',
            '</div>'
          ].join('');
        }).join(''),
        '</div>'
      ].join(''),
      footer: [
        '<button class="primary-button" id="drawer-family-balance-close">我知道了</button>'
      ].join('')
    });

    refs.drawerBody.querySelectorAll('[data-balance-adjust-member]').forEach(function (button) {
      button.addEventListener('click', function () {
        openMemberBalanceDrawer(family.id, button.dataset.balanceAdjustMember);
      });
    });
    refs.drawerBody.querySelectorAll('[data-ledger-open-member]').forEach(function (button) {
      button.addEventListener('click', function () {
        openMemberLedgerDrawer(family.id, button.dataset.ledgerOpenMember);
      });
    });
    refs.drawerFoot.querySelector('#drawer-family-balance-close').addEventListener('click', closeDrawer);
  }

  function openMemberBalanceDrawer(familyId, memberId) {
    const family = stateLib.getEntityById(state.families, familyId);
    const member = getMemberById(family, memberId);
    if (!family || !member) return;

    openDrawer({
      title: '调整成员余额',
      subtitle: family.name + ' · ' + member.name,
      body: [
        '<form class="form-grid" id="member-balance-form">',
        '  <div class="detail-grid">',
        '    <div class="detail-row"><span class="detail-label">当前可用</span><span class="detail-value">' + formatCurrency(member.availableBalance) + '</span></div>',
        '    <div class="detail-row"><span class="detail-label">当前冻结</span><span class="detail-value">' + formatCurrency(member.frozenBalance) + '</span></div>',
        '  </div>',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="member-balance-type">调整类型</label>',
        '      <select id="member-balance-type" name="type">',
        '        <option value="recharge">充值</option>',
        '        <option value="adjust">人工调整</option>',
        '        <option value="refund">退款返还</option>',
        '      </select>',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="member-balance-amount">变动金额</label>',
        '      <input id="member-balance-amount" name="amount" type="number" step="1" value="0" />',
        '    </div>',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="member-balance-note">备注</label>',
        '    <textarea id="member-balance-note" name="note" placeholder="例如：线下收款补记、商户调整、退单返还"></textarea>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-member-balance-cancel">取消</button>',
        '<button class="primary-button" id="drawer-member-balance-save">保存调整</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-member-balance-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-member-balance-save').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#member-balance-form'));
      state = stateLib.adjustMemberBalance(state, {
        familyId: family.id,
        memberId: member.id,
        amount: Number(formData.get('amount') || 0),
        type: formData.get('type'),
        note: formData.get('note')
      });
      closeDrawer();
      rerender('成员余额已调整');
    });
  }

  function openMemberLedgerDrawer(familyId, memberId) {
    const family = stateLib.getEntityById(state.families, familyId);
    const member = getMemberById(family, memberId);
    if (!family || !member) return;

    openDrawer({
      title: '余额流水',
      subtitle: family.name + ' · ' + member.name,
      body: [
        '<div class="table-card">',
        '<table class="table">',
        '<thead><tr><th>时间</th><th>类型</th><th>金额</th><th>备注</th><th>余额</th></tr></thead>',
        '<tbody>',
        (member.ledger || []).map(function (item) {
          return '<tr>' +
            '<td>' + escapeHtml(item.createdAt) + '</td>' +
            '<td>' + escapeHtml(item.type) + '</td>' +
            '<td>' + (Number(item.amount || 0) >= 0 ? '+' : '') + escapeHtml(String(item.amount)) + '</td>' +
            '<td>' + escapeHtml(item.note || '-') + '</td>' +
            '<td>' + formatCurrency(item.balanceAfter) + '</td>' +
          '</tr>';
        }).join(''),
        '</tbody>',
        '</table>',
        '</div>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-member-ledger-adjust">去调余额</button>',
        '<button class="primary-button" id="drawer-member-ledger-close">关闭</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-member-ledger-adjust').addEventListener('click', function () {
      openMemberBalanceDrawer(family.id, member.id);
    });
    refs.drawerFoot.querySelector('#drawer-member-ledger-close').addEventListener('click', closeDrawer);
  }

  function openFamilyAddressBookDrawer(familyId) {
    const family = stateLib.getEntityById(state.families, familyId);
    if (!family) return;
    const addresses = getFamilyAddresses(family);

    openDrawer({
      title: '家庭地址簿',
      subtitle: family.name + ' · 维护共享配送地址，普通成员也可维护',
      body: [
        '<div class="detail-stack">',
        addresses.map(function (address) {
          return [
            '<div class="detail-section">',
            '  <div class="list-row">',
            '    <div>',
            '      <div class="item-title">' + escapeHtml(address.tag || '常用地址') + '</div>',
            '      <div class="item-subtitle">' + escapeHtml(address.contactName) + ' · ' + escapeHtml(address.phone) + '</div>',
            '    </div>',
            address.isDefault ? '    <span class="status-badge status-active">默认地址</span>' : '',
            '  </div>',
            '  <div class="detail-value">' + escapeHtml(address.addressText) + '</div>',
            '  <div class="mini-actions">',
            '    <button class="mini-button" data-address-book-edit="' + escapeHtml(address.id) + '">编辑地址</button>',
            (address.isDefault ? '' : '    <button class="mini-button" data-address-book-default="' + escapeHtml(address.id) + '">设为默认</button>'),
            '  </div>',
            '</div>'
          ].join('');
        }).join(''),
        '</div>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-address-add">新增地址</button>',
        '<button class="primary-button" id="drawer-address-close">关闭</button>'
      ].join('')
    });

    refs.drawerBody.querySelectorAll('[data-address-book-edit]').forEach(function (button) {
      button.addEventListener('click', function () {
        openFamilyAddressFormDrawer(family.id, button.dataset.addressBookEdit);
      });
    });
    refs.drawerBody.querySelectorAll('[data-address-book-default]').forEach(function (button) {
      button.addEventListener('click', function () {
        state = stateLib.setDefaultFamilyAddress(state, family.id, button.dataset.addressBookDefault);
        closeDrawer();
        rerender('默认地址已更新');
      });
    });
    refs.drawerFoot.querySelector('#drawer-address-add').addEventListener('click', function () {
      openFamilyAddressFormDrawer(family.id);
    });
    refs.drawerFoot.querySelector('#drawer-address-close').addEventListener('click', closeDrawer);
  }

  function openFamilyAddressFormDrawer(familyId, addressId) {
    const family = stateLib.getEntityById(state.families, familyId);
    const address = getFamilyAddresses(family).find(function (item) {
      return item.id === addressId;
    }) || null;
    if (!family) return;

    openDrawer({
      title: address ? '编辑家庭地址' : '新增家庭地址',
      subtitle: family.name + ' · 家庭共享地址簿',
      body: [
        '<form class="form-grid" id="family-address-form">',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="family-address-contact">联系人</label>',
        '      <input id="family-address-contact" name="contactName" value="' + escapeHtml(address ? address.contactName : '') + '" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="family-address-phone">联系电话</label>',
        '      <input id="family-address-phone" name="phone" value="' + escapeHtml(address ? address.phone : '') + '" />',
        '    </div>',
        '  </div>',
        '  <div class="form-field">',
        '    <label for="family-address-text">地址文本</label>',
        '    <textarea id="family-address-text" name="addressText">' + escapeHtml(address ? address.addressText : '') + '</textarea>',
        '  </div>',
        '  <div class="form-grid two-columns">',
        '    <div class="form-field">',
        '      <label for="family-address-tag">地址标签</label>',
        '      <input id="family-address-tag" name="tag" value="' + escapeHtml(address ? address.tag : '常用') + '" />',
        '    </div>',
        '    <div class="form-field">',
        '      <label for="family-address-default">默认地址</label>',
        '      <select id="family-address-default" name="isDefault">',
        '        <option value="true"' + (address && address.isDefault ? ' selected' : '') + '>设为默认</option>',
        '        <option value="false"' + (!address || !address.isDefault ? ' selected' : '') + '>仅保存</option>',
        '      </select>',
        '    </div>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-address-form-cancel">取消</button>',
        '<button class="primary-button" id="drawer-address-form-save">保存地址</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-address-form-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-address-form-save').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#family-address-form'));
      state = stateLib.saveFamilyAddress(state, family.id, {
        id: address ? address.id : '',
        contactName: formData.get('contactName'),
        phone: formData.get('phone'),
        addressText: formData.get('addressText'),
        tag: formData.get('tag'),
        isDefault: formData.get('isDefault') === 'true'
      });
      closeDrawer();
      rerender('家庭地址已保存');
    });
  }

  function openFamilyMenuPriceDrawer(familyId, dishId) {
    const family = stateLib.getEntityById(state.families, familyId);
    const dish = stateLib.getEntityById(state.dishes, dishId);
    const row = getFamilyMenuRows(familyId).find(function (item) {
      return item.id === dishId;
    });
    if (!family || !dish || !row) return;

    openDrawer({
      title: '调整家庭价',
      subtitle: family.name + ' · ' + dish.name,
      body: [
        '<form class="form-grid" id="menu-price-form">',
        '  <div class="form-field">',
        '    <label for="menu-price-input">家庭价</label>',
        '    <input id="menu-price-input" name="finalPrice" type="number" min="0" step="1" value="' + escapeHtml(row.finalPrice) + '" />',
        '    <div class="field-hint">基础价 ' + formatCurrency(row.basePrice) + '，家庭菜单可单独覆盖。</div>',
        '  </div>',
        '</form>'
      ].join(''),
      footer: [
        '<button class="ghost-button" id="drawer-menu-price-cancel">取消</button>',
        '<button class="primary-button" id="drawer-menu-price-save">保存价格</button>'
      ].join('')
    });

    refs.drawerFoot.querySelector('#drawer-menu-price-cancel').addEventListener('click', closeDrawer);
    refs.drawerFoot.querySelector('#drawer-menu-price-save').addEventListener('click', function () {
      const formData = new FormData(refs.drawerBody.querySelector('#menu-price-form'));
      state = stateLib.updateFamilyMenuPrice(state, familyId, dishId, formData.get('finalPrice'));
      closeDrawer();
      rerender('家庭菜单价格已更新');
    });
  }

  function openFamilyMenuCopyDrawer() {
    const targetFamily = getSelectedMenuFamily();
    if (!targetFamily) return;
    const sourceFamilies = state.families.filter(function (item) {
      return item.id !== targetFamily.id;
    });

    openDrawer({
      title: '复制家庭菜单',
      subtitle: '把其他家庭当前菜单复制到 ' + targetFamily.name,
      body: [
        '<div class="selector-grid" id="copy-family-selector"></div>'
      ].join(''),
      footer: '<button class="ghost-button" id="drawer-copy-menu-cancel">取消</button>'
    });

    const selector = refs.drawerBody.querySelector('#copy-family-selector');
    sourceFamilies.forEach(function (family) {
      const card = document.createElement('button');
      card.className = 'selector-card';
      card.innerHTML = [
        '<div class="selector-title">' + escapeHtml(family.name) + '</div>',
        '<div class="selector-note">' + escapeHtml(family.source) + '</div>'
      ].join('');
      card.addEventListener('click', function () {
        state = stateLib.copyFamilyMenuFromFamily(state, family.id, targetFamily.id);
        closeDrawer();
        rerender('家庭菜单已复制');
      });
      selector.appendChild(card);
    });

    refs.drawerFoot.querySelector('#drawer-copy-menu-cancel').addEventListener('click', closeDrawer);
  }

  function buildOptions(values, selected, labels) {
    return values.map(function (value) {
      const text = labels && labels[value] ? labels[value] : value;
      return '<option value="' + escapeHtml(value) + '"' + (value === selected ? ' selected' : '') + '>' +
        escapeHtml(text) + '</option>';
    }).join('');
  }

  setView('dashboard');
})();
