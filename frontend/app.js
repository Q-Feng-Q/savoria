const { sessionStore } = require('./utils/session');

App({
  globalData: {
    apiBaseUrl: 'http://127.0.0.1:8080',
    sessionStore
  }
});
