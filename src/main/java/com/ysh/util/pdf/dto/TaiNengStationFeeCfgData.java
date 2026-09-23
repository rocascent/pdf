package com.ysh.util.pdf.dto;

/**
 * 泰能充结算单单站点平台手续费清分配置（来自 TBEVI_TRADE_DISTR_GRP_CFG，DISTRTYPE=11）。
 */
public record TaiNengStationFeeCfgData(
        String stationId,     // 裸站点号（无运营商前缀，与 TBEVI_TRADE_DISTR_STAGRP_RELA 一致）
        String distrRateCfg   // 清分配置 JSON
) {
}
