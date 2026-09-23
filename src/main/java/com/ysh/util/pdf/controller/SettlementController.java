package com.ysh.util.pdf.controller;

import com.ysh.util.pdf.service.SettlementDataService;
import com.ysh.util.pdf.service.SettlementExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 泰能充平台月度结算单导出正式接口。
 * <p>
 * GET /api/export/settlement，按时间段从数据库查询结算单数据，返回 TaiNengExportData JSON；
 */
@RestController
@RequestMapping("/api/export")
public class SettlementController {

    private final SettlementExportService exportService;
    private final SettlementDataService dataService;

    public SettlementController(SettlementExportService exportService, SettlementDataService taiNengDataService) {
        this.exportService = exportService;
        this.dataService = taiNengDataService;
    }

    /**
     * 导出泰能充结算单 PDF（按时间段从数据库查询）：GET /api/export/taineng，流式写出。
     */
    @GetMapping("/settlement")
    public ResponseEntity<StreamingResponseBody> exportByQuery(
            @RequestParam String tenantId,
            @RequestParam LocalDate startTime,
            @RequestParam LocalDate endTime
    ) {
        var data = dataService.getTaiNengData(tenantId, startTime, endTime);

        String name = (data == null || data.page1 == null
                || data.page1.operatorName == null || data.page1.operatorName.isBlank())
                ? "taineng" : data.page1.operatorName;
        String filename = name + "泰能充平台月度结算单.pdf";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream -> exportService.exportStream(data, stream));
    }
}
