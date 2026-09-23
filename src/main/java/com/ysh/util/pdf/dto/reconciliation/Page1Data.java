package com.ysh.util.pdf.dto.reconciliation;

/** 第 1 页：抬头 + 费用表 + 机构用户类型表（固定表，全部平铺字段，自带初始值，传什么打印什么）。 */
public class Page1Data implements PageData {
    // 表0: 基本信息
    public String settlementNo = DEFAULT_STRING;   // 1.1 结算单号
    public String period = DEFAULT_STRING;         // 1.2 结算期间
    public String operatorName = DEFAULT_STRING;   // 2.1 运营商名称
    public String operatorCode = DEFAULT_STRING;   // 2.2 运营商编号
    public String settlerName = DEFAULT_STRING;    // 3.1 结算方名称
    public String issueDate = DEFAULT_STRING;      // 3.2 出单日期
    public String accountName = DEFAULT_STRING;    // 4.1 收款户名
    public String bankName = DEFAULT_STRING;       // 4.2 开户银行
    public String accountNumber = DEFAULT_STRING;  // 5.1 银行账号

    // 表1：运营商线上结算收入
    public String totalElecMoney = DEFAULT_DOUBLE;        // 1.1 电费-净额
    public String clrElecMoney = DEFAULT_DOUBLE;     // 1.2 电费-收入
    public String totalServMoney = DEFAULT_DOUBLE;       // 2.1 服务费-净额
    public String clrServMoney = DEFAULT_DOUBLE;    // 2.2 服务费-收入
    public String totalTmoutMoney = DEFAULT_DOUBLE;    // 3.1 占位费-净额
    public String clrTmoutMoney = DEFAULT_DOUBLE; // 3.2 占位费-收入
    public String totalMoney = DEFAULT_STRING;         // 4.1 合计-净额（忘传则留空）
    public String clrMoney = DEFAULT_STRING;      // 4.2 合计-收入（忘传则留空）
    public String amountWords = DEFAULT_STRING;      // 5 大写金额

    // 表2：机构用户消费收入（线下结算）
    public String normalCharge = DEFAULT_DOUBLE;    // 1.1 普通机构-充电消费收入
    public String normalOccupy = DEFAULT_DOUBLE;    // 1.2 普通机构-占位费收入
    public String normalTotal = DEFAULT_DOUBLE;     // 1.3 普通机构-消费收入合计
    public String tedaCharge = DEFAULT_DOUBLE;     // 2.1 泰达电力机构-充电消费收入
    public String tedaOccupy = DEFAULT_DOUBLE;     // 2.2 泰达电力机构-占位费收入
    public String tedaTotal = DEFAULT_DOUBLE;      // 2.3 泰达电力机构-消费收入合计
    public String operatorCharge = DEFAULT_DOUBLE;  // 3.1 运营商机构-充电消费收入
    public String operatorOccupy = DEFAULT_DOUBLE;  // 3.2 运营商机构-占位费收入
    public String operatorTotal = DEFAULT_DOUBLE;   // 3.3 运营商机构-消费收入合计
    public String totalOrgCharge = DEFAULT_STRING;  // 4.1 合计-充电消费收入（忘传则留空）
    public String totalOrgOccupy = DEFAULT_STRING;  // 4.2 合计-占位费收入（忘传则留空）
    public String totalOrgIncome = DEFAULT_STRING;  // 4.3 合计-消费收入合计（忘传则留空）

    // 其他信息（底部签字区：左结算方 / 右运营商）
    public String settlerHandler = DEFAULT_STRING;  // 结算方-经办人
    public String settlerYear = DEFAULT_STRING;     // 结算方-日期-年
    public String settlerMonth = DEFAULT_STRING;    // 结算方-日期-月
    public String settlerDay = DEFAULT_STRING;      // 结算方-日期-日
    public String operatorHandler = DEFAULT_STRING; // 运营商-经办人
    public String operatorYear = DEFAULT_STRING;    // 运营商-日期-年
    public String operatorMonth = DEFAULT_STRING;   // 运营商-日期-月
    public String operatorDay = DEFAULT_STRING;     // 运营商-日期-日
}
