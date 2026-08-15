const { createApiRuntime, resolveNotificationScope } = require('../../../utils/api-runtime');
const { requireSession, showApiError } = require('../../../utils/page-api');

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
  data: {
    actorType: 'account',
    unreadCount: 0,
    unreadItems: [],
    readItems: [],
    context: null
  },

  onLoad(query) {
    this.setData({
      actorType: query && query.actor === 'merchant' ? 'merchant' : 'account'
    });
  },

  onShow() {
    this.load();
  },

  async load() {
    const session = requireSession();
    if (!session) return;

    const runtime = createApiRuntime();
    const receiverScope = this.data.actorType === 'merchant'
      ? 'merchant'
      : resolveNotificationScope(session);

    try {
      const pageData = await runtime.notifications.getNotifications({
        receiverScope,
        readStatus: 'all',
        page: 1,
        pageSize: 100
      });
      this.setData(mapNotificationsPage(session, this.data.actorType, pageData));
    } catch (error) {
      showApiError(error, '通知加载失败');
    }
  },

  async markRead(event) {
    try {
      await createApiRuntime().notifications.markRead(event.currentTarget.dataset.id);
      await this.load();
    } catch (error) {
      showApiError(error, '标记已读失败');
    }
  },

  async markAllRead() {
    try {
      const notifications=createApiRuntime().notifications;
      if(this.data.actorType==='merchant')await notifications.markAllRead({receiverScope:'merchant'});
      else await notifications.markAllRead({receiverScope:'account'});
      await this.load();
    } catch (error) {
      showApiError(error, '操作失败');
    }
  }
});

