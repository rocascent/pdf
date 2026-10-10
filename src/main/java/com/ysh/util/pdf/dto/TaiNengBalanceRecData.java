package com.ysh.util.pdf.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 泰能充用户资金流水（TBUSER_BALANCE_REC，按用户+时间+流水号升序）。
 *
 * <p>用于微信/支付宝付款合计的 FIFO 余额核销：充值（正数）按其渠道入池，
 * 退款（负数）与充电支付顺序扣减，扣到哪个渠道的充值就算哪个渠道的付款。
 */
public record TaiNengBalanceRecData(
        String usernum,
        String cashId,
        Integer cashTradeType,   // 1=充值 3=充电支付 4=退款 11/21/50=后台入账调整…
        String payChannel,       // wxpaym / alipaym / pubpile / appmngr / manupay / null
        BigDecimal cashAmount,   // 正数=入账（充值/消费扣款），负数=退款等出账
        LocalDateTime cashTime,
        String stationId         // 充电支付（type=3）经 PAYOUTERNUM→TBPUB_TRADE 关联的订单站点，其余为 null
) {
}
