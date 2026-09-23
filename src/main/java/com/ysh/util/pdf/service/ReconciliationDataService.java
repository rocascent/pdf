package com.ysh.util.pdf.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ysh.util.pdf.dto.*;
import com.ysh.util.pdf.dto.reconciliation.*;
import com.ysh.util.pdf.util.ChineseAmountUtil;
import org.springframework.stereotype.Service;
import com.ysh.util.pdf.mapper.DatabaseMapper;
import io.azam.ulidj.ULID;

@Service
public class ReconciliationDataService {
    private final DatabaseMapper databaseMapper;

    public ReconciliationDataService(DatabaseMapper databaseMapper) {
        this.databaseMapper = databaseMapper;
    }

    public ExportData getChargingData(String tenantId, LocalDate startTime, LocalDate endTime) {
        var data = new ExportData();
        getPage1Data(data, tenantId, startTime, endTime);
        getPage2Data(data, tenantId, startTime, endTime);
        getPage3Data(data, tenantId, startTime, endTime);
        getPage4Data(data, tenantId, startTime, endTime);
        return data;
    }

    private void getPage1Data(ExportData data, String tenantId, LocalDate startTime, LocalDate endTime) {
        var p1 = new Page1Data();
        var tenantInformation = databaseMapper.getTenantInformation(tenantId);
        p1.settlementNo = ULID.random();
        p1.period = startTime.format(DateTimeFormatter.ISO_DATE)
                + '至'
                + endTime.minusDays(1).format(DateTimeFormatter.ISO_DATE);
        p1.operatorName = tenantInformation.tenantOperatorName();
        p1.operatorCode = tenantInformation.tenantOperatorId();
        p1.settlerName = tenantInformation.tenantOperatorLongName();
        p1.issueDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
        p1.accountName = tenantInformation.bankAccName();
        p1.bankName = tenantInformation.bankLongName();
        p1.accountNumber = tenantInformation.bankCardNo();

        var clrData = databaseMapper.getTenantClrData(tenantId, startTime.atStartOfDay(), endTime.atStartOfDay());
        p1.totalElecMoney = clrData.totalElecMoney().toPlainString();
        p1.clrElecMoney = clrData.distrElecFee().toPlainString();
        p1.totalServMoney = clrData.totalSeviceMoney().toPlainString();
        p1.clrServMoney = clrData.distrServFee().toPlainString();
        p1.totalTmoutMoney = clrData.totalTimeoutMoney().toPlainString();
        p1.clrTmoutMoney = clrData.distrTimeoutFee().toPlainString();
        var totalMoney = clrData.totalElecMoney()
                .add(clrData.totalSeviceMoney())
                .add(clrData.totalTimeoutMoney());
        p1.totalMoney = totalMoney.toPlainString();
        p1.clrMoney = clrData.distrElecFee()
                .add(clrData.distrServFee())
                .add(clrData.distrTimeoutFee())
                .toPlainString();
        p1.amountWords = ChineseAmountUtil.convert(totalMoney);
        var entData = databaseMapper.getEntIncomeData(startTime.atStartOfDay(), endTime.atStartOfDay());
        p1.normalCharge = entData.normalCharge().toPlainString();
        p1.normalOccupy = entData.normalTmoutMoney().toPlainString();
        p1.normalTotal = entData.normalTotal().toPlainString();
        p1.tedaCharge = entData.tedaCharge().toPlainString();
        p1.tedaOccupy = entData.tedaTmoutMoney().toPlainString();
        p1.tedaTotal = entData.tedaTotal().toPlainString();
        p1.operatorCharge = entData.operatorCharge().toPlainString();
        p1.operatorOccupy = entData.operatorTmoutMoney().toPlainString();
        p1.operatorTotal = entData.operatorTotal().toPlainString();
        p1.totalOrgCharge = entData.normalCharge()
                .add(entData.tedaCharge())
                .add(entData.operatorCharge())
                .toPlainString();
        p1.totalOrgOccupy = entData.normalTmoutMoney()
                .add(entData.tedaTmoutMoney())
                .add(entData.operatorTmoutMoney())
                .toPlainString();
        p1.totalOrgIncome = entData.normalTotal()
                .add(entData.tedaTotal())
                .add(entData.operatorTotal())
                .toPlainString();
        data.page1 = p1;
    }

    private void getPage2Data(ExportData data, String tenantId, LocalDate startTime, LocalDate endTime) {
        var p2 = new Page2Data();
        p2.operatorName = data.page1.operatorName;
        p2.period = data.page1.period;
        var stationClrData = databaseMapper.getStationClrData(tenantId, startTime, endTime);
        p2.table1 = new ArrayList<>(stationClrData.size() + 1);
        if (!stationClrData.isEmpty()) {
            var total = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
            stationClrData.forEach(d -> {
                var row = new Page2Table1Row(
                        d.name(), d.clrElecFee().toPlainString(), d.clrServFee().toPlainString(), d.clrTmoutFee().toPlainString(),
                        d.clrElecFee().add(d.clrServFee()).add(d.clrTmoutFee()).toPlainString()
                );
                total[0] = total[0].add(d.clrElecFee());
                total[1] = total[1].add(d.clrServFee());
                total[2] = total[2].add(d.clrTmoutFee());
                total[3] = total[3].add(d.clrElecFee().add(d.clrServFee()).add(d.clrTmoutFee()));
                p2.table1.add(row);
            });
            p2.table1.add(new Page2Table1Row(
                    "合计",
                    total[0].toPlainString(),
                    total[1].toPlainString(),
                    total[2].toPlainString(),
                    total[3].toPlainString()
            ));
        }
        var totalClrData = databaseMapper.getStationTotalClrData(tenantId, startTime, endTime);
        p2.table2 = new ArrayList<>(totalClrData.size() + 1);
        if (!totalClrData.isEmpty()) {
            var total = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
            totalClrData.forEach(d -> {
                var row = new Page2Table2Row(
                        d.name(),
                        d.totalFee().toPlainString(),
                        d.selfFee().toPlainString(),
                        d.otherFee().toPlainString()
                );
                total[0] = total[0].add(d.totalFee());
                total[1] = total[1].add(d.selfFee());
                total[2] = total[2].add(d.otherFee());
                p2.table2.add(row);
            });
            p2.table2.add(new Page2Table2Row(
                    "合计",
                    total[0].toPlainString(),
                    total[1].toPlainString(),
                    total[2].toPlainString()
            ));
        }
        data.page2 = p2;
    }

    private void getPage3Data(ExportData data, String tenantId, LocalDate startTime, LocalDate endTime) {
        var stationDetailData = databaseMapper.getStationDetailData(startTime, endTime);
        var stationClrDetailData = databaseMapper.getStationClrDetailData(startTime, endTime).stream()
                .collect(Collectors.groupingBy(
                        StationClrDetailData::stationId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        var stationClrTotalData = databaseMapper.getStationTotalClrData(tenantId, startTime, endTime).stream()
                .collect(Collectors.toMap(StationTotalClrData::id, x -> x));
        data.page3 = stationDetailData.stream().map(d -> {
            var p3 = new Page3Data();
            p3.operatorName = data.page1.operatorName;
            p3.period = data.page1.period;
            p3.stationCode = d.id();
            p3.stationName = d.name();
            p3.chargeRule = d.parkFee();
            p3.table1 = List.of(
                    new Page3Table1Row(
                            d.origElecFee().toPlainString(),
                            d.origServFee().toPlainString(),
                            d.origTmoutFee().toPlainString(),
                            d.origElecFee().add(d.origServFee()).add(d.origTmoutFee()).toPlainString()
                    ),
                    new Page3Table1Row(
                            d.dcElecFee().toPlainString(),
                            d.dcServeFee().toPlainString(),
                            d.dcTmoutFee().toPlainString(),
                            d.dcElecFee().add(d.dcServeFee()).add(d.dcTmoutFee()).toPlainString()
                    ),
                    new Page3Table1Row(
                            d.handlElecFee().toPlainString(),
                            d.handlServFee().toPlainString(),
                            d.handlTmoutFee().toPlainString(),
                            d.handlElecFee().add(d.handlServFee()).add(d.handlTmoutFee()).toPlainString()
                    ),
                    new Page3Table1Row(),
                    new Page3Table1Row(),
                    new Page3Table1Row(
                            d.elecFee().toPlainString(),
                            d.servFee().toPlainString(),
                            d.tmoutFee().toPlainString(),
                            d.elecFee().add(d.servFee()).add(d.tmoutFee()).toPlainString()
                    )
            );
            var clrDetails = stationClrDetailData.getOrDefault(d.id(), List.of());
            p3.table2 = List.of(
                    clrGroup("电费", clrDetails, StationClrDetailData::elecRate, StationClrDetailData::elecFee),
                    clrGroup("服务费", clrDetails, StationClrDetailData::servRate, StationClrDetailData::servFee),
                    clrGroup("占位费", clrDetails, StationClrDetailData::tmoutRate, StationClrDetailData::tmoutFee)
            );
            var total = stationClrTotalData.getOrDefault(d.id(), new StationTotalClrData("", "", BigDecimal.ZERO, BigDecimal.ZERO));
            p3.operatorIncome = total.selfFee().toPlainString();
            p3.totalIncome = total.selfFee().add(total.otherFee()).toPlainString();

            return p3;
        }).toList();
    }

    /**
     * 一个费用项目的清分组：可分配净额 = 该站各清分方收入之和；每个清分方一行（名称/比例/收入）。
     */
    private Page3Table2Group clrGroup(String item, List<StationClrDetailData> details,
                                      Function<StationClrDetailData, BigDecimal> rate,
                                      Function<StationClrDetailData, BigDecimal> income) {
        int n = details.size();
        var partyArr = new String[n];
        var ratioArr = new String[n];
        var incomeArr = new String[n];
        var clrAmount = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            var d = details.get(i);
            partyArr[i] = d.operatorName();
            ratioArr[i] = rate.apply(d).toPlainString();
            incomeArr[i] = income.apply(d).toPlainString();
            clrAmount = clrAmount.add(income.apply(d));
        }
        return new Page3Table2Group(item, clrAmount.toPlainString(), partyArr, ratioArr, incomeArr);
    }

    private void getPage4Data(ExportData data, String tenantId, LocalDate startTime, LocalDate endTime) {
        var p4 = new Page4Data();
        p4.operatorName = data.page1.operatorName;
        p4.period = data.page1.period;
        var entData = databaseMapper.getEntIncomeData(startTime.atStartOfDay(), endTime.atStartOfDay());
        p4.table1 = List.of(
                new Page4Table1Row(entData.normalCount().toString(), entData.normalPower().toPlainString(),
                        entData.normalCharge().toPlainString(), entData.normalTmoutMoney().toPlainString(),
                        entData.normalTotal().toPlainString()),
                new Page4Table1Row(entData.operatorCount().toString(), entData.operatorPower().toPlainString(),
                        entData.operatorCharge().toPlainString(), entData.operatorTmoutMoney().toPlainString(),
                        entData.operatorTotal().toPlainString()),
                new Page4Table1Row(entData.tedaCount().toString(), entData.tedaPower().toPlainString(),
                        entData.tedaCharge().toPlainString(), entData.tedaTmoutMoney().toPlainString(),
                        entData.tedaTotal().toPlainString()),
                new Page4Table1Row(String.valueOf(entData.normalCount() + entData.operatorCount() + entData.tedaCount()),
                        entData.normalPower().add(entData.operatorPower()).add(entData.tedaPower()).toPlainString(),
                        entData.normalCharge().add(entData.operatorCharge()).add(entData.tedaCharge()).toPlainString(),
                        entData.normalTmoutMoney().add(entData.operatorTmoutMoney()).add(entData.tedaTmoutMoney()).toPlainString(),
                        entData.normalTotal().add(entData.operatorTotal()).add(entData.tedaTotal()).toPlainString())
        );
        var stationData = databaseMapper.getStationEntIncomeData(startTime.atStartOfDay(), endTime.atStartOfDay());
        var total = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        p4.table2 = stationData.stream().map(d -> {
            var group = new Page4Table2Group();
            group.station = d.name();
            group.elecFee = new String[]{
                    d.normalElecMoney().toPlainString(),
                    d.operatorElecMoney().toPlainString(),
                    d.tedaElecMoney().toPlainString(),
            };
            group.servFee = new String[]{
                    d.normalServMoney().toPlainString(),
                    d.operatorServMoney().toPlainString(),
                    d.tedaServMoney().toPlainString(),
            };
            group.consume = new String[]{
                    d.normalCharge().toPlainString(),
                    d.operatorCharge().toPlainString(),
                    d.tedaCharge().toPlainString(),
            };
            group.tmoutFee = new String[]{
                    d.normalTmoutMoney().toPlainString(),
                    d.operatorTmoutMoney().toPlainString(),
                    d.tedaTmoutMoney().toPlainString(),
            };
            group.total = new String[]{
                    d.normalTotal().toPlainString(),
                    d.operatorTotal().toPlainString(),
                    d.tedaTotal().toPlainString(),
            };
            total[0] = total[0].add(d.normalElecMoney()).add(d.operatorElecMoney()).add(d.tedaElecMoney());
            total[1] = total[1].add(d.normalServMoney()).add(d.operatorServMoney()).add(d.tedaServMoney());
            total[2] = total[2].add(d.normalCharge()).add(d.operatorCharge()).add(d.tedaCharge());
            total[3] = total[3].add(d.normalTmoutMoney()).add(d.operatorTmoutMoney()).add(d.tedaTmoutMoney());
            total[4] = total[4].add(d.normalTotal()).add(d.operatorTotal()).add(d.tedaTotal());
            return group;
        }).toList();
        p4.table2Total = new String[]{
                total[0].toPlainString(),
                total[1].toPlainString(),
                total[2].toPlainString(),
                total[3].toPlainString(),
                total[4].toPlainString()
        };
        data.page4 = p4;
    }
}
