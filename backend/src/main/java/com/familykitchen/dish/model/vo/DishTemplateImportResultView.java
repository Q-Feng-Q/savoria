package com.familykitchen.dish.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 批量导入结果，分别列出实际导入和幂等跳过的模板。
 * @param importedIds 实际导入的模板 ID
 * @param skippedIds 已存在而跳过的模板 ID
 * @param importedCount 实际导入数量
 * @param skippedCount 跳过数量
 */
@Schema(description = "批量导入平台菜品模板结果")
  public record DishTemplateImportResultView(List<Long> importedIds, List<Long> skippedIds,
    int importedCount, int skippedCount) {
  /**
   * 根据导入和跳过列表创建带统计数量的响应。
   * @param importedIds 实际导入的模板 ID
   * @param skippedIds 已存在而跳过的模板 ID
   */
  public DishTemplateImportResultView(List<Long> importedIds, List<Long> skippedIds) {
    this(importedIds, skippedIds, importedIds.size(), skippedIds.size());
  }
}
