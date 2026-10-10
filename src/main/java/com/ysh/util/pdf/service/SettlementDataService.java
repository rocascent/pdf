package com.ysh.util.pdf.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import com.ysh.util.pdf.dto.settlement.SettlementExportData;
import com.ysh.util.pdf.dto.settlement.Page1Data;
import com.ysh.util.pdf.dto.settlement.StationData;
import com.ysh.util.pdf.dto.TaiNengBalanceRecData;
import com.ysh.util.pdf.dto.TaiNengStationFeeCfgData;
import com.ysh.util.pdf.dto.TaiNengStationPayData;
import com.ysh.util.pdf.dto.TaiNengStationStatData;
import com.ysh.util.pdf.dto.TaiNengStationTradeData;
import org.springframework.stereotype.Service;
import com.ysh.util.pdf.mapper.DatabaseMapper;

/**
 * 泰能充平台月度结算单数据查询 —— 按月从数据库汇总，拼装 TaiNengExportData。
 *
 * <p>口径说明（与 PDF 模板备注一致）：
 * <ul>
 *   <li>第 1 页合计 = 各电站对应项目之和；</li>
 *   <li>站点统计（月充电量/电费/服务费、站点名称地址）= PWR_EVIDEV_MNGR 清分记录
 *       TBEVI_TRADE_DISTR（SVROPERID=运营商；电量取清分订单原始交易电量）；运营商信息、
 *       DISTRTYPE=11 清分配置亦取 PWR_EVIDEV_MNGR；</li>
 *   <li>普通/运营商/泰达机构拆分 = PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE（TYPE 0/1/2），
 *       统计时间段内全部订单（不筛清分；EVI 端订单可经 TBHLHT_STARTCHARGE 桥接：
 *       TBEVI_TRADE.ORDERNO=EVIORDERNO，ORDERNO=TBPUB_TRADE.ORDERNO）；</li>
 *   <li>微信/支付宝付款 = 按用户 FIFO 回放 PWR_ORDERLY_MNGR_TEDA.TBUSER_BALANCE_REC：
 *       充值（正数）按渠道入池，退款（负数）顺序扣减，充电支付（CASHTRADETYPE=3）
 *       扣到哪个渠道充入的金额就算哪个渠道的付款，归属到订单所在站点；</li>
 *   <li>平台服务费 = DISTRTYPE=11 清分配置的费率列：
 *       清分月充电服务费×DISTRRATESERVFEE + 清分月充电量(度)×DISTRRATESERVWH；
 *       可提现费用 = 当前运营商清分收入（电费+服务费+超时费，按配置 OPERATORID 过滤，
 *       其他运营商/平台的分成行不计入）− 余额服务扣除费
 *       （−ROUND(清分收入×BALANCEDEDUCTFEE, 2)，缺省 0.0055）；
 *       第 1 页可提现合计 = 各站可提现费用之和；</li>
 * </ul>
 */
@Service
public class SettlementDataService {

    /** 无 DISTRTYPE=11 配置时手续费字段全部留空；微信/支付宝手续费仍按该费率计算。 */
    private static final BigDecimal FEE_RATE = new BigDecimal("0.006");

    private final DatabaseMapper databaseMapper;

    public SettlementDataService(DatabaseMapper databaseMapper) {
        this.databaseMapper = databaseMapper;
    }

    /** 查询泰能充结算单数据：[startTime, endTime) 左闭右开，一般为自然月。 */
    public SettlementExportData getTaiNengData(String tenantId, LocalDate startTime, LocalDate endTime) {
        var start = startTime.atStartOfDay();
        var end = endTime.atStartOfDay();

        var stats = databaseMapper.getTaiNengStationStatData(tenantId, start, end); // EVIDEV：清分数据
        var trades = databaseMapper.getTaiNengStationTradeData(start, end);    // TEDA：普通/运营商/泰达拆分
        var pays = calcStationPayTotals(databaseMapper.getTaiNengBalanceRecs(start, end), start, end); // TEDA：微信/支付宝
        var feeCfgs = databaseMapper.getTaiNengStationFeeCfgData(start, end);  // EVIDEV：清分配置

        // 站点号归一：TEDA 交易/流水带运营商前缀（如 104313271_101010141），
        // EVIDEV 站点表与清分配置关联表用裸号（101010141）
        Map<String, TaiNengStationFeeCfgData> feeCfgMap = new HashMap<>();
        feeCfgs.forEach(f -> feeCfgMap.put(f.stationId(), f));

        var data = new SettlementExportData();
        var calcs = calcStations(stats, trades, feeCfgMap);
        data.stations = toStationData(calcs);
        data.page1 = buildPage1(tenantId, startTime, calcs, pays);
        return data;
    }

    // ==================== 站点页 ====================

    /** 机构拆分缺省值（站点无交易时全部为 0）。 */
    private static final TaiNengStationTradeData EMPTY_TRADE = new TaiNengStationTradeData(
            null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    /**
     * 站点数值 DTO：接收数据库返回的清分/拆分数据，承载全部计算结果（未舍入）。
     * 第 1 页求和用它，求和后再统一舍入；展示字符串由 {@link #toStationData} 转换。
     */
    record StationCalc(
            String stationId,
            String stationName,
            String stationAddress,
            Integer stationType,
            String feeMode,              // 平台手续费模式（取自 type-11 配置，展示用）
            String feeRate,              // 平台手续费费率文本（展示用）
            BigDecimal monthCharge,
            BigDecimal monthElecFee,
            BigDecimal monthServFee,
            BigDecimal normalCharge,
            BigDecimal normalElecFee,
            BigDecimal normalServFee,
            BigDecimal operatorCharge,
            BigDecimal operatorElecFee,
            BigDecimal operatorServFee,
            BigDecimal tedaCharge,
            BigDecimal tedaElecFee,
            BigDecimal tedaServFee,
            BigDecimal platformFee,      // 平台手续费（未舍入）：本站分摊的平台服务费
            BigDecimal withdrawable      // 可提现费用（未舍入）：当前运营商清分收入×(1−余额扣除费率)
    ) {
    }

    /**
     * 站点计算：月充电量/电费/服务费与站点名称地址取 EVIDEV 清分数据（stats，
     * 已按配置 OPERATORID 拆分：当前运营商行 + 其他运营商行 + 平台行），
     * 普通/运营商/泰达机构拆分取 TEDA 交易表（trades，按裸站点号关联）。
     * 平台手续费 = Σ各站平台（DISTRTYPE=11）行服务费 × 本站当前运营商清分服务费
     *   / Σ(当前运营商+其他运营商)清分服务费（即：type-11 行的系统已算好的平台服务费池，
     *   按各运营商清分服务费占比划分，如 82.2×131.56/(131.56+197.31)）。
     * 可提现费用 = 当前运营商清分收入（电费+服务费+超时费）×(1−余额服务扣除费率)，
     *   不再扣平台手续费（平台分成行本就不在当前运营商行内）。
     */
    static List<StationCalc> calcStations(List<TaiNengStationStatData> stats,
                                          List<TaiNengStationTradeData> trades,
                                          Map<String, TaiNengStationFeeCfgData> feeCfgMap) {
        Map<String, TaiNengStationTradeData> tradeMap = new HashMap<>();
        trades.forEach(t -> tradeMap.put(bareStationId(t.stationId()), t));
        // 平台服务费池（type-11 行）与分母（各运营商清分服务费合计，全局）
        var pool = stats.stream().map(s -> nvl(s.type11ServMoney()))
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        var shareBase = stats.stream()
                .map(s -> nvl(s.monthServMoney()).add(nvl(s.otherServMoney())))
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        return stats.stream().map(stat -> {
            var t = tradeMap.getOrDefault(stat.stationId(), EMPTY_TRADE);
            // 平台手续费：平台服务费池 × 本站当前运营商清分服务费 / 各运营商清分服务费合计
            var platformFee = shareBase.signum() == 0 ? BigDecimal.ZERO
                    : pool.multiply(nvl(stat.monthServMoney()))
                          .divide(shareBase, 10, RoundingMode.HALF_UP);
            // 可提现费用 = 当前运营商清分收入 ×(1−余额服务扣除费率)（未舍入）
            var gross = nvl(stat.monthElecMoney()).add(nvl(stat.monthServMoney()))
                    .add(nvl(stat.monthTimeoutFee()));
            var deductRate = stat.balanceDeductFee() == null
                    ? new BigDecimal("0.0055") : stat.balanceDeductFee();
            var withdrawable = gross.subtract(gross.multiply(deductRate));
            var feeCfg = feeCfgMap.get(stat.stationId());
            return new StationCalc(stat.stationId(), stat.stationName(), stat.stationAddress(),
                    stat.stationType(),
                    feeCfg == null ? null : "费率",
                    feeCfg == null || feeCfg.distrServFeeRate() == null ? null
                            : rateText(feeCfg.distrServFeeRate()),
                    stat.monthPower(), stat.monthElecMoney(), stat.monthServMoney(),
                    nvl(t.normalPower()), nvl(t.normalElecMoney()), nvl(t.normalServMoney()),
                    nvl(t.operatorPower()), nvl(t.operatorElecMoney()), nvl(t.operatorServMoney()),
                    nvl(t.tedaPower()), nvl(t.tedaElecMoney()), nvl(t.tedaServMoney()),
                    platformFee, withdrawable);
        }).toList();
    }

    /** 数值 DTO → 站点页展示对象（此处才转字符串）。 */
    static List<StationData> toStationData(List<StationCalc> calcs) {
        return calcs.stream().map(c -> {
            var s = new StationData();
            s.stationName = c.stationName();
            s.stationAddress = c.stationAddress();
            s.stationType = stationTypeText(c.stationType());
            s.feeMode = c.feeMode();
            s.feeRate = c.feeRate();
            s.fixedFee = null;                   // 费率模式下无固定金额
            s.monthCharge = fmt(c.monthCharge());
            s.monthElecFee = fmt(c.monthElecFee());
            s.monthServFee = fmt(c.monthServFee());
            s.normalCharge = fmt(c.normalCharge());
            s.normalElecFee = fmt(c.normalElecFee());
            s.normalServFee = fmt(c.normalServFee());
            s.operatorCharge = fmt(c.operatorCharge());
            s.operatorElecFee = fmt(c.operatorElecFee());
            s.operatorServFee = fmt(c.operatorServFee());
            s.tedaCharge = fmt(c.tedaCharge());
            s.tedaElecFee = fmt(c.tedaElecFee());
            s.tedaServFee = fmt(c.tedaServFee());
            s.platformFee = fmt(c.platformFee());
            s.withdrawable = fmt(c.withdrawable());
            return s;
        }).toList();
    }

    // ==================== 第 1 页（合计 = 各站点之和） ====================

    private Page1Data buildPage1(String tenantId, LocalDate startTime,
                                 List<StationCalc> calcs,
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

        buildPage1Totals(p1, calcs, pays);
        return p1;
    }

    /**
     * 第 1 页合计：各站之和。
     * 平台手续费合计 = 有配置的各站之和（全部站都无 DISTRTYPE=11 配置时留空）；
     * 可提现合计始终计算 = 清分费用（清分月电费+清分月服务费−平台手续费，无配置按 0）
     *   − 微信手续费 − 支付宝手续费。
     */
    static void buildPage1Totals(Page1Data p1, List<StationCalc> stations,
                                 List<TaiNengStationPayData> pays) {
        var wechatPay = pays.stream().map(TaiNengStationPayData::wechatPayTotal)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        var alipayPay = pays.stream().map(TaiNengStationPayData::alipayPayTotal)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        var platformFee = BigDecimal.ZERO;       // 平台手续费合计 = 各站原始值之和，最后统一舍入
        var withdrawable = BigDecimal.ZERO;      // 可提现合计 = 各站原始值之和，最后统一舍入
        var totalCharge = BigDecimal.ZERO;
        var totalElecFee = BigDecimal.ZERO;
        var totalServFee = BigDecimal.ZERO;
        var normalCharge = BigDecimal.ZERO;
        var normalElecFee = BigDecimal.ZERO;
        var normalServFee = BigDecimal.ZERO;
        var operatorCharge = BigDecimal.ZERO;
        var operatorElecFee = BigDecimal.ZERO;
        var operatorServFee = BigDecimal.ZERO;
        var tedaCharge = BigDecimal.ZERO;
        var tedaElecFee = BigDecimal.ZERO;
        var tedaServFee = BigDecimal.ZERO;
        for (var s : stations) {
            totalCharge = totalCharge.add(nvl(s.monthCharge()));
            totalElecFee = totalElecFee.add(nvl(s.monthElecFee()));
            totalServFee = totalServFee.add(nvl(s.monthServFee()));
            normalCharge = normalCharge.add(nvl(s.normalCharge()));
            normalElecFee = normalElecFee.add(nvl(s.normalElecFee()));
            normalServFee = normalServFee.add(nvl(s.normalServFee()));
            operatorCharge = operatorCharge.add(nvl(s.operatorCharge()));
            operatorElecFee = operatorElecFee.add(nvl(s.operatorElecFee()));
            operatorServFee = operatorServFee.add(nvl(s.operatorServFee()));
            tedaCharge = tedaCharge.add(nvl(s.tedaCharge()));
            tedaElecFee = tedaElecFee.add(nvl(s.tedaElecFee()));
            tedaServFee = tedaServFee.add(nvl(s.tedaServFee()));
            platformFee = platformFee.add(nvl(s.platformFee()));
            withdrawable = withdrawable.add(nvl(s.withdrawable()));
        }
        p1.totalCharge = fmt(totalCharge);
        p1.totalElecFee = fmt(totalElecFee);
        p1.totalServFee = fmt(totalServFee);
        p1.normalCharge = fmt(normalCharge);
        p1.normalElecFee = fmt(normalElecFee);
        p1.normalServFee = fmt(normalServFee);
        p1.operatorCharge = fmt(operatorCharge);
        p1.operatorElecFee = fmt(operatorElecFee);
        p1.operatorServFee = fmt(operatorServFee);
        p1.tedaCharge = fmt(tedaCharge);
        p1.tedaElecFee = fmt(tedaElecFee);
        p1.tedaServFee = fmt(tedaServFee);
        p1.wechatPayTotal = fmt(wechatPay);
        p1.wechatFee = fmt(fee(wechatPay));
        p1.alipayPayTotal = fmt(alipayPay);
        p1.alipayFee = fmt(fee(alipayPay));
        p1.platformFee = fmt(platformFee);
        // 可提现合计 = 各站未舍入可提现费用之和，最后统一舍入（与 SUM 表运营商级口径一致）；
        // 微信/支付宝付款及手续费行仅为展示项，不参与可提现计算
        p1.totalWithdrawable = fmt(withdrawable);
    }

    // ==================== 微信/支付宝付款合计（FIFO 余额核销） ====================

    /** 充电支付交易类型（TBUSER_BALANCE_REC.CASHTRADETYPE）。 */
    private static final int CASH_TRADE_TYPE_CONSUME = 3;

    /** 一笔充值形成的余额批次：某渠道充入、尚未用完的金额。 */
    private static final class Lot {
        final String channel;
        BigDecimal remaining;

        Lot(String channel, BigDecimal remaining) {
            this.channel = channel;
            this.remaining = remaining;
        }
    }

    /**
     * 按用户回放资金流水（FIFO 钱包模型），计算各站点微信/支付宝付款合计：
     * <ul>
     *   <li>充值等入账（正数金额）按其渠道生成余额批次入队；</li>
     *   <li>退款等出账（负数金额）按先进先出从队头扣减余额批次，不归属渠道；</li>
     *   <li>充电支付（CASHTRADETYPE=3）同样先进先出扣减，扣到哪个渠道充入的金额，
     *       该部分就算哪个渠道的付款，归属到订单所在站点；</li>
     *   <li>结算期外的充电支付只扣减池子（保证顺序正确），不归属；</li>
     *   <li>池子不足的部分（如用户初始余额）无充值渠道可归属，忽略。</li>
     * </ul>
     * recs 需已按 用户、时间、流水号 升序（mapper 的 SQL 已 ORDER BY）。
     */
    List<TaiNengStationPayData> calcStationPayTotals(List<TaiNengBalanceRecData> recs,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, BigDecimal> wechat = new HashMap<>();
        Map<String, BigDecimal> alipay = new HashMap<>();
        Map<String, Deque<Lot>> pools = new HashMap<>();
        String lastUsernum = null;
        Deque<Lot> pool = null;
        for (var r : recs) {
            var amount = r.cashAmount();
            if (amount == null || amount.signum() == 0) continue;
            if (!Objects.equals(r.usernum(), lastUsernum)) {   // 换用户 → 切换余额池
                lastUsernum = r.usernum();
                pool = pools.computeIfAbsent(r.usernum(), k -> new ArrayDeque<>());
            }
            if (amount.signum() > 0) {
                if (r.cashTradeType() != null && r.cashTradeType() == CASH_TRADE_TYPE_CONSUME) {
                    // 充电支付：顺序扣减余额，扣到的部分按充值渠道归属到订单站点
                    if (r.stationId() == null || !inPeriod(r.cashTime(), startTime, endTime)) {
                        consumeFifo(pool, amount);
                        continue;
                    }
                    for (var e : consumeFifo(pool, amount).entrySet()) {
                        if ("wxpaym".equals(e.getKey())) {
                            wechat.merge(r.stationId(), e.getValue(), BigDecimal::add);
                        } else if ("alipaym".equals(e.getKey())) {
                            alipay.merge(r.stationId(), e.getValue(), BigDecimal::add);
                        }
                    }
                } else {
                    // 充值/后台入账：按渠道生成余额批次
                    pool.addLast(new Lot(r.payChannel(), amount));
                }
            } else {
                // 退款等负向流水：仅扣减余额，不归属渠道
                consumeFifo(pool, amount.negate());
            }
        }
        var stationIds = new TreeSet<>(wechat.keySet());
        stationIds.addAll(alipay.keySet());
        return stationIds.stream()
                .map(id -> new TaiNengStationPayData(id,
                        wechat.getOrDefault(id, BigDecimal.ZERO),
                        alipay.getOrDefault(id, BigDecimal.ZERO)))
                .toList();
    }

    /** 从余额池队头开始顺序扣减 amount，返回 渠道 → 扣减金额（池子不足时扣到空为止）。 */
    private static Map<String, BigDecimal> consumeFifo(Deque<Lot> pool, BigDecimal amount) {
        Map<String, BigDecimal> byChannel = new LinkedHashMap<>();
        var remaining = amount;
        while (remaining.signum() > 0 && !pool.isEmpty()) {
            var head = pool.peekFirst();
            var take = head.remaining.min(remaining);
            head.remaining = head.remaining.subtract(take);
            remaining = remaining.subtract(take);
            if (take.signum() > 0) byChannel.merge(head.channel, take, BigDecimal::add);
            if (head.remaining.signum() == 0) pool.pollFirst();
        }
        return byChannel;
    }

    private static boolean inPeriod(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        return time != null && !time.isBefore(start) && time.isBefore(end);
    }

    // ==================== 工具 ====================

    /** 交易表站点号带运营商前缀（104313271_101010141），清分配置关联表用裸号（101010141）。 */

    /** 交易表站点号带运营商前缀（104313271_101010141），清分配置关联表用裸号（101010141）。 */
    private static String bareStationId(String stationId) {
        if (stationId == null) return null;
        int i = stationId.indexOf('_');
        return i < 0 ? stationId : stationId.substring(i + 1);
    }

    /**
     * 电站类型字典（TBEVI_STATION.STATIONTYPE）：
     * 1=公共，50=个人，100=公交（专用），101=环卫（专用），
     * 102=物流（专用），103=出租车（专用），255=其他；未知值留空。
     */
    static String stationTypeText(Integer type) {
        if (type == null) return null;
        return switch (type) {
            case 1 -> "公共";
            case 50 -> "个人";
            case 100 -> "公交（专用）";
            case 101 -> "环卫（专用）";
            case 102 -> "物流（专用）";
            case 103 -> "出租车（专用）";
            case 255 -> "其他";
            default -> null;
        };
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
    private static BigDecimal parse(String v) {
        try {
            return v == null ? BigDecimal.ZERO : new BigDecimal(v);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
