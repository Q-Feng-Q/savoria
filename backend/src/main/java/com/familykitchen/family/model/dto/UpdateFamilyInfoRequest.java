package com.familykitchen.family.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 承载家庭端修改家庭资料相关的请求参数。
 *
 * @param familyName 家庭名称
 * @param note 备注
 * @param contactNames 联系人名称列表
 */
public record UpdateFamilyInfoRequest(
    @NotBlank @Size(max = 100) String familyName,
    @Size(max = 500) String note,
    List<String> contactNames
) {
}
