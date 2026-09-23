package com.ysh.util.pdf.service;

import com.ysh.util.pdf.dto.settlement.SettlementExportData;
import com.ysh.util.pdf.exporter.taineng.SettlementPdfExporter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 泰能充平台月度结算单导出服务 —— 其他后端服务直接注入调用这里。
 */
@Service
public class SettlementExportService {

    /** 导出泰能充结算单：Java 对象进，PDF 字节出。 */
    public byte[] export(SettlementExportData data) throws IOException {
        return SettlementPdfExporter.export(data);
    }

    /** 流式导出：PDF 边生成边写出，整个文档不整体缓冲。 */
    public void exportStream(SettlementExportData data, OutputStream out) throws IOException {
        SettlementPdfExporter.export(data, out);
    }
}
