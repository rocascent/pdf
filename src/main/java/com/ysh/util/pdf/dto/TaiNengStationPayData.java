package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

/**
 * 泰能充结算单单站点支付渠道汇总（来自 TBUSER_BALANCE_REC，按订单关联到站点）。
 */
public record TaiNengStationPayData(
        String stationId,
        BigDecimal wechatPayTotal,  // 微信付款小计
        BigDecimal alipayPayTotal   // 支付宝付款小计
) {
}
