package com.ysh.util.pdf.dto;

/** 第 4 页表 2：一个场站的消费明细组（场站名纵跨 3 个机构类型子行，合并单元格）。 */
public class StationGroup {
    public String station;        // 场站名称 / 编号

    /** 以下每列数组按子行顺序取值（普通机构/运营商机构/泰达电力机构），不足补空。 */
    public String[] elecFee;      // 电费收入
    public String[] serviceFee;   // 服务费收入
    public String[] consume;      // 充电消费收入
    public String[] parkFee;      // 占位费收入
    public String[] total;        // 消费收入合计
}
