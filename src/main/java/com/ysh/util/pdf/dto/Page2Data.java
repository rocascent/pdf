package com.ysh.util.pdf.dto;

import java.util.List;

public class Page2Data implements PageData {

    public String operatorName = DEFAULT_STRING;
    public String period = DEFAULT_STRING;

    /** Table1: fixed 6 rows (electricity / service / parking / total) */
    public List<Page2Table1Row> table1;

    /** Table2: station distribution verification */
    public List<Page2Table2Row> table2;
}