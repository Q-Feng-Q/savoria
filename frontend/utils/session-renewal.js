const flights = new WeakMap();

function renewSession(sessionStore, refresh) {
  const before = sessionStore.getSession();
  if (!before || !before.refreshToken || !before.accessToken) return Promise.resolve(false);
  const active = flights.get(sessionStore);
  if (active && active.userId === String(before.userId)
      && active.refreshToken === before.refreshToken) return active.promise;
  const promise = (async () => {
    const renewed = await refresh(before.refreshToken);
    const current = sessionStore.getSession();
    if (!current || String(current.userId) !== String(before.userId)
        || current.accessToken !== before.accessToken
        || current.refreshToken !== before.refreshToken
        || !renewed || String(renewed.userId) !== String(before.userId)
        || !renewed.accessToken || !renewed.refreshToken) return false;
    sessionStore.setSession({ ...current, ...renewed, loginMode: 'api', requiresLogin: false });
    return true;
  })().finally(() => {
    if (flights.get(sessionStore) && flights.get(sessionStore).promise === promise)
      flights.delete(sessionStore);
  });
  flights.set(sessionStore, { userId: String(before.userId), refreshToken: before.refreshToken, promise });
  return promise;
}

module.exports = { renewSession };
