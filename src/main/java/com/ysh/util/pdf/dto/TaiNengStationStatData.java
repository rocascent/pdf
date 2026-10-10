package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

/**
 * 泰能充结算单单站点清分统计（来自 PWR_EVIDEV_MNGR.TBEVI_TRADE_DISTR，按站点分组）。
 * 同一订单可能按多个清分配置各一行（不同运营商/平台的分成行），
 * 这里按配置属性拆分统计：当前运营商行、其他运营商行、平台（DISTRTYPE=11）行。
 */
public record TaiNengStationStatData(
        String stationId,            // 裸站点号（TBEVI_STATION）
        String stationName,
        String stationAddress,
        Integer stationType,         // 电站类型（TBEVI_STATION.STATIONTYPE 字典值）
        BigDecimal monthPower,       // 月充电量（当前运营商行，清分订单原始交易电量）
        BigDecimal monthElecMoney,   // 月充电电费（当前运营商行清分后）
        BigDecimal monthServMoney,   // 月充电服务费（当前运营商行清分后）
        BigDecimal monthTimeoutFee,  // 月清分超时费（当前运营商行）
        BigDecimal type11ServMoney,  // 平台（DISTRTYPE=11）行的清分服务费 = 平台服务费池
        BigDecimal otherServMoney,   // 其他运营商行的清分服务费（平台服务费分摊分母的一部分）
        BigDecimal balanceDeductFee  // 余额服务扣除费率（当前运营商清分配置，缺省 0.0055）
) {
}
