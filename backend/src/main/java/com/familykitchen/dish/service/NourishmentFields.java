package com.familykitchen.dish.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishTemplateEntity;

/** Shared normalization; omitted fields preserve persisted data on updates. */
public final class NourishmentFields {
  private NourishmentFields() { }
  public static String type(String value, String previous) {
    if (value == null) return previous == null ? "NORMAL" : previous;
    if (!"NORMAL".equals(value) && !"NOURISHMENT".equals(value)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "productType仅支持NORMAL或NOURISHMENT");
    }
    return value;
  }
  public static String text(String value, String previous) {
    if (value == null) return previous;
    String normalized = value.strip();
    if (normalized.length() > 1000) throw new BusinessException(ErrorCode.BAD_REQUEST, "滋补文字最多1000字符");
    return normalized.isEmpty() ? null : normalized;
  }
  public static void apply(DishEntity target, DishRequest request, DishEntity previous) {
    target.setProductType(type(request.productType(), previous == null ? null : previous.getProductType()));
    target.setNourishmentDescription(text(request.nourishmentDescription(), previous == null ? null : previous.getNourishmentDescription()));
    target.setServingAdvice(text(request.servingAdvice(), previous == null ? null : previous.getServingAdvice()));
    target.setPrecautions(text(request.precautions(), previous == null ? null : previous.getPrecautions()));
  }
  public static void apply(DishTemplateEntity target, String productType, String description, String advice, String precautions) {
    String type = type(productType, target.getProductType());
    if ("COMPONENT".equals(target.getTemplateType()) && ("NOURISHMENT".equals(type)
        || (description != null && !description.isBlank()) || (advice != null && !advice.isBlank())
        || (precautions != null && !precautions.isBlank()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "配料组件不支持滋补信息");
    }
    target.setProductType(type);
    target.setNourishmentDescription(text(description, target.getNourishmentDescription()));
    target.setServingAdvice(text(advice, target.getServingAdvice()));
    target.setPrecautions(text(precautions, target.getPrecautions()));
  }
}
