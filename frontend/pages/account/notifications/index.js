const { createApiRuntime, resolveNotificationScope } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');
const { loadAllPages } = require('../../../utils/pagination');
const { createIdentityLoadGuard } = require('../../../utils/identity-load');

const CATEGORY_LABELS = {
  order: '订单',
  purchase: '采购',
  wallet: '余额',
  family: '家庭动态'
};

function buildNotificationContext(session) {
  return {
    merchant: {
      id: session.merchantId,
      name: session.roleTemplate === 'merchant_admin' ? '商户工作台' : '食光知味'
    },
    family: session.familyId ? { id: session.familyId, name: '当前家庭' } : null,
    member: session.memberId ? { id: session.memberId, name: '当前成员' } : null
  };
}

function mapNotificationsPage(session, actorType, pageData) {
  const items = (pageData.items || []).map((item) => ({
    id: item.notificationId,
    title: item.title,
    content: item.content,
    read: Boolean(item.read),
    createdAt: String(item.createdAt || '').replace('T', ' ').replace(/:\d{2}$/, ''),
    categoryLabel: CATEGORY_LABELS[item.category] || item.category || '消息'
  }));

  return {
    actorType,
    context: buildNotificationContext(session),
    unreadCount: items.filter((item) => !item.read).length,
    unreadItems: items.filter((item) => !item.read),
    readItems: items.filter((item) => item.read)
  };
}

Page({
  identityLoad: createIdentityLoadGuard(),
  data: {
    actorType: 'account',
    unreadCount: 0,
    unreadItems: [],
    readItems: [],
    context: null,
    phase: 'loading',
    errorMessage: '',
    busyReadMap: {},
    markingAllRead: false
  },

  onLoad(query) {
    this.setData({
      actorType: query && query.actor === 'merchant' ? 'merchant' : 'account'
    });
  },

  onShow() {
    this.load();
  },

  retryLoad() { return this.load(); },

  async load({ silent = false } = {}) {
    const session = requireSession();
    if (!session) return;
    const loadToken = this.identityLoad.begin(session);
    if (!silent) this.setData({ phase: 'loading', errorMessage: '', unreadCount: 0, unreadItems: [], readItems: [], context: null });

    const runtime = createApiRuntime();
    const receiverScope = this.data.actorType === 'merchant'
      ? 'merchant'
      : resolveNotificationScope(session);

    try {
      const items = await loadAllPages(({ page, pageSize }) => runtime.notifications.getNotifications({
        receiverScope,
        readStatus: 'all',
        page,
        pageSize
      }), { pageSize: 100, keyOf: (item) => item.notificationId });
      if (!this.identityLoad.isCurrent(loadToken)) return;
      this.setData({ ...mapNotificationsPage(session, this.data.actorType, { items }), phase: 'ready', errorMessage: '' });
    } catch (error) {
      if (!this.identityLoad.isCurrent(loadToken)) return;
      if (!silent) this.setData({ phase: 'error', errorMessage: (error && error.message) || '通知加载失败' });
      showApiError(error, '通知加载失败');
    }
  },

  async markRead(event) {
    const id = event.currentTarget.dataset.id;
    if (!id || this.data.busyReadMap[id]) return;
    this.setData({ [`busyReadMap.${id}`]: true });
    try {
      await createApiRuntime().notifications.markRead(id);
      await this.load({ silent: true });
    } catch (error) {
      showApiError(error, '标记已读失败');
    } finally { this.setData({ [`busyReadMap.${id}`]: false }); }
  },

  async markAllRead() {
    if (this.data.markingAllRead || !this.data.unreadCount) return;
    this.setData({ markingAllRead: true });
    try {
      const notifications=createApiRuntime().notifications;
      if(this.data.actorType==='merchant')await notifications.markAllRead({receiverScope:'merchant'});
      else await notifications.markAllRead({receiverScope:'account'});
      await this.load({ silent: true });
    } catch (error) {
      showApiError(error, '操作失败');
    } finally { this.setData({ markingAllRead: false }); }
  }
});

