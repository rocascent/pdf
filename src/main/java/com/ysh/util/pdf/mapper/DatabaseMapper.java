package com.ysh.util.pdf.mapper;

import com.ysh.util.pdf.dto.EntIncomeData;
import com.ysh.util.pdf.dto.StationEntIncomeData;
import com.ysh.util.pdf.dto.TenantClrData;
import com.ysh.util.pdf.dto.TenantInformation;
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
            GROUP BY pt.STATIONID;
            """)
    List<StationEntIncomeData> getStationEntIncomeData(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
