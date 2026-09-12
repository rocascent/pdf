package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

public record StationEntIncomeData(
        String name,
        BigDecimal normalElecMoney,
        BigDecimal normalServMoney,
        BigDecimal normalTmoutMoney,
        BigDecimal operatorElecMoney,
        BigDecimal operatorServMoney,
        BigDecimal operatorTmoutMoney,
        BigDecimal tedaElecMoney,
        BigDecimal tedaServMoney,
        BigDecimal tedaTmoutMoney
) {
    public BigDecimal normalCharge() {
        return normalElecMoney().add(normalServMoney());
    }

    public BigDecimal normalTotal() {
        return normalCharge().add(normalTmoutMoney());
    }

    public BigDecimal operatorCharge() {
        return operatorElecMoney().add(operatorServMoney());
    }

    public BigDecimal operatorTotal() {
        return operatorCharge().add(operatorTmoutMoney());
    }

    public BigDecimal tedaCharge() {
        return tedaElecMoney().add(tedaServMoney());
    }

    public BigDecimal tedaTotal() {
        return tedaCharge().add(tedaTmoutMoney());
    }
}
