package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

public record StationTotalClrData(
        String name,
        BigDecimal selfFee,
        BigDecimal otherFee
) {
    public BigDecimal totalFee() {
        return selfFee().add(otherFee());
    }
}
