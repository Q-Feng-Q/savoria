package com.familykitchen.dish.service;

import com.familykitchen.dish.model.entity.DishTemplateEntity;
import com.familykitchen.dish.model.entity.DishTemplateIngredientEntity;
import java.util.List;

/**
 * 统一判定平台模板是否具备可靠的家庭采购数据。
 *
 * <p>调用方必须一次传入根模板可达的组件模板和全部食材行。实现不会信任客户端或数据库中
 * 已保存的派生状态，而是按数量状态、组件图、倍数和单位兼容性重新计算。</p>
 */
public interface DishTemplateProcurementReadinessEvaluator {

  /**
   * 计算指定成品模板的采购就绪状态。
   *
   * @param rootTemplateId 根成品模板ID
   * @param templates 根模板及其可达组件模板
   * @param ingredients 上述模板的全部食材和组件引用行
   * @return 就绪状态和稳定排序的阻断原因
   */
  EvaluationResult evaluate(Long rootTemplateId, List<DishTemplateEntity> templates,
      List<DishTemplateIngredientEntity> ingredients);

  /**
   * 采购就绪判定结果。
   *
   * @param ready 是否可安全生成采购清单
   * @param blockingReasons 阻断原因；就绪时为空
   * @param procurementItems 展开并聚合后的采购食材；未就绪时为空
   */
  record EvaluationResult(boolean ready, List<String> blockingReasons,
                          List<ProcurementItem> procurementItems) {
    /**
     * 防止调用方修改返回的原因集合。
     * @param ready 是否可安全生成采购清单
     * @param blockingReasons 阻断原因
     * @param procurementItems 展开并聚合后的采购食材
     */
    public EvaluationResult {
      blockingReasons = List.copyOf(blockingReasons);
      procurementItems = List.copyOf(procurementItems);
    }
  }

  /**
   * 可直接写入商户菜品的采购食材。
   * @param name 食材名称
   * @param category 食材分类
   * @param quantity 已乘入组件路径倍数的数量
   * @param unit 规范化后的计量单位
   * @param calcType 采购计算方式
   */
  record ProcurementItem(String name, String category, java.math.BigDecimal quantity,
                         String unit, String calcType) { }
}
