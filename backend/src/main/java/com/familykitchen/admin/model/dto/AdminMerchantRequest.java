package com.familykitchen.admin.model.dto;
import jakarta.validation.constraints.*;
/**
 * 承载平台管理商户相关的请求参数。
 *
 * @param name 名称
 * @param contactName 联系人名称
 * @param contactPhone 联系人联系电话
 * @param ownerUserId 负责人用户标识
 * @param status 状态
 */
public record AdminMerchantRequest(@NotBlank @Size(max=100) String name,@Size(max=50) String contactName,
    @Size(max=30) String contactPhone,@NotNull Long ownerUserId,@Pattern(regexp="active|inactive|pending") String status) {}
