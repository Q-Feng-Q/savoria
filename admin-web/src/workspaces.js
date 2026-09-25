const tab = (label, path) => ({ label, path });
export const workspaces = [
  { key: 'home', label: '今日工作台', icon: 'home', scope: 'merchant', tabs: [tab('经营概览', '/dashboard')] },
  { key: 'orders', label: '订单与采购', icon: 'orders', scope: 'merchant', tabs: [tab('订单管理', '/orders'), tab('采购清单', '/purchases')] },
  { key: 'dishes', label: '菜品工作室', icon: 'pot', scope: 'merchant', tabs: [tab('我的菜品', '/dishes'), tab('模板菜市场', '/dish-templates'), tab('食材字典', '/ingredients'), tab('菜品审核记录', '/merchant-dish-reviews'), tab('模板修改申请', '/dish-template-changes')] },
  { key: 'families', label: '家庭与菜单', icon: 'home', scope: 'merchant', tabs: [tab('家庭资料', '/families'), tab('家庭菜单', '/menus')] },
  { key: 'people', label: '商户与家庭', icon: 'store', scope: 'platform', tabs: [tab('家庭管理', '/platform-families'), tab('商户管理', '/platform-merchants'), tab('用户管理', '/users')] },
  { key: 'reviews', label: '审核中心', icon: 'check', scope: 'platform', tabs: [tab('菜品审核', '/dish-reviews'), tab('模板修改', '/dish-template-change-reviews'), tab('家庭申请', '/family-applications')] },
  { key: 'templates', label: '平台菜谱库', icon: 'menu', scope: 'platform', tabs: [tab('菜谱模板', '/platform-dish-templates')] },
  { key: 'settings', label: '平台设置', icon: 'profile', scope: 'platform', tabs: [tab('系统配置', '/system-settings')] },
  { key: 'feedback', label: '用户反馈', icon: 'calendar', scope: 'platform', tabs: [tab('BUG / 建议', '/platform-feedback')] },
  { key: 'messages', label: '消息中心', icon: 'calendar', scope: 'all', tabs: [tab('通知', '/notifications')] }
];
export function visibleWorkspaces(platform, merchant) {
  return workspaces.filter(item => item.scope === 'all' || (item.scope === 'platform' ? platform : merchant));
}
export function workspaceForPath(path, items = workspaces) {
  return items.find(item => item.tabs.some(tab => path === tab.path || path.startsWith(tab.path + '/')));
}
