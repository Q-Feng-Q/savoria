const { sessionStore } = require('./session')

function identityKey(session) {
  if (!session) return ''
  return [session.userId, session.activeMode, session.familyId, session.merchantId, session.accessToken]
    .map((value) => value == null ? '' : String(value))
    .join('|')
}

function createIdentityLoadGuard(getSession = () => sessionStore.getSession()) {
  let generation = 0
  return {
    begin(session) {
      generation += 1
      return { generation, identity: identityKey(session) }
    },
    isCurrent(token) {
      return Boolean(token)
        && token.generation === generation
        && token.identity === identityKey(getSession())
    }
  }
}

module.exports = { createIdentityLoadGuard, identityKey }
