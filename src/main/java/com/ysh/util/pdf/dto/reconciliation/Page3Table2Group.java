package com.ysh.util.pdf.dto.reconciliation;

/**
 * 第 3 页表 2"各方清分收入"：一个费用项目组（N 个清分方子行，渲染时至少 1 行）。
 */
public record Page3Table2Group(
        String item,          // 费用项目：电费/服务费/占位费
        String clrAmount,     // 可分配净额
        String[] party,       // 清分方名称（数组长度 = 子行数，可为 null）
        String[] ratio,       // 清分比例
        String[] income      // 清分收入
) {
}
