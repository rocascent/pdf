package com.ysh.util.pdf.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ysh.util.pdf.dto.settlement.SettlementExportData;
import com.ysh.util.pdf.dto.settlement.Page1Data;
import com.ysh.util.pdf.dto.settlement.StationData;
import com.ysh.util.pdf.dto.TaiNengStationFeeCfgData;
import com.ysh.util.pdf.dto.TaiNengStationPayData;
import com.ysh.util.pdf.dto.TaiNengStationTradeData;
import org.springframework.stereotype.Service;
import com.ysh.util.pdf.mapper.DatabaseMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 泰能充平台月度结算单数据查询 —— 按月从数据库汇总，拼装 TaiNengExportData。
 *
 * <p>口径说明（与 PDF 模板备注一致）：
 * <ul>
 *   <li>第 1 页合计 = 各电站对应项目之和；</li>
 *   <li>月充电量 = TBPUB_TRADE.TOTALPOWER（含全部用户），
 *       普通/运营商/泰达机构为按 TBPUB_VIPGRP_ENT.TYPE 0/1/2 的分类明细；</li>
 *   <li>微信/支付宝付款 = TBUSER_BALANCE_REC（CASHTRADETYPE=3 充电支付）
 *       按订单号关联到站点、按 PAYCHANNEL=wxpaym/alipaym 汇总。</li>
 * </ul>
 */
@Service
public class SettlementDataService {

    /** 无 DISTRTYPE=11 配置时手续费字段全部留空；微信/支付宝手续费仍按该费率计算。 */
    private static final BigDecimal FEE_RATE = new BigDecimal("0.006");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final DatabaseMapper databaseMapper;

    public SettlementDataService(DatabaseMapper databaseMapper) {
        this.databaseMapper = databaseMapper;
    }

    /** 查询泰能充结算单数据：[startTime, endTime) 左闭右开，一般为自然月。 */
    public SettlementExportData getTaiNengData(String tenantId, LocalDate startTime, LocalDate endTime) {
        var start = startTime.atStartOfDay();
        var end = endTime.atStartOfDay();

        var trades = databaseMapper.getTaiNengStationTradeData(start, end);
        var pays = databaseMapper.getTaiNengStationPayData(start, end);
        var feeCfgs = databaseMapper.getTaiNengStationFeeCfgData(start, end);

        // 交易表站点号带运营商前缀（如 104313271_101010141），清分配置关联表用裸号（101010141）
        Map<String, TaiNengStationFeeCfgData> feeCfgMap = new HashMap<>();
        feeCfgs.forEach(f -> feeCfgMap.put(f.stationId(), f));

        var data = new SettlementExportData();
        data.stations = buildStations(trades, pays, feeCfgMap);
        data.page1 = buildPage1(tenantId, startTime, data.stations, pays);
        return data;
    }

    // ==================== 站点页 ====================

    private List<StationData> buildStations(List<TaiNengStationTradeData> trades,
                                            List<TaiNengStationPayData> pays,
                                            Map<String, TaiNengStationFeeCfgData> feeCfgMap) {
        Map<String, TaiNengStationPayData> payMap = new HashMap<>();
        pays.forEach(p -> payMap.put(p.stationId(), p));
        return trades.stream().map(t -> {
            var pay = payMap.getOrDefault(t.stationId(),
                    new TaiNengStationPayData(t.stationId(), BigDecimal.ZERO, BigDecimal.ZERO));
            var s = new StationData();
            s.stationName = t.stationName();
            s.stationAddress = t.stationAddress();
            // stationType 库中为整数字典，暂无映射 → 留空
            s.stationType = null;

            s.monthCharge = fmt(t.monthPower());
            s.monthElecFee = fmt(t.monthElecMoney());
            s.monthServFee = fmt(t.monthServMoney());
            s.normalCharge = fmt(t.normalPower());
            s.normalElecFee = fmt(t.normalElecMoney());
            s.normalServFee = fmt(t.normalServMoney());
            s.operatorCharge = fmt(t.operatorPower());
            s.operatorElecFee = fmt(t.operatorElecMoney());
            s.operatorServFee = fmt(t.operatorServMoney());
            s.tedaCharge = fmt(t.tedaPower());
            s.tedaElecFee = fmt(t.tedaElecMoney());
            s.tedaServFee = fmt(t.tedaServMoney());

            // 平台手续费模式/固定金额/费率：取该电站清分配置中 DISTRTYPE=11 的配置；
            // 无配置（或配置无法解析）时相关字段均留空，平台手续费/可提现也无法计算，同样留空
            var monthIncome = nvl(t.monthElecMoney()).add(nvl(t.monthServMoney()));
            var fee = resolveFee(feeCfgMap.get(bareStationId(t.stationId())));
            if (fee != null) {
                s.feeMode = fee.mode();
                if (fee.fixed() != null) s.fixedFee = fmt(fee.fixed());
                if (fee.rate() != null) s.feeRate = rateText(fee.rate());
                var platformFee = fee.fixed() != null
                        ? fee.fixed().setScale(2, RoundingMode.HALF_UP)
                        : monthIncome.multiply(fee.rate()).setScale(2, RoundingMode.HALF_UP);
                s.platformFee = fmt(platformFee);
                s.withdrawable = fmt(monthIncome.subtract(platformFee)
                        .subtract(fee(pay.wechatPayTotal()))
                        .subtract(fee(pay.alipayPayTotal())));
            }
            return s;
        }).toList();
    }

    // ==================== 第 1 页（合计 = 各站点之和） ====================

    private Page1Data buildPage1(String tenantId, LocalDate startTime,
                                 List<StationData> stations,
                                 List<TaiNengStationPayData> pays) {
        var p1 = new Page1Data();

        var info = databaseMapper.getTaiNengOperatorInfo(tenantId);
        p1.operatorName = info == null ? null : info.operatorName();
        p1.creditCode = info == null ? null : info.creditCode();
        p1.address = info == null ? null : info.address();
        p1.contact = info == null ? null : info.contact();

        p1.settleYear = String.valueOf(startTime.getYear());
        p1.settleMonth = String.format("%02d", startTime.getMonthValue());
        var now = LocalDateTime.now();
        p1.exportYear = String.valueOf(now.getYear());
        p1.exportMonth = String.format("%02d", now.getMonthValue());
        p1.exportDay = String.format("%02d", now.getDayOfMonth());
        p1.exportHour = String.format("%02d", now.getHour());
        p1.exportMinute = String.format("%02d", now.getMinute());

        var wechatPay = pays.stream().map(TaiNengStationPayData::wechatPayTotal)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        var alipayPay = pays.stream().map(TaiNengStationPayData::alipayPayTotal)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        var platformFee = BigDecimal.ZERO;       // 平台手续费合计 = 各站之和
        var withdrawable = BigDecimal.ZERO;      // 可提现合计
        var platformFeePresent = false;          // 所有站都无配置时，合计也留空
        var withdrawablePresent = false;
        for (var s : stations) {
            p1.totalCharge = add(p1.totalCharge, s.monthCharge);
            p1.totalElecFee = add(p1.totalElecFee, s.monthElecFee);
            p1.totalServFee = add(p1.totalServFee, s.monthServFee);
            p1.normalCharge = add(p1.normalCharge, s.normalCharge);
            p1.normalElecFee = add(p1.normalElecFee, s.normalElecFee);
            p1.normalServFee = add(p1.normalServFee, s.normalServFee);
            p1.operatorCharge = add(p1.operatorCharge, s.operatorCharge);
            p1.operatorElecFee = add(p1.operatorElecFee, s.operatorElecFee);
            p1.operatorServFee = add(p1.operatorServFee, s.operatorServFee);
            p1.tedaCharge = add(p1.tedaCharge, s.tedaCharge);
            p1.tedaElecFee = add(p1.tedaElecFee, s.tedaElecFee);
            p1.tedaServFee = add(p1.tedaServFee, s.tedaServFee);

            if (s.platformFee != null) {
                platformFee = platformFee.add(parse(s.platformFee));
                platformFeePresent = true;
            }
            if (s.withdrawable != null) {
                withdrawable = withdrawable.add(parse(s.withdrawable));
                withdrawablePresent = true;
            }
        }
        p1.wechatPayTotal = fmt(wechatPay);
        p1.wechatFee = fmt(fee(wechatPay));
        p1.alipayPayTotal = fmt(alipayPay);
        p1.alipayFee = fmt(fee(alipayPay));
        p1.platformFee = platformFeePresent ? fmt(platformFee) : null;
        p1.totalWithdrawable = withdrawablePresent ? fmt(withdrawable) : null;
        return p1;
    }

    // ==================== 工具 ====================

    /**
     * 平台手续费模式（由 DISTRTYPE=11 的清分配置解析）。
     *
     * @param mode  "固定金额" 或 "费率"
     * @param rate  费率（如 0.006 表示 0.6%），固定金额模式下为 null
     * @param fixed 固定手续费金额（元），费率模式下为 null
     */
    record FeeCfg(String mode, BigDecimal rate, BigDecimal fixed) {
    }

    /**
     * 由清分配置 JSON（DISTRRATECFG，如 {"ep":{"prate":0.006},"sp":{...},"tp":{...}}，与
     * DISTRTYPE=1 同构）解析平台手续费模式：含固定金额字段 → 固定金额；含费率字段 → 费率。
     * 解析不出或无配置时返回 null（调用方对应字段留空）。
     */
    static FeeCfg resolveFee(TaiNengStationFeeCfgData cfg) {
        if (cfg == null || cfg.distrRateCfg() == null || cfg.distrRateCfg().isBlank()) return null;
        try {
            JsonNode root = JSON.readTree(cfg.distrRateCfg());
            // 固定金额：顶层或费用项（ep 电费 / sp 服务费 / tp 占位费）里的 fixedfee/fixfee/fixed/fee
            BigDecimal fixed = findNumber(root, "fixedfee", "fixfee", "fixed");
            if (fixed == null) {
                for (String item : new String[]{"ep", "sp", "tp"}) {
                    JsonNode n = root.get(item);
                    if (n != null) {
                        fixed = findNumber(n, "fixedfee", "fixfee", "fixed", "fee");
                        if (fixed != null) break;
                    }
                }
            }
            if (fixed != null) return new FeeCfg("固定金额", null, fixed);
            // 费率：优先取电费项（ep）的 prate，其次顶层或任意费用项的 prate/rate
            JsonNode ep = root.get("ep");
            BigDecimal rate = ep != null ? findNumber(ep, "prate", "rate") : null;
            if (rate == null) rate = findNumber(root, "prate", "rate");
            if (rate == null) {
                for (var it : root) {
                    rate = findNumber(it, "prate", "rate");
                    if (rate != null) break;
                }
            }
            if (rate != null) return new FeeCfg("费率", rate, null);
        } catch (Exception ignore) {
            // 配置非 JSON 或结构不符 → 按无配置处理
        }
        return null;
    }

    /** 在 JSON 节点中按候选字段名（忽略大小写）找第一个数值。 */
    static BigDecimal findNumber(JsonNode node, String... names) {
        if (node == null || !node.isObject()) return null;
        for (String name : names) {
            var fields = node.propertyNames();
            for (var it = fields.iterator(); it.hasNext(); ) {
                String f = it.next();
                if (!name.equalsIgnoreCase(f)) continue;
                JsonNode v = node.get(f);
                if (v != null && v.isNumber()) return v.decimalValue();
            }
        }
        return null;
    }

    /** 交易表站点号带运营商前缀（104313271_101010141），清分配置关联表用裸号（101010141）。 */
    private static String bareStationId(String stationId) {
        if (stationId == null) return null;
        int i = stationId.indexOf('_');
        return i < 0 ? stationId : stationId.substring(i + 1);
    }

    /** 金额/电量格式化：null 安全，统一保留 2 位小数。 */
    private static String fmt(BigDecimal v) {
        return v == null ? null : nvl(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 手续费 = 金额 × 费率（2 位小数，四舍五入）。 */
    private static BigDecimal fee(BigDecimal amount) {
        return nvl(amount).multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    static String rateText() {
        return rateText(FEE_RATE);
    }

    /** 费率文本：0.006 → "0.6%"。 */
    static String rateText(BigDecimal rate) {
        return nvl(rate).movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }

    /** 第 1 页合计：字符串相加（保留 2 位小数）。 */
    private static String add(String a, String b) {
        return fmt(parse(a).add(parse(b)));
    }

    private static BigDecimal parse(String v) {
        try {
            return v == null ? BigDecimal.ZERO : new BigDecimal(v);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
