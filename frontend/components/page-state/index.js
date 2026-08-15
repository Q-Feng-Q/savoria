Component({
  properties: {
    phase: {
      type: String,
      value: 'idle'
    },
    title: {
      type: String,
      value: ''
    },
    description: {
      type: String,
      value: ''
    },
    illustration: {
      type: String,
      value: 'orders'
    },
    retryText: {
      type: String,
      value: '重新加载'
    }
  },
  methods: {
    retry() {
      this.triggerEvent('retry');
    }
  }
});
