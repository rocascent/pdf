package com.ysh.util.pdf.dto.reconciliation;

import java.util.List;

/**
 * 结算单导出数据（根 DTO）。
 */
public class ExportData {
    public Page1Data page1;
    public Page2Data page2;
    public List<Page3Data> page3;
    public Page4Data page4;
}