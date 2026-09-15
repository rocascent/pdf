package com.ysh.util.pdf.exporter.taineng;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import com.ysh.util.pdf.dto.taineng.TaiNengExportData;
import com.ysh.util.pdf.dto.taineng.TaiNengPage1Data;
import com.ysh.util.pdf.dto.taineng.TaiNengStationData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 泰能充平台月度结算单 PDF 导出 —— 整页自画（模板仅作坐标参考）：
 *
 *   页 1    ：运营商信息 + 合计信息（TaiNengPage1Drawer）；
 *   页 2..N ：各充电站清单，每站点 1 页（TaiNengStationDrawer），
 *             第 1 个站点标题"三、各充电站清单"，第 2 个起加"（续）"；
 *   黑体部分用思源宋体 Bold 顶替（项目内无黑体字体，与老结算单一致）。
 */
public class TaiNengPdfExporter {

    // 字体字节与 PDF 文档无关，可全局共享；PdfFont 绑定文档，必须每个文档新建
    private static byte[] fontBytes;
    private static byte[] boldFontBytes;

    static {
        try (InputStream is = TaiNengPdfExporter.class
                .getResourceAsStream("/fonts/SourceHanSerifCN-Regular.otf");
             InputStream ib = TaiNengPdfExporter.class
                     .getResourceAsStream("/fonts/simhei.ttf")) {
            if (is == null || ib == null) throw new IOException("Font not found");
            fontBytes = is.readAllBytes();
            boldFontBytes = ib.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Init failed", e);
        }
    }

    public static void export(TaiNengExportData data, OutputStream out) throws IOException {
        if (data == null) data = new TaiNengExportData();
        TaiNengPage1Data p1 = data.page1 != null ? data.page1 : new TaiNengPage1Data();
        List<TaiNengStationData> stations =
                data.stations != null ? data.stations : List.of();

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            PdfFont font = newFont();
            PdfFont bold = newBoldFont();

            TaiNengPage1Drawer.draw(pdf, font, bold, p1);
            float prevBottom = 0;
            for (int i = 0; i < stations.size(); i++) {
                // 第 1 个站点强制新页，之后空间足够时接续上一页（避免整页空白）
                prevBottom = TaiNengStationDrawer.draw(pdf, font, bold, p1,
                        stations.get(i), i + 1, prevBottom, i == 0);
            }
            // 页码统一回填（总页数此时才确定）
            TaiNengDrawer.drawPageNos(pdf, font);
        }
    }

    public static byte[] export(TaiNengExportData data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        export(data, out);
        return out.toByteArray();
    }

    private static PdfFont newFont() throws IOException {
        return PdfFontFactory.createFont(fontBytes,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
    }

    private static PdfFont newBoldFont() throws IOException {
        return PdfFontFactory.createFont(boldFontBytes,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
    }
}
