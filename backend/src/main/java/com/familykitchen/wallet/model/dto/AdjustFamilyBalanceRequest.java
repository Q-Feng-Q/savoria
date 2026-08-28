package com.familykitchen.wallet.model.dto;

import com.familykitchen.wallet.model.enums.LedgerType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Merchant adjustment command for one path-scoped family wallet.
 * @param requestId idempotency identifier
 * @param type manual credit or manual debit
 * @param amount positive amount with at most two decimals
 * @param remark required audit reason
 */
public record AdjustFamilyBalanceRequest(
    @NotBlank @Size(max = 64) String requestId,
    @NotNull LedgerType type,
    @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
    @NotBlank @Size(max = 500) String remark) {}
