package com.ysh.util.pdf.exporter.taineng;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import com.ysh.util.pdf.dto.taineng.TaiNengPage1Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 泰能充结算单基础绘制：版式常量 + 文本/下划线填空/表格工具 + 页眉/页脚（各页共用）。 */
final class TaiNengDrawer {

    // ==================== 版式常量（模板实测） ====================

    static final float PAGE_W = 595.28f, PAGE_H = 841.89f;  // A4
    static final float L = 40f, R = 555.28f;                // 表格左右边界

    static final float FS_TITLE = 15f;      // 大标题
    static final float FS_HEAD = 8.5f;      // 导出时间/金额单位
    static final float FS_SEC = 11f;        // 节标题
    static final float FS_ROW = 8.8f;       // 表格正文
    static final float FS_TBL_HEAD = 9f;    // 页1表头
    static final float FS_STA_HEAD = 8.8f;  // 站点页表头
    static final float FS_FOOT = 8f;        // 页脚
    static final float FS_NOTE = 8.5f;      // 页1底部注
    static final float FS_STA_NOTE = 8.2f;  // 站点页底部注释

    static final float TITLE_Y = 798.89f;   // 大标题基线
    static final float TIME_Y = 775.89f;    // 导出时间基线
    static final float MONEY_UNIT_X = 478.78f;  // "金额单位" x
    static final float HEAD_LINE_Y = 763.89f;   // 页眉分隔线（0.8 粗）
    static final float FOOT_LINE_Y = 41f;       // 页脚横线
    static final float FOOT_TEXT_Y = 26f;       // 页脚文字基线

    static final float ROW_H = 22f;          // 基础行高（模板实测）
    static final float CELL_PAD = 8f;         // 单元格左右内边距（折行可用宽度 = 列宽 - PAD）
    static final float DROP = 2.7f;           // 基线在行内的下移量（模板实测反推：top - h/2 - DROP = baseline）
    static final float MIN_Y = 55f;           // 表格底线最低允许值（低于则换页）
    static final float NOTE_RESERVE = 24f;    // 注释区安全高度：最后一行底线最低 = MIN_Y + NOTE_RESERVE（防注释压页脚）
    static final float TITLE_BLOCK = 47f;     // 站点接续标题块：上一个内容底线 → 新表格顶线（节标题+站点标题）
    static final float CONT_TOP = 735f;       // 续页表格顶线

    // 颜色（模板实测）
    static final Color BLACK = new DeviceRgb(17, 17, 17);    // 外框线/文字
    static final Color GRAY = new DeviceRgb(102, 102, 102); // 内部线
    static final Color HEAD_FILL = new DeviceRgb(241, 241, 241); // 表头灰底

    private TaiNengDrawer() {
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

    /**
     * 下划线填空：填写区 = 空格 + 值 + 空格（不紧绷，左右各留一个空格宽），
     * 有值画值 + 覆盖整个填写区的下划线；无值画 N 个下划线占位。
     * 返回结束 x。
     */
    static float drawFilled(PdfCanvas cv, PdfFont font,
                            float x, float y, String value,
                            int placeholderLen, float size) {
        String v = value == null ? "" : value.trim();
        float spaceW = font.getWidth(" ", size);
        if (v.isEmpty()) {
            return drawText(cv, font, x, y,
                    " " + "_".repeat(placeholderLen) + " ", size);
        }
        float w = font.getWidth(v, size);
        float totalW = w + 2 * spaceW;
        drawText(cv, font, x, y, " " + v + " ", size);
        cv.setLineWidth(0.5f).setStrokeColor(BLACK)
                .moveTo(x, y - size * 0.18f).lineTo(x + totalW, y - size * 0.18f)
                .stroke();
        return x + totalW;
    }

    /** 与 drawFilled 一致的填写区总宽度（用于标题居中测量）。 */
    static float filledWidth(PdfFont font, String value, int placeholderLen, float size) {
        String v = value == null ? "" : value.trim();
        float spaceW = font.getWidth(" ", size);
        if (v.isEmpty()) {
            return font.getWidth("_", size) * placeholderLen + 2 * spaceW;
        }
        return font.getWidth(v, size) + 2 * spaceW;
    }

    /** 表格外框（0.7 黑色）+ 内部列线/行线（0.35 灰色）。rowLines 含表头底线及各数据行底线。 */
    static void drawTable(PdfCanvas cv, float[] xs,
                          float top, float bottom, float[] rowLines) {
        // 外框线：0.7 黑色
        cv.setLineWidth(0.7f).setStrokeColor(BLACK);
        cv.moveTo(xs[0], top).lineTo(xs[xs.length - 1], top).stroke();
        cv.moveTo(xs[0], bottom).lineTo(xs[xs.length - 1], bottom).stroke();
        cv.moveTo(xs[0], top).lineTo(xs[0], bottom).stroke();
        cv.moveTo(xs[xs.length - 1], top).lineTo(xs[xs.length - 1], bottom).stroke();
        // 内部线：0.35 灰色
        cv.setLineWidth(0.35f).setStrokeColor(GRAY);
        for (int i = 1; i < xs.length - 1; i++) {
            cv.moveTo(xs[i], top).lineTo(xs[i], bottom).stroke();
        }
        for (float ly : rowLines) {
            cv.moveTo(xs[0], ly).lineTo(xs[xs.length - 1], ly).stroke();
        }
    }

    /** 画加粗横线（强调线，模板实测 0.8 黑色）。 */
    static void hLineBold(PdfCanvas cv, float x1, float x2, float y) {
        cv.setLineWidth(0.8f).setStrokeColor(BLACK)
                .moveTo(x1, y).lineTo(x2, y).stroke();
    }

    /** 表头灰底（模板实测 241,241,241）。 */
    static void fillGray(PdfCanvas cv, float x, float y, float w, float h) {
        cv.saveState()
                .setFillColor(HEAD_FILL)
                .rectangle(x, y, w, h).fill()
                .restoreState();
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

    /**
     * 一行中各格折行后的最大行数（texts[i] 用 widths[i] 折行）。
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

    /** 大标题（下划线填空拼接，居中）+ 导出时间 + 金额单位 + 上边线。 */
    static void drawHeader(PdfCanvas cv, PdfFont font, PdfFont bold,
                           TaiNengPage1Data p) {
        // 大标题：____运营商____年____月泰能充平台结算单（动态测量总宽后居中）
        float titleW = filledWidth(bold, p.operatorName, 8, FS_TITLE)
                + bold.getWidth("运营商", FS_TITLE)
                + filledWidth(bold, p.settleYear, 6, FS_TITLE)
                + bold.getWidth("年", FS_TITLE)
                + filledWidth(bold, p.settleMonth, 4, FS_TITLE)
                + bold.getWidth("月泰能充平台结算单", FS_TITLE);
        float x = PAGE_W / 2f - titleW / 2f;
        x = drawFilled(cv, bold, x, TITLE_Y, p.operatorName, 8, FS_TITLE);
        x = drawText(cv, bold, x, TITLE_Y, "运营商", FS_TITLE);
        x = drawFilled(cv, bold, x, TITLE_Y, p.settleYear, 6, FS_TITLE);
        x = drawText(cv, bold, x, TITLE_Y, "年", FS_TITLE);
        x = drawFilled(cv, bold, x, TITLE_Y, p.settleMonth, 4, FS_TITLE);
        drawText(cv, bold, x, TITLE_Y, "月泰能充平台结算单", FS_TITLE);

        // 导出时间：结算单导出时间：____年____月____日____时____分
        x = L;
        x = drawText(cv, font, x, TIME_Y, "结算单导出时间：", FS_HEAD);
        x = drawFilled(cv, font, x, TIME_Y, p.exportYear, 6, FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "年", FS_HEAD);
        x = drawFilled(cv, font, x, TIME_Y, p.exportMonth, 4, FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "月", FS_HEAD);
        x = drawFilled(cv, font, x, TIME_Y, p.exportDay, 4, FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "日", FS_HEAD);
        x = drawFilled(cv, font, x, TIME_Y, p.exportHour, 4, FS_HEAD);
        x = drawText(cv, font, x, TIME_Y, "时", FS_HEAD);
        x = drawFilled(cv, font, x, TIME_Y, p.exportMinute, 4, FS_HEAD);
        drawText(cv, font, x, TIME_Y, "分", FS_HEAD);

        drawText(cv, font, MONEY_UNIT_X, TIME_Y, "金额单位：人民币元", FS_HEAD);
        cv.setLineWidth(0.8f).setStrokeColor(BLACK);
        cv.moveTo(L, HEAD_LINE_Y).lineTo(R, HEAD_LINE_Y).stroke();
    }

    /** 页脚底衬：下边线 + 文档名（页码由 drawPageNos 统一回填）。 */
    static void drawFooter(PdfCanvas cv, PdfFont font) {
        cv.setLineWidth(0.4f).setStrokeColor(BLACK);
        cv.moveTo(L, FOOT_LINE_Y).lineTo(R, FOOT_LINE_Y).stroke();
        drawText(cv, font, L, FOOT_TEXT_Y, "泰能充平台月度结算单", FS_FOOT);
    }

    /** 页码统一回填：所有页画完后调用，此时总页数才确定。 */
    static void drawPageNos(PdfDocument pdf, PdfFont font) {
        int total = pdf.getNumberOfPages();
        for (int pg = 1; pg <= total; pg++) {
            PdfCanvas cv = new PdfCanvas(pdf.getPage(pg));
            drawText(cv, font, 487.28f, FOOT_TEXT_Y,
                    "第 " + pg + " 页 / 共 " + total + " 页", FS_FOOT);
        }
    }
}
