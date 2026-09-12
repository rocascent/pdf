package com.ysh.util.pdf.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.ysh.util.pdf.dto.*;
import com.ysh.util.pdf.util.ChineseAmountUtil;
import org.springframework.stereotype.Service;
import com.ysh.util.pdf.mapper.DatabaseMapper;
import io.azam.ulidj.ULID;

@Service
public class ChargingDataService {
    private final DatabaseMapper databaseMapper;

    public ChargingDataService(DatabaseMapper databaseMapper) {
        this.databaseMapper = databaseMapper;
    }

    public ExportData getChargingData(String tenantId, LocalDate startTime, LocalDate endTime) {
        var data = new ExportData();
        getPage1Data(data, tenantId, startTime, endTime);
        getPage2Data(data, tenantId, startTime, endTime);
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
        p2.table1 = List.of(new Page2Table1Row("111", "222", "3", "4", "5"));
        data.page2 = p2;
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
            group.consume = new String[]{};
            group.tmoutFee = new String[]{};
            group.total = new String[]{};
            return group;
        }).toList();

        data.page4 = p4;
    }
}
