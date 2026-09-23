package com.ysh.util.pdf.dto.settlement;

/** 泰能充结算单第 1 页（运营商信息 + 合计信息）数据。 */
public class Page1Data {
    // 大标题与导出时间（下划线填空位）
    public String operatorName;      // 运营商名称（标题"____运营商" + 表1公司全称）
    public String settleYear;        // 结算年份（标题"____年"）
    public String settleMonth;       // 结算月份（标题"____月"）
    public String exportYear;        // 导出时间-年
    public String exportMonth;       // 导出时间-月
    public String exportDay;         // 导出时间-日
    public String exportHour;        // 导出时间-时
    public String exportMinute;      // 导出时间-分

    // 表1 运营商信息
    public String creditCode;        // 统一社会信用代码
    public String bank;              // 开户行
    public String account;           // 账号
    public String address;           // 企业地址
    public String contact;           // 联系人

    // 表2 合计信息
    public String totalCharge;       // 总充电量（kWh）
    public String totalElecFee;      // 总充电电费（元）
    public String totalServFee;      // 总充电服务费（元）
    public String normalCharge;      // 普通机构充电量（kWh）
    public String normalElecFee;     // 普通机构充电电费（元）
    public String normalServFee;     // 普通机构充电服务费（元）
    public String operatorCharge;    // 运营商机构充电量（kWh）
    public String operatorElecFee;   // 运营商机构充电电费（元）
    public String operatorServFee;   // 运营商机构充电服务费（元）
    public String tedaCharge;        // 泰达电力机构充电量（kWh）
    public String tedaElecFee;       // 泰达电力机构充电电费（元）
    public String tedaServFee;       // 泰达电力机构充电服务费（元）
    public String wechatPayTotal;    // 微信付款合计（元）
    public String wechatFee;         // 微信手续费（元）
    public String alipayPayTotal;    // 支付宝付款合计（元）
    public String alipayFee;         // 支付宝手续费（元）
    public String platformFee;       // 平台手续费（元）
    public String totalWithdrawable; // 总计可提现费用（元）
}