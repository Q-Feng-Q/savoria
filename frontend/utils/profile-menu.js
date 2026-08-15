function buildProfileMenuGroups(context = {}) {
  const items = [];
  const explicitPermissions = Array.isArray(context.permissionCodes)
    ? context.permissionCodes
    : null;

  if (context.familyId) {
    items.push({ key: 'home', label: '家庭首页', note: '回到家庭餐桌' });
    items.push({ key: 'orders', label: '家庭订单', note: '查看订单与配送状态' });
    items.push({ key: 'addresses', label: '地址簿', note: '维护配送地址' });
    items.push({ key: 'wallet', label: '我的余额', note: '查看余额与流水' });
  }
  items.push({ key: 'notifications', label: '账号通知', note: '申请、邀请和订单动态' });

  const familyRole = String(context.familyRole || '').toUpperCase();
  const familyAdmin = explicitPermissions
    ? explicitPermissions.includes('FAMILY_ADMIN')
    : familyRole === 'OWNER' || familyRole === 'ADMIN';
  const merchantAdmin = explicitPermissions
    ? explicitPermissions.includes('MERCHANT_ADMIN')
    : Boolean(context.merchantRole);

  if (context.familyId && familyAdmin) {
    items.push({ key: 'familyManagement', label: '家庭管理', note: '成员、邀请与加入申请' });
  }
  if (context.merchantId && merchantAdmin) {
    items.push({ key: 'merchant', label: '商户工作台', note: '管理菜品、订单和家庭' });
  }
  return [{ title: context.familyId ? '家庭与账号' : '账号服务', items }];
}

module.exports = { buildProfileMenuGroups };
