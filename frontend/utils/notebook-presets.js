const field = (label, type = 'TEXT', options = [], unit = null) =>
  ({ label, type, required: false, options, unit });

const PRESETS = [
  { id: 'general', name: '通用事件', category: '日常', description: '只用一个字段记录今日内容。', fields: [
    field('今日内容', 'LONG_TEXT')] },
  { id: 'daily', name: '日常随记', category: '日常', description: '留住每天值得记下的小事。', fields: [
    field('心情', 'SINGLE_SELECT', ['开心', '平静', '低落', '焦虑', '其他']), field('标签'), field('图片', 'IMAGE')] },
  { id: 'period', name: '生理期', category: '身体', description: '按天记录生理期和自身感受。', fields: [
    field('经量', 'SINGLE_SELECT', ['少', '适中', '多', '不确定']),
    field('症状', 'MULTI_SELECT', ['腹痛', '腰酸', '头痛', '乏力', '其他']), field('感受', 'LONG_TEXT')] },
  { id: 'body', name: '身体状态', category: '身体', description: '记下身体状态的变化。', fields: [
    field('状态', 'SINGLE_SELECT', ['良好', '一般', '不适']), field('症状', 'LONG_TEXT'), field('程度', 'RATING')] },
  { id: 'habit', name: '习惯打卡', category: '日常', description: '记录一个习惯的坚持过程。', fields: [
    field('完成情况', 'BOOLEAN'), field('次数', 'NUMBER'), field('感受', 'LONG_TEXT')] },
  { id: 'anniversary', name: '纪念日', category: '重要日子', description: '珍藏值得纪念的日子。', fields: [
    field('纪念日期', 'DATE'), field('相关的人'), field('纪念内容', 'LONG_TEXT'), field('图片', 'IMAGE')] },
  { id: 'birthday', name: '生日', category: '重要日子', description: '记录生日与庆祝时刻。', fields: [
    field('生日日期', 'DATE'), field('人物'), field('关系'), field('庆祝记录', 'LONG_TEXT')] },
  { id: 'family', name: '家庭大事', category: '家庭', description: '记录一家人的重要事件。', fields: [
    field('事件类别'), field('相关的人'), field('经过', 'LONG_TEXT'), field('图片', 'IMAGE')] },
  { id: 'travel', name: '旅行足迹', category: '出行', description: '保存旅途中的见闻。', fields: [
    field('地点'), field('同行人'), field('感受', 'LONG_TEXT'), field('图片', 'IMAGE')] },
  { id: 'sleep', name: '睡眠记录', category: '身体', description: '观察自己的睡眠体验。', fields: [
    field('入睡时间', 'TIME'), field('起床时间', 'TIME'), field('睡眠感受', 'RATING')] },
  { id: 'exercise', name: '运动记录', category: '身体', description: '记下每次运动。', fields: [
    field('运动类型'), field('时长', 'NUMBER', [], '分钟'),
    field('强度', 'SINGLE_SELECT', ['轻松', '适中', '高强度']), field('感受', 'LONG_TEXT')] },
  { id: 'meal', name: '饮食记录', category: '日常', description: '记录一日三餐和加餐。', fields: [
    field('餐次', 'SINGLE_SELECT', ['早餐', '午餐', '晚餐', '加餐']),
    field('吃了什么', 'LONG_TEXT'), field('图片', 'IMAGE')] },
  { id: 'mood', name: '情绪日记', category: '日常', description: '为心情留一处安静的地方。', fields: [
    field('心情', 'SINGLE_SELECT', ['开心', '平静', '低落', '焦虑', '其他']),
    field('触发事件', 'LONG_TEXT'), field('想法', 'LONG_TEXT'), field('应对方式', 'LONG_TEXT')] },
  { id: 'weight', name: '体重变化', category: '身体', description: '记录体重的长期变化。', fields: [
    field('体重', 'NUMBER', [], '千克')] },
  { id: 'medical', name: '就医记录', category: '身体', description: '整理就诊过程和后续事项。', fields: [
    field('就诊地点'), field('科室'), field('主要情况', 'LONG_TEXT'), field('后续事项', 'LONG_TEXT')] },
  { id: 'medication', name: '用药记录', category: '身体', description: '记下实际用药情况，不提供用药建议。', fields: [
    field('药品名称'), field('用量'), field('服用时间', 'TIME')] },
  { id: 'baby', name: '宝宝成长', category: '家庭', description: '保存孩子成长中的变化。', fields: [
    field('身高', 'NUMBER', [], '厘米'), field('体重', 'NUMBER', [], '千克'),
    field('新变化', 'LONG_TEXT'), field('图片', 'IMAGE')] },
  { id: 'pet', name: '宠物日常', category: '家庭', description: '记下宠物的日常生活。', fields: [
    field('饮食'), field('状态'), field('特别事件', 'LONG_TEXT'), field('图片', 'IMAGE')] },
  { id: 'study', name: '学习笔记', category: '学习', description: '梳理学习过程和收获。', fields: [
    field('主题'), field('学习时长', 'NUMBER', [], '分钟'),
    field('收获', 'LONG_TEXT'), field('待解决问题', 'LONG_TEXT')] },
  { id: 'reading', name: '阅读记录', category: '学习', description: '保存读过的书和感想。', fields: [
    field('书名'), field('进度'), field('摘记', 'LONG_TEXT'), field('感想', 'LONG_TEXT')] },
  { id: 'social', name: '人情往来', category: '日常', description: '整理往来的事项。', fields: [
    field('对方'), field('事项'), field('金额或礼物')] }
];

function clone(preset) {
  return { ...preset, fields: preset.fields.map((item) =>
    ({ ...item, options: item.options.slice() })) };
}

function listPresets() { return PRESETS.map(clone); }
function getPreset(id) {
  const preset = PRESETS.find((item) => item.id === id);
  return preset ? clone(preset) : null;
}
function copyPresetToEvent(id) {
  const preset = getPreset(id);
  if (!preset) return null;
  return { name: preset.name, category: preset.category,
    description: preset.description, fields: preset.fields };
}

module.exports = { listPresets, getPreset, copyPresetToEvent };
