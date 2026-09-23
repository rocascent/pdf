package com.ysh.util.pdf.dto.reconciliation;

/** 第 4 页表 2：一个场站的消费明细组（场站名纵跨 3 个机构类型子行，合并单元格）。 */
public class Page4Table2Group {
    public String station;        // 场站名称 / 编号

    /** 以下每列数组按子行顺序取值（普通机构/运营商机构/泰达电力机构），不足补空。 */
    public String[] elecFee;      // 电费收入
    public String[] servFee;   // 服务费收入
    public String[] consume;      // 充电消费收入
    public String[] tmoutFee;      // 占位费收入
    public String[] total;        // 消费收入合计
}
