package com.ysh.util.pdf.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.ysh.util.pdf.service.ReconciliationDataService;
import com.ysh.util.pdf.service.ReconciliationExportService;

/**
 * 结算单导出正式接口。
 *
 * <p>
 * GET /api/export/settlement，返回 PDF 文件下载流（流式写出，不整体缓冲）。
 */
@RestController
@RequestMapping("/api/export")
public class ReconciliationController {

    private final ReconciliationExportService exportService;
    private final ReconciliationDataService chargingDataService;

    public ReconciliationController(ReconciliationExportService exportService, ReconciliationDataService chargingDataService) {
        this.exportService = exportService;
        this.chargingDataService = chargingDataService;
    }

    /**
     * 导出结算单 PDF：demo 数据进，PDF 文件下载流出。
     */
    @GetMapping("/reconciliation")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam(required = false) String tenantId,
            @RequestParam LocalDate startTime,
            @RequestParam LocalDate endTime
    ) {
        // if (!StpUtil.isLogin()) {
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).build();
        // }
        var data = chargingDataService.getChargingData(tenantId, startTime, endTime);

        String no = data.page1 == null ? null : data.page1.settlementNo;
        String filename = no == null || no.isBlank() ? "settlement" : no;
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream -> exportService.exportStream(data, stream));
    }
}
