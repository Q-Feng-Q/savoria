async function loginWithPermissionFallback(auth, credentials) {
  return auth.portalLogin(credentials);
}

module.exports = { loginWithPermissionFallback };
