package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

public record StationClrDetailData(
        String stationId,
        String operatorName,
        BigDecimal elecRate,
        BigDecimal servRate,
        BigDecimal tmoutRate,
        BigDecimal elecFee,
        BigDecimal servFee,
        BigDecimal tmoutFee
) {
}
