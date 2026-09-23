package com.ysh.util.pdf.dto.reconciliation;

/** 各页数据的公共接口：只提供字段默认值常量（传什么打印什么，不做自动计算）。 */
public interface PageData {

    /** 字段默认值：文本空串 / 金额 "0.00"。 */
    String DEFAULT_STRING = "";
    String DEFAULT_DOUBLE = "0.00";
}
