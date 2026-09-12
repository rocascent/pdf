package com.ysh.util.pdf.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 一行数据：label + 最多 6 个数据列。
 */
public class Row {
    public String label;
    public String col1, col2, col3, col4, col5, col6;

    // 存在多个构造器时，Jackson 3 不再默认选用无参构造器，需显式标记
    @JsonCreator
    public Row() {
    }

    public Row(String label, String... cols) {
        this.label = label;
        String[] v = {null, null, null, null, null, null};
        for (int i = 0; i < cols.length && i < 6; i++) {
            v[i] = cols[i];
        }
        this.col1 = v[0];
        this.col2 = v[1];
        this.col3 = v[2];
        this.col4 = v[3];
        this.col5 = v[4];
        this.col6 = v[5];
    }
}
