package com.ysh.util.pdf.dto.reconciliation;

/**
 * Page2 Table1 (一、场站线上清分收入)
 * Columns (order-based): station / electricity / service / parking / total
 * Row = one station (last row = total)
 */
public class Page2Table1Row {

    public String station;      // 场站名称 / 编号
    public String electricity;  // 本方电费收入
    public String service;      // 本方服务费收入
    public String parking;      // 本方占位费收入
    public String total;        // 本方清分收入合计

    public Page2Table1Row() {}

    public Page2Table1Row(String station, String electricity, String service, String parking, String total) {
        this.station = station;
        this.electricity = electricity;
        this.service = service;
        this.parking = parking;
        this.total = total;
    }
}