package com.ysh.util.pdf.mock;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * demo 接口：模拟"别的服务"调用链路。
 * JSON 进 → MockClientService 拼装 Java 对象 → SettlementExportService → PDF 出。
 * 正式入口是 /api/settlement/export，这个仅用于演示和自测。
 */
@RestController
@RequestMapping("/api/mock")
public class MockClientController {

    private final MockClientService mockClientService;

    public MockClientController(MockClientService mockClientService) {
        this.mockClientService = mockClientService;
    }

    /** 模拟别的服务调用：直接返回 PDF 字节。byte[] 收请求避免中文 JSON 编码问题。 */
    @PostMapping(value = "/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] export(@RequestBody byte[] json) throws Exception {
        return mockClientService.exportAsClient(new String(json, StandardCharsets.UTF_8));
    }
}
