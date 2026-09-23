package com.ysh.util.pdf.exporter.reconciliation;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

import com.ysh.util.pdf.dto.reconciliation.ExportData;
import com.ysh.util.pdf.dto.reconciliation.Page3Data;

import java.io.*;

import static com.ysh.util.pdf.exporter.reconciliation.PdfDrawer.*;

/**
 * Settlement PDF exporter - 单文档生成：
 *
 *   页 1    ：模板页 1 并入同一文档后填格（Page1Drawer）；
 *   页 2..N ：附件一整页自画（Page2Drawer），表格长则 Word 式延续多页；
 *   附件二  ：页 3 整页自画（Page3Drawer，表 2 组级换页可跨多页）；
 *   附件三  ：页 4 整页自画（Page4Drawer，表 2 无场站数据时留 1 个空组，多场站组级换页可跨多页）；
 *   全部画完后页码统一回填（总页数此刻才确定）。
 */
public class ReconciliationPdfExporter {

    private static byte[] templateBytes;
    // 字体字节与 PDF 文档无关，可全局共享；PdfFont 绑定文档，必须每个文档新建
    private static byte[] chineseFontBytes;
    private static byte[] boldFontBytes;

    static {
        try {
            try (InputStream is = ReconciliationPdfExporter.class
                    .getResourceAsStream("/template/settlement_v2.pdf")) {
                if (is == null) throw new IOException("Template not found");
                templateBytes = is.readAllBytes();
            }
            try (InputStream is = ReconciliationPdfExporter.class
                    .getResourceAsStream("/fonts/SourceHanSerifCN-Regular.otf")) {
                if (is == null) throw new IOException("Font not found");
                chineseFontBytes = is.readAllBytes();
            }
            try (InputStream is = ReconciliationPdfExporter.class
                    .getResourceAsStream("/fonts/SourceHanSerifCN-Bold.otf")) {
                if (is == null) throw new IOException("Bold font not found");
                boldFontBytes = is.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("Init failed", e);
        }
    }

    public static void export(ExportData data, OutputStream out) throws IOException {
        String settlementNo = data.page1 != null ? data.page1.settlementNo : null;

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             PdfDocument tpl = new PdfDocument(
                     new PdfReader(new ByteArrayInputStream(templateBytes)))) {
            // 页 1：模板页 1 并入同一文档，再填格
            new PdfMerger(pdf).merge(tpl, 1, 1);

            // 同一文档内共用字体（PdfFont 绑定文档，不能跨文档复用）
            PdfFont font = newFont();
            boolean needBold = data.page2 != null
                    || (data.page3 != null && !data.page3.isEmpty())
                    || data.page4 != null;
            PdfFont bold = needBold ? newBoldFont() : font;

            Page1Drawer.fill(pdf, font, data.page1);
            if (data.page2 != null) {
                Page2Drawer.fill(pdf, font, bold, data.page2, settlementNo);
            }
            if (data.page3 != null) {
                for (Page3Data p3 : data.page3) {
                    Page3Drawer.fill(pdf, font, bold, p3, settlementNo);
                }
            }
            if (data.page4 != null) {
                Page4Drawer.fill(pdf, font, bold, data.page4, settlementNo);
            }

            // 全部画完页码统一回填（总页数此刻才确定）
            drawPageNos(pdf, font);
        }
    }

    public static byte[] export(ExportData data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        export(data, out);
        return out.toByteArray();
    }

    private static PdfFont newFont() throws IOException {
        return PdfFontFactory.createFont(chineseFontBytes,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
    }

    private static PdfFont newBoldFont() throws IOException {
        return PdfFontFactory.createFont(boldFontBytes,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
    }
}