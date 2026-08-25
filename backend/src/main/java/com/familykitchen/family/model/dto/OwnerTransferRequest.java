package com.familykitchen.family.model.dto;
import jakarta.validation.constraints.NotNull;
/** 家庭负责人转让请求。
 * @param targetMemberId 目标家庭成员标识
 */
public record OwnerTransferRequest(@NotNull Long targetMemberId) {}
