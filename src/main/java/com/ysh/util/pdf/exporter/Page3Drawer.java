package com.ysh.util.pdf.exporter;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.Page3Table2Group;
import com.ysh.util.pdf.dto.Page3Data;
import com.ysh.util.pdf.dto.Page3Table1Row;

import java.util.ArrayList;
import java.util.List;

import static com.ysh.util.pdf.exporter.PdfDrawer.*;

/**
 * 第 3 页（附件二 场站结算明细）：整页自画，不依赖模板。
 * 坐标与字号抄自 api_out_dump3.txt（与页 2 同源的实测基准）。
 *
 * 结构：抬头小表（两行）+ 占位计费规则行 + 表 1（核算项目，固定 6 行）
 * + 表 2（各方清分收入，每个费用项目一组、N 个清分方子行，有几行画几行）+ 汇总两行 + 注释。
 * 表 2 组数与子行数可变：整组放不下当前页则换新页续写（组不可拆），Word 式延续，
 * 每页都带完整页眉页脚；汇总区固定跟在最后一个组之后。
 */
final class Page3Drawer {

    // 版式常量（L/R/FS/FS_*/MIN_Y/CONT_TOP/页眉页脚）统一放 PdfDrawer；此处只留本页特有
    private static final float ROW_H = 25f;         // 表格行高
    private static final float RULE_LINE_H = 27f;   // 占位计费规则行单行高度

    /** 表 1 固定 6 行的核算项目（label 写死），数据按下标顺序取，缺行补 "0.00"。 */
    private static final String[] T1_LABELS = {
            "费用金额", "减：优惠金额", "减：支付通道费", "减：平台服务费", "加：调整额", "可分配净额"
    };
    private static final int T1_ROWS = 6;
    private static final int T1_NET = 5;    // 可分配净额行下标（加粗 + 上边强调线）

    private Page3Drawer() {
    }

    /** 画整页（表 2 过长时自动跨多页，每页都带完整页眉页脚，Word 式延续）。boldFont 用于表头。 */
    static void fill(PdfDocument pdf, PdfFont font, PdfFont boldFont,
                     Page3Data p, String settlementNo) {
        if (p == null) return;

        PdfPage page = pdf.addNewPage();
        float boxBottom = drawHeader(page, font, p, false);   // 抬头小表底线（长文本会撑高）
        drawFooterBase(page, font, settlementNo);

        // 占位计费规则行：顶线 = 抬头小表底线 - 8；行数不限，放不完自动分页续写
        float ruleBottom = drawRuleBox(pdf, font, page, boxBottom - 8f, p.chargeRule, settlementNo);
        page = pdf.getLastPage();

        // 规则框恰好排到页底时，「一、」整块（表头带 + 6 行 + 注释）另起新页，避免掉出页面
        if (ruleBottom - (34f + ROW_H * (1 + T1_ROWS) + 17f) < MIN_Y) {
            page = newPage(pdf, font, settlementNo);
            ruleBottom = CONT_TOP + 24f;
        }

        // 一、线上可分配净额计算：标题基线 = 规则行底线 - 24，表顶线 = 规则行底线 - 34，行高 25
        drawText(page, font, 42f, ruleBottom - 24f, "一、线上可分配净额计算", FS_SEC);
        float[] x1 = {L, 185f, 277f, 369f, 461f, R};
        String[] h1 = {"核算项目", "电费收入", "服务费收入", "占位费收入", "合计"};
        float y = drawTable1(page, font, boldFont, p.table1, ruleBottom - 34f, x1, h1);

        // 表 1 注释（底线 -17；放不下则另起新页，避免压到页脚/出纸）
        if (y - 17f < MIN_Y) {
            page = newPage(pdf, font, settlementNo);
            y = CONT_TOP + 17f;
        }
        drawText(page, font, 42f, y - 17f,
                "各项可分配净额＝费用金额－优惠金额－支付通道费－平台服务费＋调整额。调整额按正负值计入。", FS);

        // 二、各方清分收入：标题 = 注释下 26（模板 414→388），表顶线 = 标题 - 10
        float t2 = y - 17f - 26f;
        float[] x2 = {L, 115f, 223f, 367f, 442f, R};
        String[] h2 = {"费用项目", "可分配净额", "清分方名称", "清分比例", "清分收入"};
        y = drawTable2(pdf, font, boldFont, page, p.feeGroups, p.totalIncome, p.operatorIncome,
                t2, x2, h2, settlementNo);
        page = pdf.getLastPage();          // 表 2 可能内部换过页，注释必须跟到最后一页

        // 表 2 注释（底线 -17 / -36；第二行 8.6pt，与页 2 小字注释规律一致）；放不下则另起新页
        float notesTop = y;
        if (notesTop - 36f < MIN_Y) {
            page = newPage(pdf, font, settlementNo);
            notesTop = CONT_TOP;
        }
        drawText(page, font, 42f, notesTop - 17f,
                "各项清分收入＝该项可分配净额×该清分方对应比例。各方清分收入合计与可分配净额一致。", FS);
        drawText(page, font, 42f, notesTop - 36f, "本表仅列线上清分业务，机构用户消费另列附件三。", 8.6f);
    }

    /**
     * 页眉（每页都画）：大标题区；抬头小表（运营商/期间 + 场站/编号两行）仅首页有（cont=false），续页不画。
     * 返回抬头小表底线 y（首页，供规则行/「一、二」布局下移）；续页返回 0，调用方不使用。
     */
    private static float drawHeader(PdfPage page, PdfFont font, Page3Data p, boolean cont) {
        drawTitleHeader(page, font, 203f, "附件二  场站结算明细", 239f, "线上费用计算及各方清分收入");
        if (cont) return 0f;
        return drawOperatorBox(page, font, p.operatorName, p.period, p.stationName, p.stationCode);
    }

    /**
     * 占位计费规则行：顶线 = ruleTop，按显式 \n + 可用宽度折行，行数不限（不截断）。
     * 一页放不完则自动分页续写（续页重复页眉页脚，左列标"（续）"）。
     * 返回规则框底线 y：当前页已是承载最后一段规则的页，调用方据此继续向下布局。
     */
    private static float drawRuleBox(PdfDocument pdf, PdfFont font, PdfPage page, float ruleTop,
                                     String chargeRule, String settlementNo) {
        float[] w = colWidths(new float[]{L, 132f, R});
        List<String> rules = wrapLines(chargeRule, font, FS, w[1]);

        boolean cont = false;
        float top = ruleTop;
        int i = 0;
        while (true) {
            int fit = Math.max(1, (int) ((top - MIN_Y) / RULE_LINE_H));   // 本页可容纳行数
            int n = Math.min(fit, rules.size() - i);
            float bottom = top - n * RULE_LINE_H;

            hLineBold(page, L, R, top);        // 规则行外框（顶）
            hLineBold(page, L, R, bottom);     // 规则行外框（底）
            vLineBold(page, L, bottom, top);
            vLine(page, 132f, bottom, top);
            vLineBold(page, R, bottom, top);
            drawText(page, font, 48f, top - 16f, cont ? "占位计费规则（续）" : "占位计费规则", FS);
            for (int k = 0; k < n; k++) {
                drawText(page, font, 136f, top - k * RULE_LINE_H - 16f, rules.get(i + k), FS);
            }

            i += n;
            if (i >= rules.size()) return bottom;

            page = newPage(pdf, font, settlementNo);   // 续页：页眉页脚 + 规则框接着排
            cont = true;
            top = CONT_TOP;
        }
    }

    /** 新页骨架：加页 + 页眉（续页样式）+ 页脚；节标题/表头带由续页方法补画。 */
    private static PdfPage newPage(PdfDocument pdf, PdfFont font, String settlementNo) {
        PdfPage page = pdf.addNewPage();
        drawHeader(page, font, null, true);
        drawFooterBase(page, font, settlementNo);
        return page;
    }

    /** 表 1：表头带（加粗）+ 固定 6 行数据（label 写死，按 index 顺序取值，缺行补 "0.00"）。返回该表底线 y。 */
    private static float drawTable1(PdfPage page, PdfFont font, PdfFont boldFont,
                                    List<Page3Table1Row> rows, float top, float[] xs, String[] header) {
        float right = xs[xs.length - 1];
        float[] w = colWidths(xs);

        // 表头带（公共）
        float y = drawHeaderBand(page, boldFont, FS, top, ROW_H, xs, header);

        // 数据行：固定 6 行（label 写死，数据按下标顺序取，缺行补 "0.00"）；行高按最大行数撑开
        List<Page3Table1Row> fixed = padTable1(rows);
        for (int i = 0; i < T1_ROWS; i++) {
            Page3Table1Row r = fixed.get(i);
            boolean isNet = (i == T1_NET);
            String[] vals = {r.elecIncome, r.serviceIncome, r.parkIncome, r.totalIncome};
            PdfFont f = isNet ? boldFont : font;
            float rh = rowLines(f, FS, w, T1_LABELS[i], vals[0], vals[1], vals[2], vals[3]) * ROW_H;
            if (isNet) {
                hLineBold(page, L, right, y);   // 可分配净额行上边强调线（模板 0.35+0.70 叠加）
            }
            drawCellText(page, f, xs[0] + 6f, y, rh, w[0], T1_LABELS[i], FS, ROW_H);
            for (int c = 0; c < vals.length; c++) {
                drawCellText(page, f, xs[c + 1] + 4f, y, rh, w[c + 1], vals[c], FS, ROW_H);
            }
            if (isNet) {
                hLineBold(page, L, right, y - rh);   // 表底线（外框底）
            } else {
                hLine(page, L, right, y - rh);
            }
            vLines(page, xs, y - rh, y);
            y -= rh;
        }
        return y;
    }

    /** 补齐/截断为固定 T1_ROWS 行，缺行用默认值填充。 */
    private static List<Page3Table1Row> padTable1(List<Page3Table1Row> rows) {
        List<Page3Table1Row> result = new ArrayList<>(T1_ROWS);
        for (int i = 0; i < T1_ROWS; i++) {
            result.add(new Page3Table1Row("0.00", "0.00", "0.00", "0.00"));
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
     * 表 2：每个费用项目一组，组内子行数 = 清分方数据长度（至少 1 行，不补空行，组高 N×ROW_H）。
     * 费用项目/可分配净额纵跨组内全部子行（组内分隔线只画 223~553）；
     * 整组放不下当前页则换新页续写（组不可拆）；组后接 8pt 间隙 + 汇总两行。
     * 返回汇总区底线 y。
     */
    private static float drawTable2(PdfDocument pdf, PdfFont font, PdfFont boldFont, PdfPage page,
                                    List<Page3Table2Group> groups, String totalIncome, String operatorIncome,
                                    float titleBase, float[] xs, String[] header, String settlementNo) {
        float right = xs[xs.length - 1];
        float[] w = colWidths(xs);

        // 该节首次出现：标题 + 表头带 + 首个内容块放不下当前页时整块挪到新页，避免页底只剩孤立表头
        float y;
        if (titleBase - 10f - ROW_H - firstBlockHeight(groups, font, w) < MIN_Y) {
            page = newSectionPage(pdf, font, boldFont, settlementNo, xs, header);
            y = CONT_TOP - 10f - ROW_H;
        } else {
            drawText(page, font, 42f, titleBase, "二、各方清分收入", FS_SEC);
            y = drawHeaderBand(page, boldFont, FS, titleBase - 10f, ROW_H, xs, header);
        }

        // 数据组：整组放不下换页续写（组高 = 各子行高度之和，子行有 \n 时撑开）
        if (groups != null) {
            for (Page3Table2Group g : groups) {
                if (g == null) continue;
                float groupH = groupHeight(g, font, w);
                if (y - groupH < MIN_Y) {           // 组不可拆：整组搬到新页
                    page = newContPage(pdf, font, boldFont, settlementNo, xs, header);
                    y = CONT_TOP - 10f - ROW_H;
                }

                // 组竖线贯穿整组（左右外框加粗）
                vLines(page, xs, y - groupH, y);

                // 费用项目 / 可分配净额：纵跨整组垂直居中
                drawCellText(page, font, xs[0] + 6f, y, groupH, w[0], g.item, FS, ROW_H);
                drawCellText(page, font, xs[1] + 4f, y, groupH, w[1], g.netAmount, FS, ROW_H);

                // 子行 0..subRows-1（行高按各自折行数；组内分隔线只画 223~553，不横穿费用项目/净额列）
                int subRows = subRowCount(g);
                float yy = y;
                for (int i = 0; i < subRows; i++) {
                    float rh = subRowHeight(g, i, font, w);
                    drawCellText(page, font, xs[2] + 4f, yy, rh, w[2], at(g.party, i), FS, ROW_H);
                    drawCellText(page, font, xs[3] + 4f, yy, rh, w[3], at(g.ratio, i), FS, ROW_H);
                    drawCellText(page, font, xs[4] + 4f, yy, rh, w[4], at(g.income, i), FS, ROW_H);
                    yy -= rh;
                    if (i < subRows - 1) {
                        hLine(page, xs[2], right, yy);
                    }
                }

                // 组底全宽横线
                hLine(page, L, right, y - groupH);
                y -= groupH;
            }
        }

        // 汇总区：8pt 间隙 + 两行（行高按最大行数撑开；竖线仅 42/381/553）；
        // 放不下换页时，新页不画节标题/表头带（此时只剩汇总两行，表头对不上内容）
        float sumRowH = 26f;
        float sumX = 381f;
        float[] sumW = {sumX - L - CELL_PAD, R - sumX - CELL_PAD};
        float sumH1 = rowLines(boldFont, FS, sumW, "各方线上清分收入合计", totalIncome) * sumRowH;
        float sumH2 = rowLines(boldFont, FS, sumW, "本运营商线上清分收入", operatorIncome) * sumRowH;
        float sumTop;
        if (y - 8f - sumH1 - sumH2 < MIN_Y) {
            page = newPage(pdf, font, settlementNo);
            sumTop = CONT_TOP - 10f;
        } else {
            sumTop = y - 8f;
        }
        float sumSplit = sumTop - sumH1;
        float sumBottom = sumSplit - sumH2;
        hLineBold(page, L, right, sumTop);   // 汇总区外框（顶）
        vLineBold(page, L, sumBottom, sumTop);
        vLine(page, sumX, sumBottom, sumTop);
        vLineBold(page, R, sumBottom, sumTop);

        drawCellText(page, boldFont, L + 6f, sumTop, sumH1, sumW[0], "各方线上清分收入合计", FS, sumRowH);
        drawCellText(page, boldFont, sumX + 4f, sumTop, sumH1, sumW[1], totalIncome, FS, sumRowH);
        hLineBold(page, L, right, sumSplit);   // 本运营商行上边强调线（模板实测粗线）

        drawCellText(page, boldFont, L + 6f, sumSplit, sumH2, sumW[0], "本运营商线上清分收入", FS, sumRowH);
        drawCellText(page, boldFont, sumX + 4f, sumSplit, sumH2, sumW[1], operatorIncome, FS, sumRowH);
        hLineBold(page, L, right, sumBottom);   // 表 2 底线（外框底）
        return sumBottom;
    }

    /** 该节首次出现但当前页放不下时的新页：页眉页脚 + 节标题"二、各方清分收入"（不加"续"）+ 表头带。 */
    private static PdfPage newSectionPage(PdfDocument pdf, PdfFont font, PdfFont boldFont,
                                          String settlementNo, float[] xs, String[] header) {
        PdfPage page = newPage(pdf, font, settlementNo);
        drawText(page, font, 42f, CONT_TOP, "二、各方清分收入", FS_SEC);
        drawHeaderBand(page, boldFont, FS, CONT_TOP - 10f, ROW_H, xs, header);
        return page;
    }

    /** 续页：新页 + 页眉页脚 + 节标题"二、各方清分收入（续）" + 表头带；返回新页。 */
    private static PdfPage newContPage(PdfDocument pdf, PdfFont font, PdfFont boldFont,
                                       String settlementNo, float[] xs, String[] header) {
        PdfPage page = newPage(pdf, font, settlementNo);
        drawText(page, font, 42f, CONT_TOP, "二、各方清分收入（续）", FS_SEC);
        drawHeaderBand(page, boldFont, FS, CONT_TOP - 10f, ROW_H, xs, header);
        return page;
    }

    /** 节内首个内容块高度：有数据组取首个非空组的高度，否则取汇总区高度（8 间隙 + 2×26 行）。 */
    private static float firstBlockHeight(List<Page3Table2Group> groups, PdfFont font, float[] w) {
        if (groups != null) {
            for (Page3Table2Group g : groups) {
                if (g != null) return groupHeight(g, font, w);
            }
        }
        return 8f + 26f * 2f;
    }

    /** 组高 = 各子行高度之和（子行折行时更高）。 */
    private static float groupHeight(Page3Table2Group g, PdfFont font, float[] w) {
        int n = subRowCount(g);
        float h = 0;
        for (int i = 0; i < n; i++) h += subRowHeight(g, i, font, w);
        return h;
    }

    /** 子行 i 的高度 = 该子行各格折行后的最大行数 × ROW_H。 */
    private static float subRowHeight(Page3Table2Group g, int i, PdfFont font, float[] w) {
        return rowLines(font, FS, new float[]{w[2], w[3], w[4]},
                at(g.party, i), at(g.ratio, i), at(g.income, i)) * ROW_H;
    }

    /** 组内子行数 = 三个数组最大长度，至少 1 行（保住费用项目/可分配净额），有几行画几行。 */
    private static int subRowCount(Page3Table2Group g) {
        int n = 0;
        if (g.party != null) n = Math.max(n, g.party.length);
        if (g.ratio != null) n = Math.max(n, g.ratio.length);
        if (g.income != null) n = Math.max(n, g.income.length);
        return Math.max(n, 1);
    }

    /** 取数组第 i 个元素，越界/数组为 null 返回 null（画空白）。 */
    private static String at(String[] arr, int i) {
        return arr != null && i < arr.length ? arr[i] : null;
    }
}
