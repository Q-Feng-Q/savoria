const test = require('node:test');
const assert = require('node:assert/strict');

const { createSessionStore } = require('../utils/session');

function createMemoryStorage() {
  const data = new Map();
  return {
    get(key) {
      return data.has(key) ? data.get(key) : null;
    },
    set(key, value) {
      data.set(key, value);
    },
    remove(key) {
      data.delete(key);
    }
  };
}

test('session store persists and returns access token and actor data', () => {
  const storage = createMemoryStorage();
  const session = createSessionStore({ storage });

  session.setSession({
    accessToken: 'token-1',
    loginMode: 'dev',
    actor: {
      memberId: 'member-chen-mei'
    }
  });

  assert.equal(session.getToken(), 'token-1');
  assert.equal(session.getSession().actor.memberId, 'member-chen-mei');
});

test('session store clears persisted data', () => {
  const storage = createMemoryStorage();
  const session = createSessionStore({ storage });

  session.setSession({
    accessToken: 'token-1',
    loginMode: 'dev',
    actor: {
      memberId: 'member-chen-mei'
    }
  });

  session.clearSession();

  assert.equal(session.getToken(), '');
  assert.equal(session.getSession(), null);
});

test('explicit backend modes override legacy id inference', () => {
  const session = createSessionStore({ storage: createMemoryStorage() });
  session.setSession({ userId: 2, accessToken: 'token', familyId: 1, merchantId: 2,
    availableModes: ['merchant'] });
  assert.deepEqual(session.getSession().availableModes, ['merchant']);
  assert.equal(session.getSession().activeMode, 'merchant');
});

test('missing explicit modes retain legacy compatibility inference', () => {
  const session = createSessionStore({ storage: createMemoryStorage() });
  session.setSession({ userId: 2, accessToken: 'token', familyId: 1, merchantId: 2 });
  assert.deepEqual(session.getSession().availableModes, ['family', 'merchant']);
});
