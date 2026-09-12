package com.ysh.util.pdf.dto;

import java.util.List;

/** 第 4 页：机构消费汇总表 + 各场站明细表。 */
public class Page4Data implements PageData {
    public String operatorName = DEFAULT_STRING;  // 运营商名称（抬头）
    public String period = DEFAULT_STRING;        // 结算期间（抬头）

    /** 表 1：三类机构消费汇总（固定 4 行，顺序即语义） */
    public List<Page4Table1Row> table1;

    /** 表 2：各场站机构消费明细 */
    public List<Page4Table2Group> table2;

    /** 表 2 合计行 */
    public String[] table2Total;
}
