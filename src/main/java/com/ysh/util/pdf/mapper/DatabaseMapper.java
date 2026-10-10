package com.ysh.util.pdf.mapper;

import com.ysh.util.pdf.dto.*;
import com.ysh.util.pdf.dto.TaiNengBalanceRecData;
import com.ysh.util.pdf.dto.TaiNengStationFeeCfgData;
import com.ysh.util.pdf.dto.TaiNengStationPayData;
import com.ysh.util.pdf.dto.TaiNengStationStatData;
import com.ysh.util.pdf.dto.TaiNengStationTradeData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DatabaseMapper {
    @Select("""
            SELECT TENANTOPERATORID       AS tenantOperatorId,
                   TENANTOPERATORNAME     AS tenantOperatorName,
                   TENANTOPERATORLONGNAME AS tenantOperatorLongName,
                   BANKACCNAME            AS bankAccName,
                   BANKLONGNAME           AS bankLongName,
                   BANKCARDNO             AS bankCardNo
            FROM TBTENANT_OPERATOR tb
                     LEFT JOIN TBTENANT_DISTRCFG td ON tb.TENANTOPERATORID = td.OPERATORID
            WHERE tb.TENANTOPERATORID = #{tenantId};
            """)
    TenantInformation getTenantInformation(String tenantId);

    @Select("""
            SELECT SUM(TOTALELECMONEY)    AS totalElecMoney,
                   SUM(TOTALSEVICEMONEY)  AS totalSeviceMoney,
                   SUM(TOTALTIMEOUTMONEY) AS totalTimeoutMoney,
                   SUM(DISTRELECFEE)      AS distrElecFee,
                   SUM(DISTRSERVFEE)      AS distrServFee,
                   SUM(DISTRTIMEOUTFEE)   AS distrTimeoutFee
            FROM TBEVI_TRADE_DISTR_SUM AS ttds
            WHERE ttds.OPERATORID = #{tenantId}
            AND ttds.STATTIME >= #{startTime} AND ttds.STATTIME < #{endTime};
            """)
    TenantClrData getTenantClrData(@Param("tenantId") String tenantId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT CONCAT(ts.STATIONNAME, ' / ', ts.STATIONID) AS name,
                   SUM(DISTRELECFEE)                           AS clrElecFee,
                   SUM(DISTRSERVFEE)                           AS clrServFee,
                   SUM(DISTRTIMEOUTFEE)                        AS clrTmoutFee
            FROM TBEVI_TRADE_DISTR AS ttd
                          LEFT JOIN TBEVI_TRADE AS tt
            ON ttd.ORDERNO = tt.ORDERNO
                LEFT JOIN TBEVI_CONNECTOR AS tc ON TT.CONNECTORID = tc.CONNECTORID
                LEFT JOIN TBEVI_STATION AS ts ON tc.STATIONID = TS.STATIONID
            WHERE ttd.SVROPERID = #{tenantId}
                          AND ttd.STATTIME >= #{startTime} AND ttd.STATTIME < #{endTime}
            GROUP BY ts.STATIONID
            """)
    List<StationClrData> getStationClrData(@Param("tenantId") String tenantId,
                                           @Param("startTime") LocalDate startTime,
                                           @Param("endTime") LocalDate endTime);

    @Select("""
            SELECT s.STATIONID                                                                                     AS id,
                   CONCAT(s.STATIONNAME, ' / ', s.STATIONID)                                                       AS name,
                   SUM(IF(td.SVROPERID = #{tenantId}, td.DISTRELECFEE + td.DISTRSERVFEE + td.DISTRTIMEOUTFEE, 0))  AS selfFee,
                   SUM(IF(td.SVROPERID != #{tenantId}, td.DISTRELECFEE + td.DISTRSERVFEE + td.DISTRTIMEOUTFEE, 0)) AS otherFee
            FROM TBEVI_TRADE_DISTR td
                     LEFT JOIN TBEVI_TRADE t ON td.ORDERNO = t.ORDERNO
                     LEFT JOIN TBEVI_CONNECTOR c ON t.CONNECTORID = c.CONNECTORID
                     LEFT JOIN TBEVI_STATION s ON c.STATIONID = s.STATIONID
            WHERE td.STATTIME >= #{startTime} AND  td.STATTIME < #{endTime}
            GROUP BY s.STATIONID
            """)
    List<StationTotalClrData> getStationTotalClrData(@Param("tenantId") String tenantId,
                                                     @Param("startTime") LocalDate startTime,
                                                     @Param("endTime") LocalDate endTime);

//    List<StationInfo> getStationInfo();

    @Select("""
            SELECT s.STATIONID                                     AS id,
                   FIRST_VALUE(s.STATIONNAME)                      AS name,
                   FIRST_VALUE(s.FREEPARK)                         AS parkFee,
                   SUM(t.EVIELECMNYORIG)                           AS origElecFee,
                   SUM(t.EVIELECMNYORIG - t.EVIELECMNYDC)          AS dcElecFee,
                   ROUND(SUM(t.TOTALELECMONEY * 0.006), 2)         AS handlElecFee,
                   ROUND(SUM(t.TOTALELECMONEY * (1 - 0.006)), 2)   AS elecFee,
                   SUM(t.EVISERVMNYORIG)                           AS origServFee,
                   SUM(t.EVISERVMNYORIG - t.EVISERVMNYDC)          AS dcServeFee,
                   ROUND(SUM(t.TOTALSEVICEMONEY * 0.006), 2)       AS handlServFee,
                   ROUND(SUM(t.TOTALSEVICEMONEY * (1 - 0.006)), 2) AS servFee,
                   SUM(t.EVITMOUTMNYORIG)                          AS origTmoutFee,
                   SUM(t.EVITMOUTMNYORIG - t.EVITMOUTMNYDC)        AS dcTmoutFee,
                   ROUND(SUM(t.TOTALTMOUTMONEY * 0.006), 2)        AS handlTmoutFee,
                   ROUND(SUM(t.TOTALTMOUTMONEY * (1 - 0.006)), 2)  AS tmoutFee
            FROM TBEVI_TRADE t
                     LEFT JOIN TBEVI_CONNECTOR c ON t.CONNECTORID = c.CONNECTORID
                     LEFT JOIN TBEVI_STATION s ON c.STATIONID = s.STATIONID
            WHERE t.PUSHTIMESTAMP >= #{startTime}
              AND t.PUSHTIMESTAMP < #{endTime}
            GROUP BY s.STATIONID
            """)
    List<StationDetailData> getStationDetailData(@Param("startTime") LocalDate startTime, @Param("endTime") LocalDate endTime);

    @Select("""
            SELECT s.STATIONID                        as stationId,
                   FIRST_VALUE(o.TENANTOPERATORNAME)  AS operatorName,
                   FIRST_VALUE(tdgc.DISTRRATEELECFEE) AS elecRate,
                   FIRST_VALUE(tdgc.DISTRRATESERVFEE) AS servRate,
                   FIRST_VALUE(tdgc.DISTRRATETIMEOUT) AS tmoutRate,
                   SUM(td.DISTRELECFEE)               AS elecFee,
                   sum(td.DISTRSERVFEE)               AS servFee,
                   sum(td.DISTRTIMEOUTFEE)            AS tmoutFee
            FROM TBEVI_TRADE_DISTR td
                     LEFT JOIN TBEVI_TRADE_DISTR_GRP_CFG tdgc ON td.DISTRCFGID = tdgc.DISTRCFGID
                     LEFT JOIN TBEVI_TRADE t ON td.ORDERNO = t.ORDERNO
                     LEFT JOIN TBEVI_CONNECTOR c ON t.CONNECTORID = c.CONNECTORID
                     LEFT JOIN TBEVI_STATION s ON c.STATIONID = s.STATIONID
                     LEFT JOIN TBTENANT_OPERATOR o on td.SVROPERID = o.TENANTOPERATORID
            WHERE td.STATTIME >= #{startTime}
              AND td.STATTIME < #{endTime}
            GROUP BY s.STATIONID, td.SVROPERID
            """)
    List<StationClrDetailData> getStationClrDetailData(@Param("startTime") LocalDate startTime, @Param("endTime") LocalDate endTime);

    @Select("""
            SELECT NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALELECMONEY END), 0)   AS normalElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALSEVICEMONEY END), 0) AS normalServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALTMOUTMONEY END), 0)  AS normalTmoutMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALPOWER END), 0)       AS normalPower,
                   COUNT(CASE WHEN e.TYPE = 0 THEN 1 END)                         AS normalCount,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALELECMONEY END), 0)   AS operatorElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALSEVICEMONEY END), 0) AS operatorServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALTMOUTMONEY END), 0)  AS operatorTmoutMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALPOWER END), 0)       AS operatorPower,
                   COUNT(CASE WHEN e.TYPE = 1 THEN 1 END)                         AS operatorCount,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALELECMONEY END), 0)   AS tedaElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALSEVICEMONEY END), 0) AS tedaServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALTMOUTMONEY END), 0)  AS tedaTmoutMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALPOWER END), 0)       AS tedaPower,
                   COUNT(CASE WHEN e.TYPE = 2 THEN 1 END)                         AS tedaCount
            FROM (SELECT *
                  FROM PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE
                  WHERE PUSHTIMESTAMP >= #{startTime} AND PUSHTIMESTAMP < #{endTime}) pt
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBUSER u ON pt.USERNUM = u.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_USER_RELA vur ON u.USERNUM = vur.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_ENT e ON vur.VIPGRPID = e.ENTID
            """)
    EntIncomeData getEntIncomeData(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT CONCAT(s.STATIONNAME, ' / ', s.STATIONID)                      AS name,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALELECMONEY END), 0)   AS normalElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALSEVICEMONEY END), 0) AS normalServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALTMOUTMONEY END), 0)  AS normalTmoutMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALELECMONEY END), 0)   AS operatorElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALSEVICEMONEY END), 0) AS operatorServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALTMOUTMONEY END), 0)  AS operatorTmoutMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALELECMONEY END), 0)   AS tedaElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALSEVICEMONEY END), 0) AS tedaServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALTMOUTMONEY END), 0)  AS tedaTmoutMoney
            FROM (SELECT *
                  FROM PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE
                  WHERE PUSHTIMESTAMP >= #{startTime} AND PUSHTIMESTAMP < #{endTime}) pt
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBUSER u ON pt.USERNUM = u.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_USER_RELA vur ON u.USERNUM = vur.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_ENT e ON vur.VIPGRPID = e.ENTID
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_STATION s on pt.STATIONID = s.STATIONID
            WHERE e.TYPE IN (0, 1, 2)
            GROUP BY pt.STATIONID;
            """)
    List<StationEntIncomeData> getStationEntIncomeData(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // ==================== 泰能充月度结算单 ====================

    @Select("""
            SELECT NVL(TENANTOPERATORLONGNAME, TENANTOPERATORNAME) AS operatorName,
                   TENANTUNIFIEDCODE                               AS creditCode,
                   TENANTOPERATORREGADDRESS                        AS address,
                   CONTACTSUSER1                                   AS contact,
                   TENANTOPERATORTEL1                              AS phone
            FROM PWR_EVIDEV_MNGR.TBTENANT_OPERATOR
            WHERE TENANTOPERATORID = #{tenantId};
            """)
    TaiNengOperatorInfo getTaiNengOperatorInfo(@Param("tenantId") String tenantId);

    /**
     * 各站点清分统计（电站/总览页的月充电量、月充电电费、月充电服务费）：
     * 取 PWR_EVIDEV_MNGR 清分记录 TBEVI_TRADE_DISTR（SVROPERID=运营商），
     * 清分电费/服务费为 DISTRELECFEE/DISTRSERVFEE；另取超时费与余额服务扣除费率
     * （清分配置 BALANCEDEDUCTFEE，按明细 DISTRCFGID 关联，缺省 0.0055）供可提现费用计算。
     * 同一订单可能按多个清分配置各一行（不同运营商/平台的分成行），
     * 这里只取清分配置 OPERATORID=当前运营商的行（可提现只算当前运营商的份额）。
     * 清分明细一单一行，SUM 不会重复。
     */
    @Select("""
            SELECT s.STATIONID                AS stationId,
                   FIRST_VALUE(s.STATIONNAME) AS stationName,
                   FIRST_VALUE(s.ADDRESS)     AS stationAddress,
                   FIRST_VALUE(s.STATIONTYPE) AS stationType,
                   SUM(CASE WHEN gc.OPERATORID = #{tenantId} THEN t.TOTALPOWER END)       AS monthPower,
                   SUM(CASE WHEN gc.OPERATORID = #{tenantId} THEN td.DISTRELECFEE END)    AS monthElecMoney,
                   SUM(CASE WHEN gc.OPERATORID = #{tenantId} THEN td.DISTRSERVFEE END)    AS monthServMoney,
                   SUM(CASE WHEN gc.OPERATORID = #{tenantId} THEN td.DISTRTIMEOUTFEE END) AS monthTimeoutFee,
                   SUM(CASE WHEN gc.DISTRTYPE = 11 THEN td.DISTRSERVFEE END)              AS type11ServMoney,
                   SUM(CASE WHEN NVL(gc.OPERATORID, ' ') <> #{tenantId}
                             AND NVL(gc.DISTRTYPE, 0) <> 11 THEN td.DISTRSERVFEE END)     AS otherServMoney,
                   MAX(CASE WHEN gc.OPERATORID = #{tenantId}
                            THEN NVL(gc.BALANCEDEDUCTFEE, 0.0055) END)                    AS balanceDeductFee
            FROM PWR_EVIDEV_MNGR.TBEVI_TRADE_DISTR td
                     LEFT JOIN PWR_EVIDEV_MNGR.TBEVI_TRADE t ON td.ORDERNO = t.ORDERNO
                     LEFT JOIN PWR_EVIDEV_MNGR.TBEVI_CONNECTOR c ON t.CONNECTORID = c.CONNECTORID
                     LEFT JOIN PWR_EVIDEV_MNGR.TBEVI_STATION s ON c.STATIONID = s.STATIONID
                     LEFT JOIN PWR_EVIDEV_MNGR.TBEVI_TRADE_DISTR_GRP_CFG gc ON td.DISTRCFGID = gc.DISTRCFGID
            WHERE td.SVROPERID = #{tenantId}
              AND td.STATTIME >= #{startTime} AND td.STATTIME < #{endTime}
            GROUP BY s.STATIONID
            ORDER BY s.STATIONID
            """)
    List<TaiNengStationStatData> getTaiNengStationStatData(@Param("tenantId") String tenantId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 各站点普通/运营商/泰达机构拆分（PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE + 机构表）：
     * 机构口径：TBPUB_VIPGRP_ENT.TYPE，0=普通 1=运营商 2=泰达电力。
     * 口径约定：PUB 端机构拆分统计时间段内全部订单（不筛是否已清分，
     * 可经 TBHLHT_STARTCHARGE 桥接：TBEVI_TRADE.ORDERNO=EVIORDERNO，
     * ORDERNO=TBPUB_TRADE.ORDERNO）；与 EVI 端仅统计清分记录的站点统计并存。
     */
    @Select("""
            SELECT pt.STATIONID                                                     AS stationId,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALPOWER END), 0)         AS normalPower,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALELECMONEY END), 0)     AS normalElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALSEVICEMONEY END), 0)   AS normalServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALPOWER END), 0)         AS operatorPower,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALELECMONEY END), 0)     AS operatorElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALSEVICEMONEY END), 0)   AS operatorServMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALPOWER END), 0)         AS tedaPower,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALELECMONEY END), 0)     AS tedaElecMoney,
                   NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALSEVICEMONEY END), 0)   AS tedaServMoney
            FROM PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE pt
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBUSER u ON pt.USERNUM = u.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_USER_RELA vur ON u.USERNUM = vur.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_ENT e ON vur.VIPGRPID = e.ENTID
            WHERE pt.PUSHTIMESTAMP >= #{startTime} AND pt.PUSHTIMESTAMP < #{endTime}
            GROUP BY pt.STATIONID
            ORDER BY pt.STATIONID
            """)
    List<TaiNengStationTradeData> getTaiNengStationTradeData(@Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 用户资金流水（仅限结算期内有充电支付的用户，取结算期结束前的全部记录）：
     * 按用户+时间+流水号升序，供 Java 侧 FIFO 回放计算微信/支付宝付款合计。
     */
    @Select("""
            SELECT br.USERNUM       AS usernum,
                   br.CASHID        AS cashId,
                   br.CASHTRADETYPE AS cashTradeType,
                   br.PAYCHANNEL    AS payChannel,
                   br.CASHAMOUNT    AS cashAmount,
                   br.CASHTIME      AS cashTime,
                   pt.STATIONID     AS stationId
            FROM PWR_ORDERLY_MNGR_TEDA.TBUSER_BALANCE_REC br
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE pt ON br.PAYOUTERNUM = pt.ORDERNO
            WHERE br.CASHTIME < #{endTime}
              AND br.USERNUM IN (SELECT br2.USERNUM
                                 FROM PWR_ORDERLY_MNGR_TEDA.TBUSER_BALANCE_REC br2
                                 WHERE br2.CASHTRADETYPE = 3
                                   AND br2.CASHTIME >= #{startTime}
                                   AND br2.CASHTIME < #{endTime})
            ORDER BY br.USERNUM, br.CASHTIME, br.CASHID
            """)
    List<TaiNengBalanceRecData> getTaiNengBalanceRecs(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 各站点生效的平台手续费清分配置（DISTRTYPE = 11）：
     * 站点 → 清分组（TBEVI_TRADE_DISTR_STAGRP_RELA）→ 组内 DISTRTYPE=11、ISVALID=1
     * 且有效期覆盖结算期的配置；多条重叠时取 CFGSTARTTIME 最新一条。
     * 平台服务费 = 清分服务费×DISTRRATESERVFEE + 清分电量×DISTRRATESERVWH。
     */
    @Select("""
            SELECT t.stationId    AS stationId,
                   t.servFeeRate  AS distrServFeeRate
            FROM (SELECT r.STATIONID    AS stationId,
                         c.DISTRRATESERVFEE AS servFeeRate,
                         ROW_NUMBER() OVER (PARTITION BY r.STATIONID ORDER BY c.CFGSTARTTIME DESC) AS rn
                  FROM PWR_EVIDEV_MNGR.TBEVI_TRADE_DISTR_STAGRP_RELA r
                           INNER JOIN PWR_EVIDEV_MNGR.TBEVI_TRADE_DISTR_GRP_CFG c
                               ON c.DISTRSTAGROUPID = r.DISTRSTAGROUPID
                  WHERE c.DISTRTYPE = 11
                    AND c.ISVALID = 1
                    AND c.CFGSTARTTIME < #{endTime}
                    AND c.CFGENDTIME >= #{startTime}
                 ) t
            WHERE t.rn = 1
            """)
    List<TaiNengStationFeeCfgData> getTaiNengStationFeeCfgData(@Param("startTime") LocalDateTime startTime,
                                                               @Param("endTime") LocalDateTime endTime);
}
