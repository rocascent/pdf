package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

/**
 * 泰能充结算单单站点交易汇总（来自 TBPUB_TRADE，按站点分组）。
 * normal/operator/teda 三列机构口径与 EntIncomeData 一致（TYPE 0/1/2）。
 */
public record TaiNengStationTradeData(
        String stationId,
        String stationName,
        String stationAddress,
        BigDecimal monthPower,        // 月充电量（全部用户）
        BigDecimal monthElecMoney,    // 月充电电费（全部用户）
        BigDecimal monthServMoney,    // 月充电服务费（全部用户）
        BigDecimal normalPower,       // 普通机构充电量
        BigDecimal normalElecMoney,   // 普通机构充电电费
        BigDecimal normalServMoney,   // 普通机构充电服务费
        BigDecimal operatorPower,     // 运营商机构充电量
        BigDecimal operatorElecMoney, // 运营商机构充电电费
        BigDecimal operatorServMoney, // 运营商机构充电服务费
        BigDecimal tedaPower,         // 泰达电力机构充电量
        BigDecimal tedaElecMoney,     // 泰达电力机构充电电费
        BigDecimal tedaServMoney      // 泰达电力机构充电服务费
) {
}
