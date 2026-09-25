const { brandStore } = require('../../utils/branding');

Component({
  data: { brandName: brandStore.get().siteName },
  lifetimes: {
    attached() {
      this._unsubscribeBrand = brandStore.subscribe(brand => this.setData({ brandName: brand.siteName }));
      brandStore.refresh();
    },
    detached() { if (this._unsubscribeBrand) this._unsubscribeBrand(); }
  },
  pageLifetimes: { show() { brandStore.refresh(); } },
  properties: {
    title: { type: String, value: '' },
    subtitle: { type: String, value: '' },
    character: { type: String, value: 'welcome' },
    compact: { type: Boolean, value: false },
    image: { type: String, value: '' },
    showCharacter: { type: Boolean, value: true }
  }
});
