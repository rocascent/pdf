package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

/**
 * 租户结算汇总数据（各费用合计），对应 TBEVI_TRADE_DISTR_SUM 的统计结果。
 * 查询区间无数据时各字段为 null。
 */
public record TenantClrData(
        BigDecimal totalElecMoney,
        BigDecimal totalSeviceMoney,
        BigDecimal totalTimeoutMoney,
        BigDecimal distrElecFee,
        BigDecimal distrServFee,
        BigDecimal distrTimeoutFee
) {
}
