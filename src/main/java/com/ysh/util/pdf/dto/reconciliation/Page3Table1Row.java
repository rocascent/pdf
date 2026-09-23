package com.ysh.util.pdf.dto.reconciliation;

/**
 * Page3 Table1 (一、线上可分配净额计算)
 * Columns (order-based): elecIncome / serviceIncome / parkIncome / totalIncome
 * 固定 6 行，顺序即语义（label 写死在 Page3Drawer）：
 *   费用金额 / 减：优惠金额 / 减：支付通道费 / 减：平台服务费 / 加：调整额 / 可分配净额
 */
public class Page3Table1Row {

    public String elecIncome;     // 电费收入
    public String serviceIncome;  // 服务费收入
    public String parkIncome;     // 占位费收入
    public String totalIncome;    // 合计

    public Page3Table1Row() {}

    public Page3Table1Row(String elecIncome, String serviceIncome, String parkIncome, String totalIncome) {
        this.elecIncome = elecIncome;
        this.serviceIncome = serviceIncome;
        this.parkIncome = parkIncome;
        this.totalIncome = totalIncome;
    }
}
