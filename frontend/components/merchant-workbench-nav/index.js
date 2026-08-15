const ROUTES = {
  overview: '/pages/merchant/index',
  orders: '/pages/merchant/merchant-orders/index',
  dishes: '/pages/merchant/merchant-dishes/index',
  families: '/pages/merchant/merchant-families/index'
};

Component({
  properties: {
    active: {
      type: String,
      value: 'overview'
    }
  },
  methods: {
    handleNavigate(event) {
      const key = event.currentTarget.dataset.key;
      if (!ROUTES[key] || key === this.data.active || this._navigationInFlight) return;
      this._navigationInFlight = true;
      const releaseNavigation = () => {
        this._navigationInFlight = false;
      };
      wx.redirectTo({
        url: ROUTES[key],
        complete: releaseNavigation,
        fail: releaseNavigation
      });
    }
  }
});
