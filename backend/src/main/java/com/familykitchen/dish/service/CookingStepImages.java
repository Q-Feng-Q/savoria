package com.familykitchen.dish.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared validation and lossless legacy request resolution, run before replacing any rows. */
public final class CookingStepImages {
  private CookingStepImages() { }

  public static List<String> validate(List<String> images) {
    if (images == null) return null;
    if (images.size() > 5) throw bad("每个制作步骤最多5张图片");
    for (String value : images) {
      if (value == null || value.isBlank() || value.length() > 2048 ||
          value.chars().anyMatch(c -> Character.isISOControl(c) || Character.isWhitespace(c)) || value.contains("\\")) {
        throw bad("步骤图片地址无效");
      }
      try {
        URI uri = URI.create(value);
        boolean local = value.startsWith("/uploads/images/") || value.startsWith("/images/")
            || value.startsWith("/uploads/dish-template-assets/");
        boolean https = "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
        String path = uri.getPath();
        if ((!local && !https) || (local && (uri.getScheme() != null || uri.getRawAuthority() != null))
            || path == null || path.contains("\\") || path.chars().anyMatch(Character::isISOControl)
            || java.util.Arrays.stream(path.split("/", -1)).anyMatch(p -> p.equals(".") || p.equals(".."))) {
          throw bad("步骤图片地址无效");
        }
      } catch (IllegalArgumentException ex) { throw bad("步骤图片地址无效"); }
    }
    return List.copyOf(images);
  }

  public static List<DishRequest.CookingStepRequest> resolveDish(List<DishRequest.CookingStepRequest> requested,
      List<DishCookingStepEntity> previous) {
    List<DishCookingStepEntity> current = previous == null ? List.of() : previous;
    if (requested == null) return current.stream().map(CookingStepImages::toRequest).toList();
    boolean missing = requested.stream().anyMatch(s -> s == null || s.imageUrls() == null);
    if (missing && current.stream().anyMatch(s -> !s.getImageUrls().isEmpty())) {
      if (requested.size() != current.size()) throw stale();
      for (int i = 0; i < requested.size(); i++) {
        if (!sameMetadata(requested.get(i), current.get(i))) throw stale();
      }
    }
    List<DishRequest.CookingStepRequest> resolved = new ArrayList<>();
    for (int i = 0; i < requested.size(); i++) {
      var s = requested.get(i);
      if (s == null) throw bad("制作步骤不能为空");
      List<String> images = s.imageUrls();
      if (images == null) images = i < current.size() && sameMetadata(s, current.get(i))
          ? current.get(i).getImageUrls() : List.of();
      resolved.add(new DishRequest.CookingStepRequest(s.stepNo(), s.title(), s.content(), s.durationSeconds(),
          s.temperatureText(), s.heatLevel(), s.componentTemplateId(), validate(images)));
    }
    return List.copyOf(resolved);
  }

  public static DishRequest.CookingStepRequest toRequest(DishCookingStepEntity s) {
    return new DishRequest.CookingStepRequest(s.getStepNo(), s.getTitle(), s.getContent(), s.getDurationSeconds(),
        s.getTemperatureText(), s.getHeatLevel(), s.getComponentTemplateId(), s.getImageUrls());
  }

  public static List<String> resolveTemplate(Long templateId, String itemId, List<String> images,
      List<DishTemplateCookingStepEntity> current) {
    if (images != null) return validate(images);
    if (current != null) for (int i = 0; i < current.size(); i++) {
      var step = current.get(i);
      String key = step.getItemKey() == null ? "template:" + templateId + ":step:" + i : step.getItemKey();
      if (Objects.equals(itemId, key)) return step.getImageUrls();
    }
    // Older web/mini editors derived item IDs from database IDs rather than itemKey.
    // Resolve those aliases only after exact keys, and never by mutable position.
    if (current != null) for (var step : current) {
      if (step.getId() != null && (Objects.equals(itemId, "step-" + step.getId())
          || Objects.equals(itemId, "template-step:" + step.getId()))) return step.getImageUrls();
    }
    return List.of();
  }

  private static boolean sameMetadata(DishRequest.CookingStepRequest a, DishCookingStepEntity b) {
    return a != null && Objects.equals(a.stepNo(), b.getStepNo()) && Objects.equals(a.title(), b.getTitle())
        && Objects.equals(a.content(), b.getContent()) && Objects.equals(a.durationSeconds(), b.getDurationSeconds())
        && Objects.equals(a.temperatureText(), b.getTemperatureText()) && Objects.equals(a.heatLevel(), b.getHeatLevel())
        && Objects.equals(a.componentTemplateId(), b.getComponentTemplateId());
  }
  private static BusinessException stale() { return bad("步骤图片信息缺失且步骤已变化，请刷新后重新编辑"); }
  private static BusinessException bad(String message) { return new BusinessException(ErrorCode.BAD_REQUEST, message); }
}
