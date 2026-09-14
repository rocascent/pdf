package com.ysh.util.pdf.dto;

import java.math.BigDecimal;

public record StationDetailData(
        String id,
        String name,
        String parkFee,
        BigDecimal origElecFee,
        BigDecimal dcElecFee,
        BigDecimal handlElecFee,
        BigDecimal elecFee,
        BigDecimal origServFee,
        BigDecimal dcServeFee,
        BigDecimal handlServFee,
        BigDecimal servFee,
        BigDecimal origTmoutFee,
        BigDecimal dcTmoutFee,
        BigDecimal handlTmoutFee,
        BigDecimal tmoutFee
) {
}
