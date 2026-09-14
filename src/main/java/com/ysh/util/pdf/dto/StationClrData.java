package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

public record StationClrData(
        String name,
        BigDecimal clrElecFee,
        BigDecimal clrServFee,
        BigDecimal clrTmoutFee
) {
}
