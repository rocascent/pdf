package com.ysh.util.pdf.dto.settlement;

/** 泰能充结算单单个充电站结算数据（每站点占 1 页）。 */
public class StationData {
    public String stationName;       // 电站名称
    public String stationAddress;    // 电站地址
    public String stationType;       // 电站类型
    public String feeMode;           // 平台手续费模式
    public String fixedFee;          // 固定手续费（元，固定金额模式）
    public String feeRate;           // 平台手续费费率（%，费率模式）
    public String monthCharge;       // 月充电量（kWh）
    public String monthElecFee;      // 月充电电费（元）
    public String monthServFee;      // 月充电服务费（元）
    public String normalCharge;      // 普通机构充电量（kWh）
    public String normalElecFee;     // 普通机构充电电费（元）
    public String normalServFee;     // 普通机构充电服务费（元）
    public String operatorCharge;    // 运营商机构充电量（kWh）
    public String operatorElecFee;   // 运营商机构充电电费（元）
    public String operatorServFee;   // 运营商机构充电服务费（元）
    public String tedaCharge;        // 泰达电力机构充电量（kWh）
    public String tedaElecFee;       // 泰达电力机构充电电费（元）
    public String tedaServFee;       // 泰达电力机构充电服务费（元）
    public String platformFee;       // 平台手续费（元）
    public String withdrawable;      // 可提现费用（元）
}