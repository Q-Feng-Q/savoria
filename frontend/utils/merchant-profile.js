function clean(value) {
  return String(value == null ? '' : value).trim();
}

function createMerchantProfileSnapshot(profile = {}) {
  return {
    name: clean(profile.name),
    contactName: clean(profile.contactName),
    contactPhone: clean(profile.contactPhone)
  };
}

function validateMerchantProfile(profile = {}) {
  const value = createMerchantProfileSnapshot(profile);
  if (!value.name) return { valid: false, message: '请填写商户名称' };
  if (value.name.length > 100) return { valid: false, message: '商户名称不能超过 100 个字' };
  if (value.contactName.length > 50) return { valid: false, message: '联系人不能超过 50 个字' };
  if (value.contactPhone.length > 30) return { valid: false, message: '联系电话不能超过 30 个字' };
  return { valid: true, value };
}

function isMerchantAccessError(error) {
  return Boolean(error && (
    error.code === 40301 || error.statusCode === 403 || error.status === 403
  ));
}

function failClosedMerchantSession(session = {}) {
  const familyAvailable = Boolean(session.familyId);
  return {
    ...session,
    merchantId: null,
    merchantRole: null,
    activeMode: familyAvailable ? 'family' : null,
    roleTemplate: familyAvailable ? String(session.familyRole || 'user').toLowerCase() : 'user',
    permissionCodes: (session.permissionCodes || []).filter((code) => code !== 'MERCHANT_ADMIN'),
    backendRoles: (session.backendRoles || []).filter((role) => String(role).toLowerCase() !== 'merchant_admin'),
    merchantAdminScopes: [],
    availableModes: (session.availableModes || []).filter((mode) => String(mode).toLowerCase() !== 'merchant')
  };
}

module.exports = {
  createMerchantProfileSnapshot,
  validateMerchantProfile,
  isMerchantAccessError,
  failClosedMerchantSession
};
