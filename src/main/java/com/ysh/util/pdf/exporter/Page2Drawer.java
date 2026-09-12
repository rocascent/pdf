package com.ysh.util.pdf.exporter;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.Page2Data;
import com.ysh.util.pdf.dto.Page2Table2Row;

import java.util.List;

import static com.ysh.util.pdf.exporter.PdfDrawer.*;

/**
 * 第 2 页（附件一 场站线上清分汇总）
 * 表 1：Page2Table1Row（station + 4 数值，顺序即语义，最后一行 = 合计）
 * 表 2：Page2Table2Row（station + 3 数值，顺序即语义，最后一行 = 合计）
 */
final class Page2Drawer {

    // 版式常量（L/R/FS/FS_*/MIN_Y/CONT_TOP/页眉页脚）统一放 PdfDrawer；此处只留本页特有
    private static final float FS_T2 = 9.1f;        // 表 2 字号
    private static final float ROW_H1 = 33f;        // 表 1 基础行高
    private static final float ROW_H2 = 29f;        // 表 2 基础行高
    private static final float NOTES_H = 57f;       // 表 2 下方 3 行注释块高度（换页判断用）
    private static final int MIN_DATA_ROWS = 5;     // 数据行不足时补空行到该行数

    private Page2Drawer() {
    }

    static void fill(PdfDocument pdf, PdfFont font, PdfFont boldFont,
                     Page2Data p, String settlementNo) {
        if (p == null) return;

        PdfPage page = pdf.addNewPage();
        float boxBottom = drawHeader(page, font, p, false);   // 抬头小表底线（长文本会撑高）
        drawFooterBase(page, font, settlementNo);

        // 一、场站线上清分收入
        float[] x1 = {L, 185f, 277f, 369f, 461f, R};
        String[] h1 = {"场站名称 / 编号", "本方电费收入", "本方服务费收入", "本方占位费收入", "本方清分收入合计"};
        drawText(page, font, 42f, boxBottom - 24f, "一、场站线上清分收入", FS_SEC);
        float y = drawRows(pdf, font, boldFont, page, p.table1, boxBottom - 34f, ROW_H1, x1, h1, FS,
                "一、场站线上清分收入（续）", p, settlementNo,
                r -> new String[]{r.station, r.electricity, r.service, r.parking, r.total});
        page = pdf.getLastPage();

        // 二、场站分配总额核对：标题 + 表头带 + 首个数据行放不下则整块换页，避免页底只剩孤立标题/表头
        float[] x2 = {L, 185f, 307f, 430f, R};
        String[] h2 = {"场站名称 / 编号", "线上可分配净额", "本运营商清分收入", "其他清分方收入"};
        float[] w2 = colWidths(x2);
        float blockH = rowLines(boldFont, FS_T2, w2, h2) * ROW_H2;
        if (p.table2 != null && !p.table2.isEmpty()) {
            Page2Table2Row r0 = p.table2.get(0);
            blockH += rowLines(font, FS_T2, w2, r0.station, r0.onlineNet, r0.operatorIncome, r0.otherIncome) * ROW_H2;
        }
        float t2 = y - 24f;
        if (t2 - 10f - blockH < MIN_Y) {
            page = newPage(pdf, font, p, settlementNo);
            t2 = CONT_TOP + 10f - 24f;
        }
        drawText(page, font, 42f, t2, "二、场站分配总额核对", FS_SEC);
        y = drawRows(pdf, font, boldFont, page, p.table2, t2 - 10f, ROW_H2, x2, h2, FS_T2,
                "二、场站分配总额核对（续）", p, settlementNo,
                r -> new String[]{r.station, r.onlineNet, r.operatorIncome, r.otherIncome});
        page = pdf.getLastPage();

        // 注释块放不下则另起新页，避免压到页脚/出纸
        float notesTop = y;
        if (notesTop - NOTES_H < MIN_Y) {
            page = newPage(pdf, font, p, settlementNo);
            notesTop = CONT_TOP;
        }
        drawText(page, font, 42f, notesTop - 17f, "线上可分配净额＝本运营商清分收入＋其他清分方收入。", FS_T2);
        drawText(page, font, 42f, notesTop - 37f, "本方电费收入＋本方服务费收入＋本方占位费收入＝本运营商线上结算金额。", FS_T2);
        drawText(page, font, 42f, notesTop - 57f, "机构用户消费不在本表线上清分范围内，列入附件三。", 8.6f);
    }

    /** 返回抬头小表底线 y（续页无抬头小表，返回 0 且调用方不使用）。 */
    private static float drawHeader(PdfPage page, PdfFont font, Page2Data p, boolean cont) {
        drawTitleHeader(page, font, 184f, "附件一  场站线上清分汇总", 239f, "本运营商各场站线上清分收入");
        if (cont) return 0f;
        return drawOperatorBox(page, font, p.operatorName, p.period, null, null);
    }

    /** 新页骨架：加页 + 页眉（续页样式）+ 页脚；节标题/表头带由续页方法补画。 */
    private static PdfPage newPage(PdfDocument pdf, PdfFont font, Page2Data p, String settlementNo) {
        PdfPage page = pdf.addNewPage();
        drawHeader(page, font, p, true);
        drawFooterBase(page, font, settlementNo);
        return page;
    }

    /** 行数据 → 各列文本（须每次返回新数组，合计行会改写首列）。 */
    @FunctionalInterface
    private interface RowCells<T> {
        String[] cells(T row);
    }

    /**
     * 通用行表（表 1 / 表 2 共用）：表头带 + 数据行 + 合计行。
     * 最后一行视为合计：首列固定画"合计"，其余列取该行数据；数据行不足 MIN_DATA_ROWS 补空行占位（只画格子）。
     * 列文本按列宽折行撑开行高，放不下换新页续写（带"（续）"节标题 + 表头带）。返回表底线 y。
     */
    private static <T> float drawRows(PdfDocument pdf, PdfFont font, PdfFont boldFont, PdfPage page,
                                      List<T> rows, float top, float rowH, float[] xs, String[] header,
                                      float fs, String secTitle, Page2Data p, String settlementNo,
                                      RowCells<T> toCells) {
        if (rows == null || rows.isEmpty()) {
            return top;
        }

        float right = xs[xs.length - 1];
        float[] w = colWidths(xs);
        float y = drawHeaderBand(page, boldFont, fs, top, rowH, xs, header);

        int totalIndex = rows.size() - 1;
        int n = Math.max(totalIndex, MIN_DATA_ROWS);

        for (int i = 0; i < n; i++) {
            T row = i < totalIndex ? rows.get(i) : null;
            String[] cells = row == null ? null : toCells.cells(row);
            float rh = cells == null ? rowH : rowLines(font, fs, w, cells) * rowH;
            if (y - rh < MIN_Y) {
                page = newContPage(pdf, font, boldFont, p, settlementNo, secTitle, fs, xs, header, rowH);
                y = CONT_TOP - 10f - rowH;
            }
            if (cells != null) {
                drawCells(page, font, xs, w, y, rh, cells, fs, rowH);
            }
            hLine(page, xs[0], right, y - rh);
            vLines(page, xs, y - rh, y);
            y -= rh;
        }

        String[] total = toCells.cells(rows.get(totalIndex));
        total[0] = "合计";
        float trh = rowLines(boldFont, fs, w, total) * rowH;
        if (y - trh < MIN_Y) {
            page = newContPage(pdf, font, boldFont, p, settlementNo, secTitle, fs, xs, header, rowH);
            y = CONT_TOP - 10f - rowH;
        }
        hLineBold(page, xs[0], right, y);
        drawCells(page, boldFont, xs, w, y, trh, total, fs, rowH);
        hLineBold(page, xs[0], right, y - trh);
        vLines(page, xs, y - trh, y);

        return y - trh;
    }

    /** 画一行各格：首列左内边距 6，其余 4。 */
    private static void drawCells(PdfPage page, PdfFont font, float[] xs, float[] w,
                                  float y, float h, String[] cells, float fs, float rowH) {
        for (int c = 0; c < cells.length; c++) {
            drawCellText(page, font, xs[c] + (c == 0 ? 6f : 4f), y, h, w[c], cells[c], fs, rowH);
        }
    }

    private static PdfPage newContPage(PdfDocument pdf, PdfFont font, PdfFont boldFont, Page2Data p,
                                       String settlementNo, String secTitle, float fs,
                                       float[] xs, String[] header, float rowH) {
        PdfPage page = newPage(pdf, font, p, settlementNo);
        drawText(page, font, 42f, CONT_TOP, secTitle, FS_SEC);
        drawHeaderBand(page, boldFont, fs, CONT_TOP - 10f, rowH, xs, header);
        return page;
    }
}