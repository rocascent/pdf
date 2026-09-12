package com.ysh.util.pdf.dto;

import java.util.List;

/** 第 3 页：场站抬头 + 核算项目表 + 各方清分表 + 底部汇总。 */
public class Page3Data implements PageData {
    public String operatorName = DEFAULT_STRING;  // 运营商名称（抬头）
    public String period = DEFAULT_STRING;        // 结算期间（抬头）

    public String stationName = DEFAULT_STRING;    // 场站名称
    public String stationCode = DEFAULT_STRING;    // 场站编号
    public String chargeRule = DEFAULT_STRING;     // 占位计费规则

    public List<Page3Table1Row> table1;  // 一、线上可分配净额计算（固定 6 行，顺序即语义：电费/服务费/占位费/合计）
    public List<Page3Table2Group> feeGroups;  // 二、各方清分收入（每个费用项目一组，子行数 = 清分方个数，至少 1 行）

    public String operatorIncome = DEFAULT_STRING; // 本运营商线上清分收入
    public String totalIncome = DEFAULT_STRING;    // 各方线上清分收入合计
}
