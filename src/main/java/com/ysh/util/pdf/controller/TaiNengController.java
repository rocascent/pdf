package com.ysh.util.pdf.controller;

import com.ysh.util.pdf.dto.taineng.TaiNengExportData;
import com.ysh.util.pdf.service.TaiNengExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 泰能充平台月度结算单导出正式接口。
 *
 * <p>POST /api/taineng/export，请求体为 TaiNengExportData 对应的 JSON，
 * 返回 PDF 文件下载流。
 */
@RestController
@RequestMapping("/api/taineng")
public class TaiNengController {

    private final TaiNengExportService exportService;

    public TaiNengController(TaiNengExportService exportService) {
        this.exportService = exportService;
    }

    /** 导出泰能充结算单 PDF：JSON 进，PDF 文件下载流出。 */
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody TaiNengExportData data) throws Exception {
        byte[] pdf = exportService.export(data);

        String name = (data == null || data.page1 == null
                || data.page1.operatorName == null || data.page1.operatorName.isBlank())
                ? "taineng" : data.page1.operatorName;
        String filename = name + "泰能充平台月度结算单.pdf";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
