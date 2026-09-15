package com.ysh.util.pdf.service;

import com.ysh.util.pdf.dto.taineng.TaiNengExportData;
import com.ysh.util.pdf.exporter.taineng.TaiNengPdfExporter;
import org.springframework.stereotype.Service;

/**
 * 泰能充平台月度结算单导出服务 —— 其他后端服务直接注入调用这里。
 */
@Service
public class TaiNengExportService {

    /** 导出泰能充结算单：Java 对象进，PDF 字节出。 */
    public byte[] export(TaiNengExportData data) throws Exception {
        return TaiNengPdfExporter.export(data);
    }
}
