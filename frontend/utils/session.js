const SESSION_KEY = 'family_kitchen_session_v1';
const ACCOUNTS_KEY = 'family_kitchen_accounts_v1';

function defaultStorage() {
  return {
    get(key) {
      return wx.getStorageSync(key) || null;
    },
    set(key, value) {
      wx.setStorageSync(key, value);
    },
    remove(key) {
      wx.removeStorageSync(key);
    }
  };
}

function createSessionStore(options = {}) {
  const storage = options.storage || defaultStorage();
  const now = options.now || Date.now;

  function accountKey(session) {
    return session && session.userId != null
      ? `user:${session.userId}`
      : `name:${String(session && session.username || '').toLowerCase()}`;
  }

  function availableModes(session) {
    if (session && Array.isArray(session.availableModes)) {
      return Array.from(new Set(session.availableModes
        .map((mode) => String(mode).toLowerCase())
        .filter((mode) => mode === 'family' || mode === 'merchant')));
    }
    const modes = [];
    const hasMerchant = Boolean(session && session.merchantId);
    if ((session && session.familyId) || !hasMerchant) modes.push('family');
    if (hasMerchant) modes.push('merchant');
    return modes;
  }

  function normalizeSession(session, previous = null, touch = false) {
    if (!session) return null;
    const modes = availableModes(session);
    const preferredMode = session.activeMode || (previous && previous.activeMode);
    const activeMode = modes.includes(preferredMode)
      ? preferredMode
      : (modes.includes('family') ? 'family' : modes[0]);
    return {
      ...session,
      availableModes: modes,
      activeMode,
      requiresLogin: session.requiresLogin == null
        ? Boolean(previous && previous.requiresLogin && !session.accessToken)
        : Boolean(session.requiresLogin),
      lastUsedAt: touch ? now() : (session.lastUsedAt || (previous && previous.lastUsedAt) || 0)
    };
  }

  function rawAccounts() {
    const value = storage.get(ACCOUNTS_KEY);
    return Array.isArray(value) ? value : [];
  }

  function saveAccounts(accounts) {
    storage.set(ACCOUNTS_KEY, accounts);
    return accounts;
  }

  function getSession() {
    return storage.get(SESSION_KEY);
  }

  function setSession(session, options = {}) {
    const accounts = rawAccounts();
    const key = accountKey(session);
    const index = accounts.findIndex((item) => accountKey(item) === key);
    const previous = index >= 0 ? accounts[index] : null;
    const normalized = normalizeSession(session, previous, options.touch !== false);
    storage.set(SESSION_KEY, normalized);
    if(options.save!==false&&normalized&&normalized.accessToken){
      if(index>=0)accounts[index]=normalized;else accounts.push(normalized);
      saveAccounts(accounts);
    }
    return normalized;
  }

  function clearSession() {
    storage.remove(SESSION_KEY);
  }

  function getToken() {
    const session = getSession();
    return session && session.accessToken ? session.accessToken : '';
  }
  function listAccounts(){return rawAccounts().map((item)=>normalizeSession(item)).sort((a,b)=>(b.lastUsedAt||0)-(a.lastUsedAt||0));}
  function getAccount(userId){return listAccounts().find(item=>String(item.userId)===String(userId))||null;}
  function activateAccount(userId,mode){
    const accounts=rawAccounts();
    const index=accounts.findIndex(item=>String(item.userId)===String(userId));
    if(index<0)return null;
    const target=normalizeSession(accounts[index]);
    const selectedMode=mode||target.activeMode;
    if(target.requiresLogin||!target.accessToken||!target.availableModes.includes(selectedMode))return null;
    const active=normalizeSession({...target,activeMode:selectedMode,requiresLogin:false},target,true);
    accounts[index]=active;
    saveAccounts(accounts);
    storage.set(SESSION_KEY,active);
    return active;
  }
  function switchAccount(userId){return activateAccount(userId);}
  function markRequiresLogin(userId,required=true){
    const accounts=rawAccounts();
    const index=accounts.findIndex(item=>String(item.userId)===String(userId));
    if(index<0)return null;
    const updated=normalizeSession({...accounts[index],requiresLogin:Boolean(required),...(required?{accessToken:''}:{})},accounts[index]);
    accounts[index]=updated;
    saveAccounts(accounts);
    const current=getSession();
    if(current&&String(current.userId)===String(userId))storage.set(SESSION_KEY,updated);
    return updated;
  }
  function removeAccount(userId){const current=getSession();const remaining=listAccounts().filter(item=>String(item.userId)!==String(userId));saveAccounts(remaining);if(current&&String(current.userId)===String(userId)){if(remaining.length)storage.set(SESSION_KEY,remaining[0]);else storage.remove(SESSION_KEY);}return remaining;}
  function clearAllAccounts(){storage.remove(ACCOUNTS_KEY);storage.remove(SESSION_KEY);}

  return {
    getSession,
    setSession,
    clearSession,
    getToken,listAccounts,getAccount,activateAccount,switchAccount,markRequiresLogin,removeAccount,clearAllAccounts
  };
}

const sessionStore = createSessionStore();

module.exports = {
  SESSION_KEY,
  ACCOUNTS_KEY,
  createSessionStore,
  sessionStore
};
