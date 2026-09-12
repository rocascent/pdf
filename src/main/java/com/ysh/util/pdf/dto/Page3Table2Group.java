package com.ysh.util.pdf.dto;

/** 第 3 页表 2"各方清分收入"：一个费用项目组（N 个清分方子行，渲染不足 2 行补空行）。 */
public class Page3Table2Group {
    public String item;          // 费用项目：电费/服务费/占位费
    public String netAmount;     // 可分配净额
    public String[] party;       // 清分方名称（数组长度 = 子行数，可为 null）
    public String[] ratio;       // 清分比例
    public String[] income;      // 清分收入
}
