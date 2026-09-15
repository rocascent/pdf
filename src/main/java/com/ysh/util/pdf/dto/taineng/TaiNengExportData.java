package com.ysh.util.pdf.dto.taineng;

import java.util.List;

/** 泰能充平台月度结算单导出数据（根 DTO）。 */
public class TaiNengExportData {
    public TaiNengPage1Data page1;           // 第 1 页：运营商信息 + 合计信息
    public List<TaiNengStationData> stations; // 充电站清单，每站点 1 页
}