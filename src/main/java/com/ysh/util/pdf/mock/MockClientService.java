package com.ysh.util.pdf.mock;

import tools.jackson.databind.ObjectMapper;
import com.ysh.util.pdf.dto.reconciliation.ExportData;
import com.ysh.util.pdf.service.ReconciliationExportService;
import org.springframework.stereotype.Service;

/**
 * 模拟"别的服务"（demo）：演示拿到上游 JSON 后，如何拼装 Java 对象并调用导出服务。
 * 真实业务里，这个类相当于你同事自己服务里的代码。
 */
@Service
public class MockClientService {

    private final ReconciliationExportService settlementExportService;
    private final ObjectMapper objectMapper;

    public MockClientService(ReconciliationExportService settlementExportService, ObjectMapper objectMapper) {
        this.settlementExportService = settlementExportService;
        this.objectMapper = objectMapper;
    }

    /**
     * 别的服务接入的完整姿势（demo）：
     * 1. 收到上游 JSON（假设是你自己服务的业务报文）
     * 2. 拼装成本模块的 ExportData —— JSON 结构一致时 Jackson 一步到位；
     *    不一致时在这里手动 new Page1Data() 逐字段映射
     * 3. 调用 SettlementExportService.export() 拿到 PDF
     */
    public byte[] exportAsClient(String json) throws Exception {
        ExportData data = objectMapper.readValue(json, ExportData.class);
        return settlementExportService.export(data);
    }
}
