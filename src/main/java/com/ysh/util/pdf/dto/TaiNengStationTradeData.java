package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

/**
 * 泰能充结算单单站点普通/运营商/泰达机构拆分（来自 PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE，
 * 机构口径与 EntIncomeData 一致：TBPUB_VIPGRP_ENT.TYPE 0/1/2）。
 */
public record TaiNengStationTradeData(
        String stationId,             // 带运营商前缀（如 104313271_101010141）
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
