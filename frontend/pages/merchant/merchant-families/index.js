const { createApiRuntime } = require('../../../utils/api-runtime');
const { buildApiMerchantFamiliesScene } = require('../../../utils/merchant-scenes');
const { requireSession, resolveApiErrorMessage } = require('../../../utils/page-api');

Page({
  data: {
    phase: 'loading',
    pageTitle: '正在读取家庭',
    pageDescription: '请稍候',
    families: [],
    familyCount: 0,
    context: null
  },

  onShow() { this.load(); },

  async load() {
    const session = requireSession({ merchantOnly: true });
    if (!session) return;
    const generation = (this._loadGeneration || 0) + 1;
    this._loadGeneration = generation;
    this.setData({ phase: 'loading', pageTitle: '正在读取家庭', pageDescription: '请稍候' });
    try {
      const rawFamilies = await createApiRuntime().merchant.getFamilies();
      if (generation !== this._loadGeneration) return;
      const scene = buildApiMerchantFamiliesScene({ session, families: rawFamilies });
      const families = (scene.families || []).map((item, index) => {
        const raw = rawFamilies[index] || {};
        const activeMenuCount = Number(raw.activeMenuCount || 0);
        const addressCount = Number(raw.addressCount || (raw.addresses || []).length || 0);
        return {
          ...item,
          addressCountText: `${addressCount} 个地址`,
          serviceStateText: raw.serviceStateText || (raw.deliveryEnabled ? '服务中' : '配送未开启'),
          activeMenuCount,
          menuStatus: activeMenuCount > 0 ? 'configured' : 'attention',
          menuStatusText: activeMenuCount > 0 ? `菜单已配置 · ${activeMenuCount} 道生效` : '菜单待配置'
        };
      });
      this.setData({
        ...scene,
        families,
        familyCount: families.length,
        phase: families.length ? 'ready' : 'empty',
        pageTitle: families.length ? '' : '还没有服务家庭',
        pageDescription: families.length ? '' : '家庭完成签约后会显示在这里。'
      });
    } catch (error) {
      if (generation !== this._loadGeneration) return;
      this.setData({ phase: 'error', pageTitle: '家庭加载失败', pageDescription: resolveApiErrorMessage(error, '家庭列表加载失败') });
    }
  },

  retryLoad() { return this.load(); },

  openFamilyDetail(event) {
    wx.navigateTo({ url: `/pages/merchant/merchant-family-detail/index?familyId=${event.currentTarget.dataset.id}` });
  },

  openFamilyMenu(event) {
    wx.navigateTo({ url: `/pages/merchant/family-menu/index?familyId=${event.currentTarget.dataset.id}` });
  }
});
