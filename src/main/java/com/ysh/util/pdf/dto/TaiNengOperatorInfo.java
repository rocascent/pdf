package com.ysh.util.pdf.dto;

/**
 * 泰能充结算单运营商信息（来自 TBTENANT_OPERATOR，第 1 页表 1）。
 */
public record TaiNengOperatorInfo(
        String operatorName,   // 运营商公司全称（LONGNAME，缺省回退 NAME）
        String creditCode,     // 统一社会信用代码
        String address,        // 企业地址
        String contact,        // 联系人
        String phone           // 联系电话
) {
}
