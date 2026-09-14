package com.ysh.util.pdf.mapper;

import com.ysh.util.pdf.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DatabaseMapper {
    @Select("""
            SELECT "tb".TENANTOPERATORID AS tenantOperatorId, "tb".TENANTOPERATORNAME AS tenantOperatorName, "tb".TENANTOPERATORLONGNAME AS tenantOperatorLongName,
                   "td".BANKACCNAME AS bankAccName, "td".BANKLONGNAME AS bankLongName, "td".BANKCARDNO AS bankCardNo
            FROM TBTENANT_OPERATOR AS tb
            LEFT JOIN TBTENANT_DISTRCFG AS td ON tb.TENANTOPERATORID = td.OPERATORID
            WHERE "tb".TENANTOPERATORID = #{tenantId};
            """)
    TenantInformation getTenantInformation(String tenantId);

    @Select("""
            SELECT
              SUM(TOTALELECMONEY) AS totalElecMoney,
              SUM(TOTALSEVICEMONEY) AS totalSeviceMoney,
              SUM(TOTALTIMEOUTMONEY) AS totalTimeoutMoney,
              SUM(DISTRELECFEE) AS distrElecFee,
              SUM(DISTRSERVFEE) AS distrServFee,
              SUM(DISTRTIMEOUTFEE) AS distrTimeoutFee
            FROM
              TBEVI_TRADE_DISTR_SUM AS ttds
            WHERE
              ttds.OPERATORID = #{tenantId}
              AND ttds.STATTIME >= #{startTime} AND ttds.STATTIME < #{endTime};
            """)
    TenantClrData getTenantClrData(@Param("tenantId") String tenantId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT
              CONCAT(ts.STATIONNAME, ' / ', ts.STATIONID) AS name,
              SUM(DISTRELECFEE) AS clrElecFee,
              SUM(DISTRSERVFEE) AS clrServFee,
              SUM(DISTRTIMEOUTFEE) AS clrTmoutFee
            FROM
              TBEVI_TRADE_DISTR AS ttd
              LEFT JOIN TBEVI_TRADE AS tt ON ttd.ORDERNO = tt.ORDERNO
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
            SELECT
                CONCAT(s.STATIONNAME, ' / ', s.STATIONID) AS name,
                SUM(IF(td.SVROPERID = #{tenantId}, td.DISTRELECFEE + td.DISTRSERVFEE + td.DISTRTIMEOUTFEE, 0)) AS selfFee,
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
            SELECT
                NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALELECMONEY END), 0) AS normalElecMoney,
                NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALSEVICEMONEY END), 0) AS normalServMoney,
                NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALTMOUTMONEY END), 0) AS normalTmoutMoney,
                NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALPOWER END), 0) AS normalPower,
                COUNT(CASE WHEN e.TYPE = 0 THEN 1 END) AS normalCount,
                NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALELECMONEY END), 0) AS operatorElecMoney,
                NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALSEVICEMONEY END), 0) AS operatorServMoney,
                NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALTMOUTMONEY END), 0) AS operatorTmoutMoney,
                NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALPOWER END), 0) AS operatorPower,
                COUNT(CASE WHEN e.TYPE = 1 THEN 1 END) AS operatorCount,
                NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALELECMONEY END), 0) AS tedaElecMoney,
                NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALSEVICEMONEY END), 0) AS tedaServMoney,
                NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALTMOUTMONEY END), 0) AS tedaTmoutMoney,
                NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALPOWER END), 0) AS tedaPower,
                COUNT(CASE WHEN e.TYPE = 2 THEN 1 END) AS tedaCount
            FROM (SELECT * FROM PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE
                           WHERE PUSHTIMESTAMP >= #{startTime} AND PUSHTIMESTAMP < #{endTime}) pt
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBUSER u ON pt.USERNUM = u.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_USER_RELA vur ON u.USERNUM = vur.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_ENT e ON vur.VIPGRPID = e.ENTID
            """)
    EntIncomeData getEntIncomeData(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT
                CONCAT(s.STATIONNAME,' / ', s.STATIONID) AS name,
                NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALELECMONEY END), 0) AS normalElecMoney,
                NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALSEVICEMONEY END), 0) AS normalServMoney,
                NVL(SUM(CASE WHEN e.TYPE = 0 THEN pt.TOTALTMOUTMONEY END), 0) AS normalTmoutMoney,
                NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALELECMONEY END), 0) AS operatorElecMoney,
                NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALSEVICEMONEY END), 0) AS operatorServMoney,
                NVL(SUM(CASE WHEN e.TYPE = 1 THEN pt.TOTALTMOUTMONEY END), 0) AS operatorTmoutMoney,
                NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALELECMONEY END), 0) AS tedaElecMoney,
                NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALSEVICEMONEY END), 0) AS tedaServMoney,
                NVL(SUM(CASE WHEN e.TYPE = 2 THEN pt.TOTALTMOUTMONEY END), 0) AS tedaTmoutMoney
            FROM (SELECT * FROM PWR_ORDERLY_MNGR_TEDA.TBPUB_TRADE
                           WHERE PUSHTIMESTAMP >= #{startTime} AND PUSHTIMESTAMP < #{endTime}) pt
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBUSER u ON pt.USERNUM = u.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_USER_RELA vur ON u.USERNUM = vur.USERNUM
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_VIPGRP_ENT e ON vur.VIPGRPID = e.ENTID
                     LEFT JOIN PWR_ORDERLY_MNGR_TEDA.TBPUB_STATION s on pt.STATIONID = s.STATIONID
            WHERE e.TYPE IN (0, 1, 2)
            GROUP BY pt.STATIONID;
            """)
    List<StationEntIncomeData> getStationEntIncomeData(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
