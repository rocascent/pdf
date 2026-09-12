package com.ysh.util.pdf.exporter;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.Page4Data;
import com.ysh.util.pdf.dto.Page4Table1Row;
import com.ysh.util.pdf.dto.Page4Table2Group;

import java.util.ArrayList;
import java.util.List;

import static com.ysh.util.pdf.exporter.PdfDrawer.*;

/**
 * 第 4 页（附件三 机构用户充电消费统计）：整页自画，不依赖模板。
 *
 * 表 1 固定 4 行，按 index 顺序：0=普通机构, 1=运营商机构, 2=泰达电力机构, 3=合计
 * 不依赖 label 字符串匹配，顺序即语义。
 */
final class Page4Drawer {

    // 版式常量（L/R/FS/FS_*/MIN_Y/CONT_TOP/页眉页脚）统一放 PdfDrawer；此处只留本页特有
    private static final float T1_HEAD_H = 35f;      // 表 1 表头带高度
    private static final float T1_ROW_H = 29f;       // 表 1 基础行高
    private static final float T2_ROW_H = 31f;       // 表 2 基础行高（子行）
    private static final float NOTES_H = 102f;       // 表 2 下方 5 行注释块高度（换页判断用）

    /** 表 1 固定 4 行，按 index 顺序 */
    private static final int T1_NORMAL = 0;
    private static final int T1_OPERATOR = 1;
    private static final int T1_TAIDA = 2;
    private static final int T1_TOTAL = 3;
    private static final int T1_ROWS = 4;

    /** 表 2 每个场站固定 3 个机构类型子行 */
    private static final String[] T2_TYPES = {"普通机构", "运营商机构", "泰达电力机构"};

    /** 表 2 表头（首列"场站名称 / 编号"显式换行；带高按各列折行数撑开，不写死） */
    private static final String[] T2_HEADERS = {
            "场站名称\n/ 编号", "机构用户类型", "电费收入", "服务费收入", "充电消费收入", "占位费收入", "消费收入合计"
    };

    private Page4Drawer() {
    }

    static void fill(PdfDocument pdf, PdfFont font, PdfFont boldFont,
                     Page4Data p, String settlementNo) {
        if (p == null) return;

        PdfPage page = pdf.addNewPage();
        float boxBottom = drawHeader(page, font, p, false);   // 抬头小表底线（长文本会撑高）
        drawFooterBase(page, font, settlementNo);

        float tbl1Bottom = drawTable1(page, font, boldFont, p.table1, boxBottom);
        float bottom = drawTable2(pdf, font, boldFont, page, p.table2, p.table2Total, settlementNo, tbl1Bottom);
        page = pdf.getLastPage();

        // 注释块（5 行）放不下则另起新页，避免压到页脚/出纸
        if (bottom - NOTES_H < MIN_Y) {
            page = newPage(pdf, font, settlementNo);
            bottom = CONT_TOP;
        }
        drawNotes(page, font, boldFont, bottom);
    }

    /**
     * 页眉（每页都画）：大标题区；抬头小表（运营商/期间单行）仅首页有（cont=false），续增页不画。
     */
    /** 返回抬头小表底线 y（续页无抬头小表，返回 0 且调用方不使用）。 */
    private static float drawHeader(PdfPage page, PdfFont font, Page4Data p, boolean cont) {
        drawTitleHeader(page, font, 165f, "附件三  机构用户充电消费统计", 271f, "线下结算业务");
        if (cont) return 0f;
        return drawOperatorBox(page, font, p.operatorName, p.period, null, null);
    }

    /** 新页骨架：加页 + 页眉（续页样式）+ 页脚；节标题/表头带由续页方法补画。 */
    private static PdfPage newPage(PdfDocument pdf, PdfFont font, String settlementNo) {
        PdfPage page = pdf.addNewPage();
        drawHeader(page, font, null, true);
        drawFooterBase(page, font, settlementNo);
        return page;
    }

    /**
     * 表 1：三类机构消费汇总
     * 按 index 顺序取值，不依赖 label 字符串匹配：
     *   index 0 = 普通机构
     *   index 1 = 运营商机构
     *   index 2 = 泰达电力机构
     *   index 3 = 合计（加粗）
     */
    private static float drawTable1(PdfPage page, PdfFont font, PdfFont boldFont, List<Page4Table1Row> rows,
                                    float boxBottom) {
        float[] xs = {L, 164f, 222f, 304f, 387f, 467f, R};
        float[] w = colWidths(xs);

        // 标题 + 表头带（标题 = 抬头小表底线 - 24，表头顶线 = -34，表头带高 35）
        float headerTop = boxBottom - 34f;
        float headerBottom = headerTop - T1_HEAD_H;
        drawText(page, font, 42f, boxBottom - 24f, "一、三类机构用户消费汇总", FS_SEC);
        hLineBold(page, L, R, headerTop);
        hLine(page, L, R, headerBottom);
        drawText(page, boldFont, xs[0] + 6f, headerTop - 20f, "机构用户类型", FS);
        drawText(page, boldFont, xs[1] + 6f, headerTop - 14f, "订单数", FS);
        drawText(page, boldFont, xs[1] + 6f, headerTop - 26f, "（笔）", FS);
        drawText(page, boldFont, xs[2] + 6f, headerTop - 14f, "充电量", FS);
        drawText(page, boldFont, xs[2] + 6f, headerTop - 26f, "（kWh）", FS);
        drawText(page, boldFont, xs[3] + 6f, headerTop - 20f, "充电消费收入", FS);
        drawText(page, boldFont, xs[4] + 6f, headerTop - 20f, "占位费收入", FS);
        drawText(page, boldFont, xs[5] + 6f, headerTop - 20f, "消费收入合计", FS);

        // 按 index 画，完全不用 label；行高按折行数撑开
        List<Page4Table1Row> fixed = padTable1(rows);
        float y = headerBottom;
        for (int i = 0; i < T1_ROWS; i++) {
            Page4Table1Row r = fixed.get(i);
            boolean isTotal = (i == T1_TOTAL);
            PdfFont f = isTotal ? boldFont : font;
            // 行头写死 + 数据列
            String[] cells = {getLabel(i), r.orderCount, r.chargeQty, r.chargeIncome, r.parkIncome, r.totalIncome};
            float rh = rowLines(f, FS, w, cells) * T1_ROW_H;

            if (isTotal) {
                hLineBold(page, L, R, y);  // 合计行上边强调线
            }
            for (int c = 0; c < cells.length; c++) {
                drawCellText(page, f, xs[c] + (c == 0 ? 6f : 4f), y, rh, w[c], cells[c], FS, T1_ROW_H);
            }

            y -= rh;
            if (isTotal) {
                hLineBold(page, L, R, y);  // 表 1 底线（外框底）
            } else {
                hLine(page, L, R, y);       // 普通行分隔线
            }
        }

        // 整表竖线（表头顶线 → 表底 y）
        vLines(page, xs, y, headerTop);
        return y;
    }

    /** 按 index 返回固定 label */
    private static String getLabel(int index) {
        switch (index) {
            case T1_NORMAL:   return "普通机构";
            case T1_OPERATOR: return "运营商机构";
            case T1_TAIDA:    return "泰达电力机构";
            case T1_TOTAL:    return "合计";
            default:          return "";
        }
    }

    /** 补齐/截断为固定 4 行，空行用默认值填充 */
    private static List<Page4Table1Row> padTable1(List<Page4Table1Row> rows) {
        List<Page4Table1Row> result = new ArrayList<>(T1_ROWS);
        for (int i = 0; i < T1_ROWS; i++) {
            result.add(new Page4Table1Row("0", "0.00", "0.00", "0.00", "0.00"));
        }
        if (rows != null) {
            for (int i = 0; i < Math.min(rows.size(), T1_ROWS); i++) {
                if (rows.get(i) != null) {
                    result.set(i, rows.get(i));
                }
            }
        }
        return result;
    }

    /**
     * 表 2：各场站机构消费明细
     */
    private static float drawTable2(PdfDocument pdf, PdfFont font, PdfFont boldFont, PdfPage page,
                                    List<Page4Table2Group> groups, String[] total, String settlementNo,
                                    float tbl1Bottom) {
        float[] xs = {L, 120f, 222f, 287f, 352f, 419f, 485f, R};
        float[] w = colWidths(xs);

        // 表 1 变高时整块跟着下移（标题 = 表 1 底线 - 23，表头顶线 = 表 1 底线 - 34）
        drawText(page, font, 42f, tbl1Bottom - 23f, "二、各场站机构用户消费明细", FS_SEC);
        float y = drawHeaderBand(page, boldFont, tbl1Bottom - 34f, xs);

        for (Page4Table2Group g : padGroups(groups)) {
            float groupH = groupHeight(g, font, w);
            if (y - groupH < MIN_Y) {
                page = newContPage(pdf, font, boldFont, settlementNo, xs);
                y = CONT_TOP - 10f - headerBandHeight(boldFont, xs);
            }

            vLines(page, xs, y - groupH, y);

            // 场站名：纵跨整组垂直居中
            drawCellText(page, font, xs[0] + 6f, y, groupH, w[0], g.station, FS, T2_ROW_H);

            // 3 个子行：组内富余高度平均摊到三行（三个格子等高），且每行不低于自身折行所需高度
            float[] needH = new float[3];
            float need = 0f;
            for (int i = 0; i < 3; i++) {
                needH[i] = rowHeight(g, i, font, w);
                need += needH[i];
            }
            float extra = (groupH - need) / 3f;

            float yy = y;
            for (int i = 0; i < 3; i++) {
                float rh = needH[i] + extra;
                drawCellText(page, font, xs[1] + 6f, yy, rh, w[1], T2_TYPES[i], FS, T2_ROW_H);
                drawCellText(page, font, xs[2] + 4f, yy, rh, w[2], at(g.elecFee, i), FS, T2_ROW_H);
                drawCellText(page, font, xs[3] + 4f, yy, rh, w[3], at(g.servFee, i), FS, T2_ROW_H);
                drawCellText(page, font, xs[4] + 4f, yy, rh, w[4], at(g.consume, i), FS, T2_ROW_H);
                drawCellText(page, font, xs[5] + 4f, yy, rh, w[5], at(g.tmoutFee, i), FS, T2_ROW_H);
                drawCellText(page, font, xs[6] + 4f, yy, rh, w[6], at(g.total, i), FS, T2_ROW_H);
                yy -= rh;
                if (i < 2) hLine(page, xs[1], R, yy);
            }
            hLine(page, L, R, y - groupH);
            y -= groupH;
        }

        // 合计行：行高按折行数撑开
        float trh = totalRowHeight(total, boldFont, w);
        if (y - trh < MIN_Y) {
            page = newContPage(pdf, font, boldFont, settlementNo, xs);
            y = CONT_TOP - 10f - headerBandHeight(boldFont, xs);
        }
        hLineBold(page, L, R, y);
        drawCellText(page, boldFont, xs[0] + 6f, y, trh, w[0], "合计", FS, T2_ROW_H);
        if (total != null) {
            for (int c = 0; c < 5 && c < total.length; c++) {
                drawCellText(page, boldFont, xs[c + 2] + 4f, y, trh, w[c + 2], total[c], FS, T2_ROW_H);
            }
        }
        vLines(page, xs, y - trh, y);
        hLineBold(page, L, R, y - trh);
        return y - trh;
    }

    /**
     * 一个场站组高度 = max(3 个子行高度之和, 场站名列折行高度)。
     * 场站名纵跨整组，名字过长时必须把组撑高，否则文字会溢到表头带和下一组上。
     */
    private static float groupHeight(Page4Table2Group g, PdfFont font, float[] w) {
        float h = 0;
        for (int i = 0; i < 3; i++) h += rowHeight(g, i, font, w);
        float stationH = rowLines(font, FS, new float[]{w[0]}, g.station) * T2_ROW_H;
        return Math.max(h, stationH);
    }

    /** 场站组第 i 子行高度 = 该子行各格折行后的最大行数 × T2_ROW_H。 */
    private static float rowHeight(Page4Table2Group g, int i, PdfFont font, float[] w) {
        return rowLines(font, FS, new float[]{w[1], w[2], w[3], w[4], w[5], w[6]},
                T2_TYPES[i], at(g.elecFee, i), at(g.servFee, i),
                at(g.consume, i), at(g.tmoutFee, i), at(g.total, i)) * T2_ROW_H;
    }

    /** 合计行高度 = 各格折行后的最大行数 × T2_ROW_H。 */
    private static float totalRowHeight(String[] total, PdfFont font, float[] w) {
        String[] texts = {"合计", at(total, 0), at(total, 1), at(total, 2), at(total, 3), at(total, 4)};
        return rowLines(font, FS, new float[]{w[0], w[2], w[3], w[4], w[5], w[6]}, texts) * T2_ROW_H;
    }

    /** 场站组不足 2 个补空组 */
    private static List<Page4Table2Group> padGroups(List<Page4Table2Group> groups) {
        List<Page4Table2Group> list = new ArrayList<>();
        if (groups != null) {
            for (Page4Table2Group g : groups) {
                if (g != null) list.add(g);
            }
        }
        while (list.size() < 2) list.add(new Page4Table2Group());
        return list;
    }

    /** 续页 */
    private static PdfPage newContPage(PdfDocument pdf, PdfFont font, PdfFont boldFont,
                                       String settlementNo, float[] xs) {
        PdfPage page = newPage(pdf, font, settlementNo);
        drawText(page, font, 42f, CONT_TOP, "二、各场站机构用户消费明细（续）", FS_SEC);
        drawHeaderBand(page, boldFont, CONT_TOP - 10f, xs);
        return page;
    }

    /** 表 2 表头带高度：按各列折行数撑开（首列两行 → 2×T2_ROW_H）；续页布置首行时与 drawHeaderBand 保持一致。 */
    private static float headerBandHeight(PdfFont boldFont, float[] xs) {
        return rowLines(boldFont, FS, colWidths(xs), T2_HEADERS) * T2_ROW_H;
    }

    /** 画表 2 表头带（高度随折行数变化），返回底线 y */
    private static float drawHeaderBand(PdfPage page, PdfFont boldFont, float top, float[] xs) {
        float[] w = colWidths(xs);
        float bandH = headerBandHeight(boldFont, xs);
        hLineBold(page, L, R, top);
        for (int c = 0; c < T2_HEADERS.length; c++) {
            drawCellText(page, boldFont, xs[c] + 6f, top, bandH, w[c], T2_HEADERS[c], FS, T2_ROW_H);
        }
        float y = top - bandH;
        hLine(page, L, R, y);
        vLines(page, xs, y, top);
        return y;
    }

    /** 统计口径注释 */
    private static void drawNotes(PdfPage page, PdfFont font, PdfFont boldFont, float bottom) {
        drawText(page, font, 42f, bottom - 23f, "三、统计口径", FS_SEC);
        drawText(page, font, 42f, bottom - 43f,
                "电费收入＝电费金额－电费优惠；服务费收入＝服务费金额－服务费优惠；占位费收入＝占位费金额－占位费优惠。", FS);
        drawText(page, font, 42f, bottom - 62f,
                "充电消费收入＝电费收入＋服务费收入；消费收入合计＝充电消费收入＋占位费收入。", FS);
        drawText(page, boldFont, 42f, bottom - 82f,
                "本表统计机构用户充电消费形成的收入，属于线下结算业务，不计入平台线上清分金额及可提现余额。", FS);
        drawText(page, font, 42f, bottom - 102f,
                "三类机构用户按订单分类分别统计；分类汇总与场站明细合计一致，同一业务不重复计入。", FS);
    }

    /** 取数组第 i 个元素，越界/数组为 null 返回 null */
    private static String at(String[] arr, int i) {
        return arr != null && i < arr.length ? arr[i] : null;
    }
}
