package com.ysh.util.pdf;

import com.ysh.util.pdf.dto.taineng.TaiNengExportData;
import com.ysh.util.pdf.dto.taineng.TaiNengPage1Data;
import com.ysh.util.pdf.dto.taineng.TaiNengStationData;
import com.ysh.util.pdf.exporter.taineng.TaiNengPdfExporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 泰能充演示入口：按页拼装 Java 对象，生成 out.pdf。
 */
public class TaiNengDemo {

    public static void main(String[] args) throws Exception {
        TaiNengExportData d = new TaiNengExportData();

        // ---- 第 1 页：运营商信息 + 合计信息 ----
        TaiNengPage1Data p1 = new TaiNengPage1Data();
        p1.operatorName = "泰达新能源科技有限公司";
        p1.settleYear = "2026";
        p1.settleMonth = "09";
        p1.exportYear = "2026";
        p1.exportMonth = "09";
        p1.exportDay = "15";
        p1.exportHour = "10";
        p1.exportMinute = "30";
        p1.creditCode = "91120116MA05K1234X";
        p1.address = "天津市滨海新区泰达大街九十九号泰达新能源大厦十八楼一八零一号室";
        p1.contact = "王五";
        p1.phone = "022-88888888";
        p1.totalCharge = "12000.50";
        p1.totalElecFee = "8000.30";
        p1.totalServFee = "4000.20";
        p1.normalCharge = "2000.00";
        p1.normalElecFee = "1300.00";
        p1.normalServFee = "700.00";
        p1.operatorCharge = "3000.00";
        p1.operatorElecFee = "2000.00";
        p1.operatorServFee = "1000.00";
        p1.tedaCharge = "1000.00";
        p1.tedaElecFee = "700.00";
        p1.tedaServFee = "300.00";
        p1.wechatPayTotal = "6000.00";
        p1.wechatFee = "36.00";
        p1.alipayPayTotal = "3000.00";
        p1.alipayFee = "18.00";
        p1.platformFee = "500.00";
        p1.totalWithdrawable = "10446.50";
        d.page1 = p1;

        // ---- 站点页：每站点 1 页 ----
        TaiNengStationData s1 = new TaiNengStationData();
        s1.stationName = "泰达一号充电站";
        s1.stationAddress = "天津市滨海新区第一大街一号泰达国际物流园区C栋一层东侧";
        s1.stationType = "普通";
        s1.feeMode = "固定金额";
        s1.fixedFee = "300.00";
        s1.monthCharge = "7000.00";
        s1.monthElecFee = "4600.00";
        s1.monthServFee = "2400.00";
        s1.normalCharge = "1200.00";
        s1.normalElecFee = "800.00";
        s1.normalServFee = "400.00";
        s1.operatorCharge = "1800.00";
        s1.operatorElecFee = "1200.00";
        s1.operatorServFee = "600.00";
        s1.tedaCharge = "600.00";
        s1.tedaElecFee = "400.00";
        s1.tedaServFee = "200.00";
        s1.wechatPayTotal = "4000.00";
        s1.wechatFeeRate = "0.6%";
        s1.wechatFee = "24.00";
        s1.alipayPayTotal = "2000.00";
        s1.alipayFeeRate = "0.6%";
        s1.alipayFee = "12.00";
        s1.platformFee = "300.00";
        s1.withdrawable = "6664.00";

        TaiNengStationData s2 = new TaiNengStationData();
        s2.stationName = "泰达二号充电站";
        s2.stationAddress = "天津市滨海新区经济技术开发区第九大街九十九号泰达新能源产业园综合服务中心大楼十八层一八零一号房间东侧充电站管理中心办公区西侧地下停车场B区第三排充电桩区域旁边的小型休息室内东北角综合缴费服务台旁";
        s2.stationType = "运营商";
        s2.feeMode = "费率";
        s2.feeRate = "0.8%";
        s2.monthCharge = "5000.00";
        s2.monthElecFee = "3400.00";
        s2.monthServFee = "1600.00";
        s2.normalCharge = "800.00";
        s2.normalElecFee = "500.00";
        s2.normalServFee = "300.00";
        s2.operatorCharge = "2200.00";
        s2.operatorElecFee = "1500.00";
        s2.operatorServFee = "700.00";
        s2.tedaCharge = "400.00";
        s2.tedaElecFee = "300.00";
        s2.tedaServFee = "100.00";
        s2.wechatPayTotal = "3000.00";
        s2.wechatFeeRate = "0.6%";
        s2.wechatFee = "18.00";
        s2.alipayPayTotal = "1000.00";
        s2.alipayFeeRate = "0.6%";
        s2.alipayFee = "6.00";
        s2.platformFee = "200.00";
        s2.withdrawable = "4776.00";

        // s3：部分字段不赋值，测试留空显示
        TaiNengStationData s3 = new TaiNengStationData();
        s3.stationName = "泰达三号充电站";
        s3.stationAddress = "天津市滨海新区第三大街三号泰达广场11111111112222222222";
        // stationType、feeMode、fixedFee、feeRate 均不赋值 → 应留空
        s3.monthCharge = "3000.00";
        s3.monthElecFee = "2000.00";
        s3.monthServFee = "1000.00";
        // normal/operator/teda 系列、微信/支付宝、平台手续费、可提现费用均不赋值 → 应留空
        d.stations = List.of(s1, s2, s3);

        byte[] pdf = TaiNengPdfExporter.export(d);
        Files.write(Path.of("out.pdf"), pdf);
        System.out.println("生成完成: out.pdf (" + pdf.length + " bytes)");
    }
}
