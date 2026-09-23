package com.ysh.util.pdf.dto.reconciliation;

/**
 * Page4 Table1：三类机构消费汇总（固定 4 行）
 * 顺序固定：普通机构 / 运营商机构 / 泰达电力机构 / 合计
 *
 * 构造函数参数顺序：orderCount, chargeQty, chargeIncome, parkIncome, totalIncome
 */
public class Page4Table1Row {

    public String orderCount;      // 订单数（笔）
    public String chargeQty;       // 充电量（kWh）
    public String chargeIncome;    // 充电消费收入
    public String parkIncome;      // 占位费收入
    public String totalIncome;     // 消费收入合计

    public Page4Table1Row(String orderCount,
                          String chargeQty,
                          String chargeIncome,
                          String parkIncome,
                          String totalIncome) {
        this.orderCount = orderCount;
        this.chargeQty = chargeQty;
        this.chargeIncome = chargeIncome;
        this.parkIncome = parkIncome;
        this.totalIncome = totalIncome;
    }
}
