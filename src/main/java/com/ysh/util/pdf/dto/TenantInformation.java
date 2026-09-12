package com.ysh.util.pdf.dto;

/**
 * 租户运营方信息（含对公银行账户）。
 */
public record TenantInformation(
        String tenantOperatorId,
        String tenantOperatorName,
        String tenantOperatorLongName,
        String bankAccName,
        String bankLongName,
        String bankCardNo
) {
}
