function createProfileSnapshot(info = {}) {
  return {
    familyName: info.familyName == null ? '' : String(info.familyName),
    note: info.note == null ? '' : String(info.note)
  };
}

function validateFamilyProfile(draft = {}) {
  const familyName = String(draft.familyName || '').trim();
  const note = String(draft.note || '').trim();
  if (!familyName) return { valid: false, payload: null, message: '请输入家庭名称' };
  if (familyName.length > 100) return { valid: false, payload: null, message: '家庭名称不能超过 100 个字' };
  if (note.length > 255) return { valid: false, payload: null, message: '家庭备注不能超过 255 个字' };
  return {
    valid: true,
    payload: { familyName, note: note || null },
    message: ''
  };
}

function buildOwnerOptions(candidates = []) {
  const groups = new Map();
  candidates.forEach((item) => {
    const name = String(item.displayName || '家庭成员').trim() || '家庭成员';
    if (!groups.has(name)) groups.set(name, []);
    groups.get(name).push(item);
  });
  return candidates.map((item) => {
    const name = String(item.displayName || '家庭成员').trim() || '家庭成员';
    const group = groups.get(name);
    let label = name;
    if (group.length > 1) {
      const suffix = item.phoneSuffix == null ? '' : String(item.phoneSuffix);
      const suffixUnique = suffix && group.filter((entry) => String(entry.phoneSuffix || '') === suffix).length === 1;
      label = suffixUnique
        ? `${name}（尾号 ${suffix}）`
        : `${name}（成员 ${group.indexOf(item) + 1}）`;
    }
    return { memberId: item.memberId, label };
  });
}

function findDefaultAddress(addresses = []) {
  return addresses.find((item) => Boolean(item && item.defaultAddress)) || null;
}

function currentOwnerLabel(session = {}) {
  const name = String(session.nickname || session.username || '我').trim() || '我';
  return `当前负责人：我 · ${name}`;
}

function failClosedOwnerSession(session = {}) {
  return {
    ...session,
    familyRole: 'MEMBER',
    roleTemplate: 'member',
    permissionCodes: (session.permissionCodes || []).filter((code) => code !== 'FAMILY_ADMIN')
  };
}

function mergeIdentityContext(session = {}, context = {}) {
  const familyId = context.familyId == null ? null : context.familyId;
  const familyRole = familyId ? String(context.familyRole || 'MEMBER').toUpperCase() : null;
  return {
    ...session,
    ...context,
    familyId,
    memberId: familyId ? context.userId : null,
    familyRole,
    roleTemplate: familyId ? familyRole.toLowerCase() : 'user',
    availableModes: Array.isArray(context.availableModes) ? context.availableModes : [],
    permissionCodes: Array.isArray(context.permissionCodes) ? context.permissionCodes : []
  };
}

module.exports = {
  buildOwnerOptions,
  createProfileSnapshot,
  validateFamilyProfile,
  findDefaultAddress,
  currentOwnerLabel,
  failClosedOwnerSession,
  mergeIdentityContext
};
