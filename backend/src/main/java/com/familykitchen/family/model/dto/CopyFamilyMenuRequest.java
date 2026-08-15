package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 承载Copy家庭菜单相关的请求参数。
 *
 * @param sourceFamilyId 来源家庭标识
 */
public record CopyFamilyMenuRequest(@NotNull Long sourceFamilyId) {
}

