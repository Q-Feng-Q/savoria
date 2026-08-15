package com.familykitchen.family.model.dto;
import jakarta.validation.constraints.NotNull;
/** 家庭负责人转让请求。 
 * @param targetUserId 目标用户标识
 */
public record OwnerTransferRequest(@NotNull Long targetUserId) {}
