import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

const [, , manifestArgument, outputArgument] = process.argv;
if (!manifestArgument || !outputArgument) {
  throw new Error('Usage: node generate-recipe-catalog.mjs <manifest.json> <output.md>');
}

const manifestPath = resolve(manifestArgument);
const outputPath = resolve(outputArgument);
const manifest = JSON.parse(await readFile(manifestPath, 'utf8'));
const recipes = Array.isArray(manifest.recipes) ? manifest.recipes : [];
const categories = new Map();

for (const recipe of recipes) {
  const category = recipe.sourceCategory || '未分类';
  const current = categories.get(category) || [];
  current.push(recipe);
  categories.set(category, current);
}

const lines = [
  '# CookLikeHOC 模板菜谱同步总册',
  '',
  '> 本文档由规范化同步清单自动生成，是数据库初始化 SQL 的人工核对依据。',
  '> 未从来源中取得的价格、标准采购数量和图片不会被虚构；制作步骤按来源内容完整保留。',
  '',
  '## 同步摘要',
  '',
  `- 来源版本：\`${manifest.sourceRevision}\``,
  `- 来源菜谱文件：${recipes.reduce((sum, recipe) => sum + recipe.sourceKeys.length, 0)} 份`,
  `- 合并后唯一来源菜名：${recipes.length} 道`,
  '- 数据库模板总数：552 个（含既有家庭菜模板和配料组件）',
  `- 来源分类：${categories.size} 个`,
  `- 食材记录：${recipes.reduce((sum, recipe) => sum + recipe.ingredients.length, 0)} 条`,
  `- 制作步骤：${recipes.reduce((sum, recipe) => sum + recipe.cookingSteps.length, 0)} 条`,
  '',
  '## 分类目录',
  ''
];

for (const [category, categoryRecipes] of categories) {
  lines.push(`- ${category}：${categoryRecipes.length} 道`);
}

for (const [category, categoryRecipes] of categories) {
  lines.push('', `## ${category}`, '');
  for (const recipe of categoryRecipes) {
    const sourceLinks = recipe.sourceKeys.map((sourceKey) => {
      const sourceUrl = `https://github.com/Gar-b-age/CookLikeHOC/blob/${manifest.sourceRevision}/${encodeURI(sourceKey)}`;
      return `[${sourceKey}](${sourceUrl})`;
    }).join('、');
    const readiness = recipe.procurementReady ? '可直接导入' : '待商户完善采购数量/价格后导入';
    lines.push(
      `### ${recipe.name}`,
      '',
      `- 模板：\`${recipe.templateCode}\`（ID ${recipe.templateId}）`,
      `- 来源文件：${sourceLinks}`,
      `- 数据状态：\`${recipe.dataStatus}\`，${readiness}`,
      `- 参考价格：${recipe.referencePrice == null ? '来源未提供' : `￥${recipe.referencePrice}`}`,
      '',
      '**原材料**',
      ''
    );
    if (recipe.ingredients.length === 0) {
      lines.push('- 来源未提供');
    } else {
      for (const ingredient of recipe.ingredients) {
        const quantity = ingredient.quantity == null
          ? (ingredient.sourceQuantityText || '')
          : `${ingredient.quantity}${ingredient.unit || ''}`;
        lines.push(`- ${ingredient.sourceText || ingredient.ingredientName}${quantity ? `（${quantity}）` : ''}`);
      }
    }
    lines.push('', '**制作流程**', '');
    if (recipe.cookingSteps.length === 0) {
      lines.push('来源暂未提供制作流程。');
    } else {
      for (const step of recipe.cookingSteps) {
        lines.push(`${step.stepNo}. ${step.content}`);
      }
    }
    lines.push('');
  }
}

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${lines.join('\n').replace(/\n{3,}/g, '\n\n').trim()}\n`, 'utf8');
console.log(`Generated ${recipes.length} recipes: ${outputPath}`);
