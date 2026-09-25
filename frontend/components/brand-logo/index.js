const { brandStore, resolveLogo } = require('../../utils/branding');

Component({
  properties: {
    size: { type: String, value: 'standard' },
    showName: { type: Boolean, value: true },
    stacked: { type: Boolean, value: false },
    navigationTitle: { type: String, value: '' }
  },
  data: { logoSrc: '', logoFallback: '', logoSize: 112, brandName: '食光栀味' },
  observers: {
    size() { if (this._brandAttached) this.applyBrand(brandStore.get()); }
  },
  lifetimes: {
    attached() {
      this._brandAttached = true;
      this._unsubscribeBrand = brandStore.subscribe(value => this.applyBrand(value));
      brandStore.refresh();
    },
    detached() {
      this._brandAttached = false;
      if (this._unsubscribeBrand) this._unsubscribeBrand();
    }
  },
  pageLifetimes: {
    show() { this.applyBrand(brandStore.get()); brandStore.refresh(); }
  },
  methods: {
    applyBrand(brand) {
      const logo = resolveLogo(brand, this.properties.size, brandStore.baseUrl());
      // Do not retry a known broken URL every time another component refreshes settings.
      const key = `${logo.src}:${logo.fallback}`;
      const update = { logoFallback: logo.fallback, logoSize: logo.sizeRpx, brandName: logo.name };
      if (key !== this._sourceKey) { update.logoSrc = logo.src; this._sourceKey = key; }
      this.setData(update);
      if (this.properties.navigationTitle && typeof wx !== 'undefined' && wx.setNavigationBarTitle) {
        const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : [];
        const owner = typeof this.getPageId === 'function' ? this.getPageId() : null;
        const current = pages[pages.length - 1];
        if (!current || owner === null || current.getPageId && current.getPageId() === owner) {
          wx.setNavigationBarTitle({ title: this.properties.navigationTitle.replace('{name}', logo.name) });
        }
      }
    },
    onImageError() {
      this.setData({ logoSrc: this.data.logoSrc && this.data.logoSrc !== this.data.logoFallback ? this.data.logoFallback : '' });
    }
  }
});
