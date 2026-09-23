package com.ysh.util.pdf.exporter.taineng;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import com.ysh.util.pdf.dto.settlement.Page1Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 泰能充结算单基础绘制：版式常量 + 文本/表格工具 + 页眉/页脚（各页共用）。 */
final class PdfDrawer {

    // ==================== 版式常量（新模板实测） ====================

    static final float PAGE_W = 595.28f, PAGE_H = 841.89f;  // A4
    static final float L = 89f, R = 569f;                   // 表2/站点表外框左右边界

    static final float FS_TITLE = 18f;      // 大标题（黑体加粗）
    static final float FS_HEAD = 10.5f;     // 导出时间/金额单位/开户行/账号
    static final float FS_SEC = 14f;        // 节标题（SimHei）
    static final float FS_ROW = 10f;        // 表格正文/表头（SimSun）
    static final float FS_FOOT = 9f;        // 页脚

    static final float TITLE_Y = 748f;      // 大标题基线
    static final float TIME_Y = 713f;       // 导出时间基线
    static final float MONEY_UNIT_X = 394f; // "金额单位" x
    static final float FOOT_TEXT_Y = 52f;   // 页脚文字基线
    static final float FOOT_PAGE_X = 501f;  // 页码数字 x
    static final float FOOT_PAGE_Y = 52.5f; // 页码数字基线

    static final float ROW_H = 30f;          // 站点表普通行高
    static final float CELL_PAD = 8f;        // 单元格左右内边距（折行可用宽度 = 列宽 - PAD）
    static final float DROP = 4f;            // 基线在行内的下移量（模板实测反推：top - h/2 - DROP = baseline）
    static final float MIN_Y = 80f;          // 表格底线最低允许值（低于则换页）
    static final float TITLE_BLOCK = 41f;    // 站点接续标题块：上一个内容底线 → 新表格顶线（节标题 16 + 顶差 25）
    static final float CONT_TOP = 770f;      // 续页表格顶线（模板实测）

    // 颜色（模板实测：全黑，无灰底）
    static final Color BLACK = new DeviceRgb(17, 17, 17);

    private PdfDrawer() {
    }

    // ==================== 绘制工具 ====================

    /** 画文字并返回结束 x（null/空串跳过，颜色 BLACK）。 */
    static float drawText(PdfCanvas cv, PdfFont font,
                          float x, float y, String text, float size) {
        if (text == null || text.isEmpty()) return x;
        cv.beginText().setFontAndSize(font, size).setColor(BLACK, true)
                .moveText(x, y).showText(text).endText();
        return x + font.getWidth(text, size);
    }

    /** 表格外框 + 内部列线/行线（模板实测全为 1.0 黑色，无粗细之分）。rowLines 含表头底线及各数据行底线。 */
    static void drawTable(PdfCanvas cv, float[] xs,
                          float top, float bottom, float[] rowLines) {
        cv.setLineWidth(1f).setStrokeColor(BLACK);
        cv.moveTo(xs[0], top).lineTo(xs[xs.length - 1], top).stroke();
        cv.moveTo(xs[0], bottom).lineTo(xs[xs.length - 1], bottom).stroke();
        cv.moveTo(xs[0], top).lineTo(xs[0], bottom).stroke();
        cv.moveTo(xs[xs.length - 1], top).lineTo(xs[xs.length - 1], bottom).stroke();
        for (int i = 1; i < xs.length - 1; i++) {
            cv.moveTo(xs[i], top).lineTo(xs[i], bottom).stroke();
        }
        for (float ly : rowLines) {
            cv.moveTo(xs[0], ly).lineTo(xs[xs.length - 1], ly).stroke();
        }
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

    /** 单格折行后的行数。 */
    static int lineCount(String text, PdfFont font, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) return 1;
        return wrapLines(text, font, fontSize, maxWidth).size();
    }

    /** 一行中各格折行后的最大行数（texts[i] 用 widths[i] 折行），用于按内容撑开行高。 */
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
     * 单行时等价于 top - h/2 - DROP 基线（与模板固定坐标一致）。
     */
    static void drawCellText(PdfCanvas cv, PdfFont font,
                             float x, float top, float h, float maxWidth,
                             String text, float fontSize, float lineH) {
        if (text == null || text.isEmpty()) return;
        List<String> lines = wrapLines(text, font, fontSize, maxWidth);
        float blockTop = top - (h - lines.size() * lineH) / 2f;
        for (int i = 0; i < lines.size(); i++) {
            drawText(cv, font, x, blockTop - i * lineH - lineH / 2f - DROP, lines.get(i), fontSize);
        }
    }

    // ==================== 页眉 / 页脚（每页一致） ====================

    /** 填空值前后补英文空格（无值返回空串，drawText 会跳过）。 */
    private static String padded(String v) {
        return (v == null || v.isEmpty()) ? "" : " " + v.trim() + " ";
    }

    /** 大标题「运营商[年份]年[月份]月泰能充平台结算单」（黑体加粗 18pt，居中）+ 导出时间 + 金额单位；无页眉分隔线。 */
    static void drawHeader(PdfCanvas cv, PdfFont font, PdfFont bold,
                           Page1Data p) {
        // 大标题：运营商[年份]年[月份]月泰能充平台结算单（黑体加粗，填空值前后留空格，动态测量总宽后居中，无下划线）
        String year = p.settleYear == null ? "" : p.settleYear.trim();
        String month = p.settleMonth == null ? "" : p.settleMonth.trim();
        String title = "运营商" + padded(year) + "年" + padded(month) + "月泰能充平台结算单";
        float titleW = bold.getWidth(title, FS_TITLE);
        drawText(cv, bold, PAGE_W / 2f - titleW / 2f, TITLE_Y, title, FS_TITLE);

        // 导出时间：结算单导出时间：2026 年 09 月 15 日 10 时 30 分（填空值前后留空格，无下划线）
        float x = L;
        x = drawText(cv, font, x, TIME_Y, "结算单导出时间：", FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, padded(p.exportYear), FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "年", FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, padded(p.exportMonth), FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "月", FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, padded(p.exportDay), FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "日", FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, padded(p.exportHour), FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "时", FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, padded(p.exportMinute), FS_HEAD);
        drawText(cv, font, x, TIME_Y, "分", FS_HEAD);

        drawText(cv, font, MONEY_UNIT_X, TIME_Y, "金额单位：人民币元", FS_HEAD);
    }

    /** 页脚：居中文档名（无页码框；页码数字由 drawPageNos 统一回填）。 */
    static void drawFooter(PdfCanvas cv, PdfFont font) {
        drawText(cv, font, PAGE_W / 2f - font.getWidth("泰能充平台月度结算单", FS_FOOT) / 2f,
                FOOT_TEXT_Y, "泰能充平台月度结算单", FS_FOOT);
    }

    /** 页码数字统一回填：所有页画完后调用，此时总页数才确定。 */
    static void drawPageNos(PdfDocument pdf, PdfFont font) {
        int total = pdf.getNumberOfPages();
        for (int pg = 1; pg <= total; pg++) {
            PdfCanvas cv = new PdfCanvas(pdf.getPage(pg));
            drawText(cv, font, FOOT_PAGE_X, FOOT_PAGE_Y,
                    String.valueOf(pg), FS_FOOT);
        }
    }
}
