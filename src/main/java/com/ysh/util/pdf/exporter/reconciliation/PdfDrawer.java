package com.ysh.util.pdf.exporter.reconciliation;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 基础绘制：文本、线条、页码回填；页 2/3/4 共用的版式常量与页眉/页脚/抬头小表。 */
final class PdfDrawer {

    // ==================== 版式常量（页 2/3/4 自画共用） ====================

    static final float L = 42f, R = 553f;      // 表格左右边界
    static final float FS = 9f;                // 正文/表格字号
    static final float FS_HEAD = 8.5f;         // 页眉"运营结算/金额单位"
    static final float FS_TITLE = 19f;         // 大标题
    static final float FS_SEC = 10.5f;         // 节标题
    static final float FS_FOOT = 7.8f;         // 页脚单号
    static final float MIN_Y = 60f;            // 表格底线最低允许值（低于则换页）
    static final float CONT_TOP = 730f;        // 续页节标题基线
    static final float CELL_DROP = 3.15f;      // 单元格文本基线在行内的下移量
    static final float CELL_PAD = 8f;          // 单元格左内边距 + 右侧留白（折行可用宽度用）

    // 页眉大标题区
    static final float HEAD_TOP_Y = 810f;      // "运营结算/金额单位"基线
    static final float MONEY_UNIT_X = 477f;    // "金额单位：人民币元" x
    static final float HEAD_TITLE_Y = 779f;    // 大标题基线
    static final float HEAD_SUB_Y = 758f;      // 副标题基线
    static final float HEAD_LINE_Y = 744f;     // 页眉分隔线

    // 抬头小表（运营商名称/结算期间 [+ 场站名称/编号]）
    static final float BOX_TOP = 729f;         // 顶线
    static final float BOX_ROW_H = 27f;        // 抬头小表单行高度（多行文本按此撑开）
    private static final float BOX_DROP = 2.5f;  // 抬头小表基线偏移（=27/2-16，对齐模板实测 713/686）
    private static final float BOX_LABEL_X = 48f, BOX_VALUE_X = 119f;
    private static final float BOX_LABEL2_X = 303f, BOX_VALUE2_X = 374f;
    private static final float[] BOX_XS = {L, 115f, 297f, 370f, R};

    // 页脚
    static final float FOOT_LINE_Y = 46f;      // 横线
    static final float FOOT_TEXT_Y = 29f;      // 文字基线
    static final float FOOT_LABEL_X = 42f;     // "结算单号：" x
    static final float FOOT_NO_X = 84f;        // 单号 x

    private PdfDrawer() {
    }

    static void drawText(PdfDocument pdf, PdfFont font, int pageNum,
                         float x, float y, String text, float fontSize) {
        if (text == null || text.isEmpty()) return;
        drawText(pdf.getPage(pageNum), font, x, y, text, fontSize);
    }

    static void drawText(PdfPage page, PdfFont font,
                         float x, float y, String text, float fontSize) {
        if (text == null || text.isEmpty()) return;
        new PdfCanvas(page).beginText()
                .setFontAndSize(font, fontSize)
                .moveText(x, y)
                .showText(text)
                .endText();
    }

    /** 画横线（自画表用）。 */
    static void hLine(PdfPage page, float x1, float x2, float y) {
        new PdfCanvas(page).setLineWidth(0.35f)
                .moveTo(x1, y).lineTo(x2, y).stroke();
    }

    /** 画加粗横线（合计行上边线等强调线，模板实测 0.35+0.70 叠加，此处 0.7 粗单线等效）。 */
    static void hLineBold(PdfPage page, float x1, float x2, float y) {
        new PdfCanvas(page).setLineWidth(0.7f)
                .moveTo(x1, y).lineTo(x2, y).stroke();
    }

    /** 画竖线（自画表用）。 */
    static void vLine(PdfPage page, float x, float y1, float y2) {
        new PdfCanvas(page).setLineWidth(0.35f)
                .moveTo(x, y1).lineTo(x, y2).stroke();
    }

    /** 画加粗竖线（表格左右外框）。 */
    static void vLineBold(PdfPage page, float x, float y1, float y2) {
        new PdfCanvas(page).setLineWidth(0.7f)
                .moveTo(x, y1).lineTo(x, y2).stroke();
    }

    /**
     * 页码统一回填（最后一步调用，此时总页数才确定）：
     * 白色矩形盖掉旧页码区（含模板预印页码），画"第 X 页 / 共 Y 页"。
     */
    static void drawPageNos(PdfDocument pdf, PdfFont font) {
        int total = pdf.getNumberOfPages();
        for (int pg = 1; pg <= total; pg++) {
            PdfPage page = pdf.getPage(pg);
            new PdfCanvas(page).saveState()
                    .setFillColor(ColorConstants.WHITE)
                    .rectangle(476f, 20f, 102f, 20f)
                    .fill()
                    .restoreState();
            drawText(page, font, 487f, 29f, "第 " + pg + " 页 / 共 " + total + " 页", 9f);
        }
    }

    /** 画白色矩形（遮盖模板预印内容用）。 */
    static void fillWhite(PdfPage page, float x, float y, float w, float h) {
        new PdfCanvas(page).saveState()
                .setFillColor(ColorConstants.WHITE)
                .rectangle(x, y, w, h)
                .fill()
                .restoreState();
    }

    // ==================== PAGE HEADER / FOOTER / INFO BOX（页 2/3/4 共用） ====================

    /** 页眉大标题区（每页都画）：运营结算 / 金额单位 / 大标题 / 副标题 / 分隔线。 */
    static void drawTitleHeader(PdfPage page, PdfFont font,
                                float titleX, String title, float subX, String sub) {
        drawText(page, font, L, HEAD_TOP_Y, "运营结算", FS_HEAD);
        drawText(page, font, MONEY_UNIT_X, HEAD_TOP_Y, "金额单位：人民币元", FS_HEAD);
        drawText(page, font, titleX, HEAD_TITLE_Y, title, FS_TITLE);
        drawText(page, font, subX, HEAD_SUB_Y, sub, FS);
        hLine(page, L, R, HEAD_LINE_Y);
    }

    /**
     * 抬头小表：第一行 运营商名称/结算期间；stationName 非 null 时补第二行 场站名称/编号。
     * 单元格支持 \n 与按宽度自动折行（行高撑开）；返回表格底线 y，下方内容据此下移。
     */
    static float drawOperatorBox(PdfPage page, PdfFont font,
                                 String operatorName, String period,
                                 String stationName, String stationCode) {
        float[] w = colWidths(BOX_XS);
        float row1H = rowLines(font, FS, w, "运营商名称", operatorName, "结算期间", period) * BOX_ROW_H;
        boolean twoRows = stationName != null || stationCode != null;
        float row2H = twoRows
                ? rowLines(font, FS, w, "场站名称", stationName, "场站编号", stationCode) * BOX_ROW_H
                : 0f;
        float bottom = BOX_TOP - row1H - row2H;

        hLineBold(page, L, R, BOX_TOP);
        if (twoRows) hLine(page, L, R, BOX_TOP - row1H);   // 双行的中间分隔线
        hLineBold(page, L, R, bottom);
        for (float x : BOX_XS) {
            if (x == L || x == R) vLineBold(page, x, bottom, BOX_TOP);
            else vLine(page, x, bottom, BOX_TOP);
        }
        float[] cols = {BOX_LABEL_X, BOX_VALUE_X, BOX_LABEL2_X, BOX_VALUE2_X};
        String[] row1 = {"运营商名称", operatorName, "结算期间", period};
        for (int c = 0; c < row1.length; c++) {
            drawCellText(page, font, cols[c], BOX_TOP, row1H, w[c], row1[c], FS, BOX_ROW_H, BOX_DROP);
        }
        if (twoRows) {
            String[] row2 = {"场站名称", stationName, "场站编号", stationCode};
            for (int c = 0; c < row2.length; c++) {
                drawCellText(page, font, cols[c], BOX_TOP - row1H, row2H, w[c], row2[c], FS, BOX_ROW_H, BOX_DROP);
            }
        }
        return bottom;
    }

    /** 页脚底衬：横线 + 结算单号（页码文本由 drawPageNos 统一回填）。 */
    static void drawFooterBase(PdfPage page, PdfFont font, String settlementNo) {
        hLine(page, L, R, FOOT_LINE_Y);
        drawText(page, font, FOOT_LABEL_X, FOOT_TEXT_Y, "结算单号：", FS_FOOT);
        drawText(page, font, FOOT_NO_X, FOOT_TEXT_Y, settlementNo, FS_FOOT);
    }

    // ==================== 单元格折行（先显式 \n，再按宽度自动折行） ====================

    /** 各列可用宽度 = 列宽 - CELL_PAD；xs 为列边界数组（含最左 L 与最右 R）。 */
    static float[] colWidths(float[] xs) {
        float[] w = new float[xs.length - 1];
        for (int i = 0; i < w.length; i++) {
            w[i] = Math.max(0f, xs[i + 1] - xs[i] - CELL_PAD);
        }
        return w;
    }

    /** 显式 \n 断行（去掉末尾空行）。 */
    private static String[] splitExplicit(String text) {
        String[] raw = text.replace("\r", "").split("\n", -1);
        int n = raw.length;
        while (n > 1 && raw[n - 1].isBlank()) n--;
        return Arrays.copyOf(raw, n);
    }

    /** 单元格文本折行：先按显式 \n 断行，再对每段按 maxWidth 逐字折行。 */
    static List<String> wrapLines(String text, PdfFont font, float fontSize, float maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            out.add("");
            return out;
        }
        for (String seg : splitExplicit(text)) {
            if (seg.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < seg.length(); ) {
                int cp = seg.codePointAt(i);
                String ch = new String(Character.toChars(cp));
                if (cur.length() > 0 && font.getWidth(cur + ch, fontSize) > maxWidth) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                cur.append(ch);
                i += Character.charCount(cp);
            }
            out.add(cur.toString());
        }
        return out;
    }

    /** 单格折行后的行数（不截断，行高/组高据此撑开）。 */
    static int lineCount(String text, PdfFont font, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) return 1;
        return wrapLines(text, font, fontSize, maxWidth).size();
    }

    /**
     * 一行中各格折行后的最大行数（texts[i] 用 widths[i] 折行；widths 为 null 或不够则不折）。
     * 用于按内容撑开行高。
     */
    static int rowLines(PdfFont font, float fontSize, float[] widths, String... texts) {
        int max = 1;
        if (texts == null) return max;
        for (int i = 0; i < texts.length; i++) {
            float w = (widths == null || i >= widths.length) ? Float.MAX_VALUE : widths[i];
            int n = lineCount(texts[i], font, fontSize, w);
            if (n > max) max = n;
        }
        return max;
    }

    /**
     * 画单元格文本：先显式 \n 断行、再按 maxWidth 自动折行，整块在 [top-h, top] 内垂直居中，行距 lineH。
     * 单行时等价于原来的 top - h/2 - CELL_DROP 基线。
     */
    static void drawCellText(PdfPage page, PdfFont font, float x, float top, float h, float maxWidth,
                             String text, float fontSize, float lineH) {
        drawCellText(page, font, x, top, h, maxWidth, text, fontSize, lineH, CELL_DROP);
    }

    /** 同上，drop = 首行基线的额外下移量（默认 CELL_DROP；抬头小表用 BOX_DROP 对齐模板实测坐标）。 */
    static void drawCellText(PdfPage page, PdfFont font, float x, float top, float h, float maxWidth,
                             String text, float fontSize, float lineH, float drop) {
        if (text == null || text.isEmpty()) return;
        List<String> lines = wrapLines(text, font, fontSize, maxWidth);
        float blockTop = top - (h - lines.size() * lineH) / 2f;
        for (int i = 0; i < lines.size(); i++) {
            drawText(page, font, x, blockTop - i * lineH - lineH / 2f - drop, lines.get(i), fontSize);
        }
    }

    // ==================== 表格公共绘制 ====================

    /** 整行竖线：首末列加粗（表格外框），其余细线。 */
    static void vLines(PdfPage page, float[] xs, float y0, float y1) {
        for (float x : xs) {
            if (x == xs[0] || x == xs[xs.length - 1]) vLineBold(page, x, y0, y1);
            else vLine(page, x, y0, y1);
        }
    }

    /** 表头带：顶线加粗 + 表头文本（按列宽折行、整带加高）+ 底线 + 竖线；返回底线 y。 */
    static float drawHeaderBand(PdfPage page, PdfFont boldFont, float fontSize, float top, float rowH,
                                float[] xs, String[] header) {
        float right = xs[xs.length - 1];
        float[] w = colWidths(xs);
        float bandH = rowLines(boldFont, fontSize, w, header) * rowH;
        hLineBold(page, xs[0], right, top);
        for (int c = 0; c < header.length; c++) {
            drawCellText(page, boldFont, xs[c] + 6f, top, bandH, w[c], header[c], fontSize, rowH);
        }
        float y = top - bandH;
        hLine(page, xs[0], right, y);
        vLines(page, xs, y, top);
        return y;
    }

    // ==================== CELL HELPER ====================

    /** 固定表单元格空值补 "0.00"（调用方未给该字段时不留空）。 */
    static String z(String v) {
        return v == null || v.isBlank() ? "0.00" : v.trim();
    }
}
