package com.familykitchen.wallet.model.dto;

import com.familykitchen.wallet.model.enums.LedgerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 承载Adjust成员余额相关的请求参数。
 *
 * @param type 类型
 * @param amount 金额
 * @param remark 备注
 */
public record AdjustMemberBalanceRequest(
    @NotNull LedgerType type,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String remark
) {
}


