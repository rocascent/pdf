package com.ysh.util.pdf.dto;

/**
 * Page2 Table2: Station distribution total verification
 * Columns: station / onlineNet / operatorIncome / otherIncome
 */
public class Page2Table2Row {

    public String station;          // Station name / code
    public String onlineNet;        // Online distributable net amount
    public String operatorIncome;   // Operator income
    public String otherIncome;      // Other party income

    public Page2Table2Row() {
    }

    public Page2Table2Row(String station, String onlineNet, String operatorIncome, String otherIncome) {
        this.station = station;
        this.onlineNet = onlineNet;
        this.operatorIncome = operatorIncome;
        this.otherIncome = otherIncome;
    }
}