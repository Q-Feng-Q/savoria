<template>
  <div class="snapshot-comparison">
    <div class="snapshot-heading"><strong>字段对比</strong><span>高亮项为本次修改</span></div>
    <div class="snapshot-grid snapshot-grid--head"><span>字段</span><span>修改前</span><span>申请修改为</span></div>
    <div class="image-comparison"><strong>图片对比</strong><div><figure><img v-if="baseSnapshot.imageUrl" :src="assetUrl(baseSnapshot.imageUrl)" alt="修改前菜品图片" /><span v-else>暂无图片</span><figcaption>修改前</figcaption></figure><figure><img v-if="targetSnapshot.imageUrl" :src="assetUrl(targetSnapshot.imageUrl)" alt="申请修改后的菜品图片" /><span v-else>暂无图片</span><figcaption>申请修改为</figcaption></figure></div></div>
    <div v-for="row in fieldRows" :key="row.key" class="snapshot-grid" :class="{ 'is-changed': row.changed }"><strong>{{ row.label }}</strong><span>{{ row.before }}</span><span>{{ row.after }}</span></div>
    <div class="ingredient-comparison">
      <section><strong>修改前食材 · {{ baseIngredients.length }} 项</strong><div v-for="(item, index) in baseIngredients" :key="item.itemId || `${item.ingredientName}-${index}`" class="ingredient-line"><span>{{ item.ingredientName }}</span><small>{{ ingredientText(item) }}</small></div><small v-if="!baseIngredients.length" class="empty-copy">无食材</small></section>
      <section><strong>申请食材 · {{ targetIngredients.length }} 项</strong><div v-for="(item, index) in targetIngredients" :key="item.itemId || `${item.ingredientName}-${index}`" class="ingredient-line ingredient-line--target"><span>{{ item.ingredientName }}</span><small>{{ ingredientText(item) }}</small></div><small v-if="!targetIngredients.length" class="empty-copy">无食材</small></section>
    </div>
    <div class="snapshot-heading step-heading"><strong>制作步骤对比</strong><span>按执行顺序完整展示</span></div>
    <div class="ingredient-comparison step-comparison">
      <section><strong>修改前步骤 · {{ baseSteps.length }} 项</strong><div v-for="item in baseSteps" :key="item.itemId || item.stepNo" class="step-line"><b>{{ item.stepNo }}. {{ item.title || '步骤' }}</b><span>{{ item.content }}</span><StepImages :model-value="item.imageUrls || []" /><small>{{ stepFacts(item) }}</small></div><small v-if="!baseSteps.length" class="empty-copy">暂无步骤</small></section>
      <section><strong>申请步骤 · {{ targetSteps.length }} 项</strong><div v-for="item in targetSteps" :key="item.itemId || item.stepNo" class="step-line step-line--target"><b>{{ item.stepNo }}. {{ item.title || '步骤' }}</b><span>{{ item.content }}</span><StepImages :model-value="item.imageUrls || []" /><small>{{ stepFacts(item) }}</small></div><small v-if="!targetSteps.length" class="empty-copy">暂无步骤</small></section>
    </div>
  </div>
</template>

<script setup>
import StepImages from './StepImages.vue'; import { computed } from 'vue';
import { getApiBaseUrl } from '../api/http';
const props = defineProps({ baseSnapshot: { type: Object, default: () => ({}) }, targetSnapshot: { type: Object, default: () => ({}) } });
const fields = [['name','菜品名称'],['categoryId','分类 ID'],['productType','菜品类型'],['nourishmentDescription','滋补介绍'],['servingAdvice','食用建议'],['precautions','注意事项'],['description','菜品简介'],['referencePrice','参考价格'],['tasteTags','口味标签'],['mealTags','推荐餐次'],['sortOrder','排序值'],['enabled','启用状态']];
function assetUrl(value) { const path=String(value||''); if(!path||/^https?:\/\//i.test(path))return path; const base=getApiBaseUrl(); return base==='/api'?`/api${path.startsWith('/')?path:`/${path}`}`:`${String(base||'').replace(/\/api\/?$/,'').replace(/\/$/,'')}${path.startsWith('/')?path:`/${path}`}`; }
function display(value) { if (Array.isArray(value)) return value.join('、') || '无'; if (typeof value === 'boolean') return value ? '启用' : '停用'; return value === undefined || value === null || value === '' ? '未填写' : String(value); }
const foodFields = new Set(['productType', 'nourishmentDescription', 'servingAdvice', 'precautions']);
const fieldDisplay = (snapshot, key) => key === 'productType' && snapshot[key] ? ({ NOURISHMENT: '滋补食品', NORMAL: '普通菜品' }[snapshot[key]] || display(snapshot[key])) : display(snapshot[key]);
const fieldRows = computed(() => fields.map(([key, label]) => {
  const historicalMissing = foodFields.has(key) && props.baseSnapshot[key] == null;
  const preserved = foodFields.has(key) && props.targetSnapshot[key] == null;
  return { key, label,
    before: historicalMissing ? '历史快照未记录' : fieldDisplay(props.baseSnapshot, key),
    after: preserved ? '未修改，保留原值' : fieldDisplay(props.targetSnapshot, key),
    changed: !preserved && JSON.stringify(props.baseSnapshot[key]) !== JSON.stringify(props.targetSnapshot[key])
  };
}));
const baseIngredients = computed(() => props.baseSnapshot.ingredients || []); const targetIngredients = computed(() => props.targetSnapshot.ingredients || []);
const baseSteps = computed(() => props.baseSnapshot.cookingSteps || []); const targetSteps = computed(() => props.targetSnapshot.cookingSteps || []);
function ingredientText(item) { if (item.quantityStatus === 'NOT_APPLICABLE') return `${item.ingredientCategory} · 无需采购`; if (item.quantityStatus === 'SOURCE_BATCH') return `${item.ingredientCategory} · ${item.sourceQuantityText || '原配方批量'}`; if (item.quantityStatus === 'MISSING') return `${item.ingredientCategory} · 用量待补`; return `${item.quantity} ${item.unit} · ${item.ingredientCategory} · ${item.calcType}`; }
function stepFacts(item) { return [item.heatLevel, item.temperatureText, item.durationSeconds ? `${item.durationSeconds} 秒` : ''].filter(Boolean).join(' · '); }
</script>

<style scoped>
.snapshot-grid > span { white-space: pre-wrap; overflow-wrap: anywhere; }
.snapshot-comparison{overflow:hidden;border:1px solid var(--line);border-radius:12px;background:#fff}.snapshot-heading{display:flex;align-items:center;justify-content:space-between;padding:14px 16px}.snapshot-heading span{color:var(--text-soft);font-size:12px}.snapshot-grid{display:grid;grid-template-columns:150px 1fr 1fr;border-top:1px solid var(--line)}.snapshot-grid>*{padding:11px 13px;border-right:1px solid var(--line);font-size:12px;word-break:break-word}.snapshot-grid--head{background:#f6f2e9;color:var(--text-soft);font-weight:700}.snapshot-grid.is-changed{background:#fff7df}.snapshot-grid.is-changed span:last-child{color:var(--brand-deep);font-weight:700}.ingredient-comparison{display:grid;grid-template-columns:1fr 1fr;gap:14px;padding:16px;border-top:1px solid var(--line);background:#fbfaf6}.ingredient-comparison section{padding:14px;border:1px solid var(--line);border-radius:10px;background:#fff}.ingredient-line{display:flex;justify-content:space-between;gap:10px;padding:9px 0;border-bottom:1px dashed var(--line);font-size:12px}.ingredient-line small{color:var(--text-soft);text-align:right}.ingredient-line--target span{color:var(--brand-deep);font-weight:700}.step-heading{border-top:1px solid var(--line)}.step-line{display:grid;gap:5px;padding:10px 0;border-bottom:1px dashed var(--line);font-size:12px}.step-line span{line-height:1.55}.step-line small,.empty-copy{color:var(--text-soft)}.step-line--target b{color:var(--brand-deep)}@media(max-width:720px){.snapshot-grid{grid-template-columns:105px 1fr 1fr}.ingredient-comparison{grid-template-columns:1fr}}
.image-comparison{padding:14px 16px;border-top:1px solid var(--line)}.image-comparison>div{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:10px}.image-comparison figure{margin:0}.image-comparison img,.image-comparison figure>span{display:grid;place-items:center;width:100%;height:180px;object-fit:cover;border-radius:8px;background:#f3f1eb;color:var(--text-soft)}.image-comparison figcaption{margin-top:6px;color:var(--text-soft);font-size:12px;text-align:center}
</style>
