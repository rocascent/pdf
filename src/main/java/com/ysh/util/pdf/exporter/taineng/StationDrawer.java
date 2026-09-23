package com.ysh.util.pdf.exporter.taineng;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.settlement.Page1Data;
import com.ysh.util.pdf.dto.settlement.StationData;

import static com.ysh.util.pdf.exporter.taineng.PdfDrawer.*;

/**
 * 泰能充站点页：单个充电站结算信息表（整页自画，固定行高，可跨页）。
 * <p>第 1 个站点节标题"三、各充电站清单"，第 2 个起"三、各充电站清单（续）"。
 * <p>固定手续费/费率、平台手续费 2 行为双行备注行（32），可提现费用行（36）通常单独续页。
 */
final class StationDrawer {

    private static final float STA_ROW = 30f;      // 普通行高
    private static final float STA_DOUBLE = 32f;   // 双行备注行行高
    private static final float STA_HEAD = 24f;     // 表头行高
    private static final float STA_LAST = 36f;     // 可提现费用行行高
    private static final float NOTE_LH = 14f;      // 备注列多行文字行距（模板实测 493→478）
    private static final float STA_LH = 16f;       // 内容列多行文字行距（折行时行高按此自适应）

    private static final String[] LABELS = {
            "电站名称", "电站地址", "电站类型", "平台手续费模式",
            "固定手续费/平台手续费费率", "月充电量(kWh)", "月充电电费(元)", "月充电服务费(元)",
            "普通机构充电量(kWh)", "普通机构充电电费(元)", "普通机构充电服务费(元)",
            "运营商机构充电量(kWh)", "运营商机构充电电费(元)", "运营商机构充电服务费(元)",
            "泰达电力机构充电量(kWh)", "泰达电力机构充电电费(元)", "泰达电力机构充电服务费(元)",
            "平台手续费(元)", "可提现费用(元)"};

    private static final boolean[] DOUBLE = new boolean[19];
    static { DOUBLE[4] = DOUBLE[17] = true; }

    private static final String[] NOTES = {
            null, null, null,
            "固定金额或服务费费率",
            "固定金额模式列示金额（元）；\n服务费费率模式列示费率（%）",
            "普通用户月充电电量合计", "普通用户月充电电费合计", "普通用户月充电服务费合计",
            "普通机构月充电电量合计", "普通机构月充电电费合计", "普通机构月充电服务费合计",
            "运营商机构月充电电量合计", "运营商机构月充电电费合计", "运营商机构月充电服务费合计",
            "泰达电力机构月充电电量合计", "泰达电力机构月充电电费合计", "泰达电力机构月充电服务费合计",
            "固定金额模式：按固定金额计收。\n费率模式：月充电服务费×平台手续费费率",
            "可提现费用合计"};

    private StationDrawer() {
    }

    /**
     * 画一个站点（每站最多 1 页起始，可跨页）。
     *
     * @param prevBottom   上一个内容画到的底线（本站之前），0 表示无
     * @param forceNewPage 强制从新页开始（第 1 个站点），否则空间足够时接续上一页
     * @return 本站画完后当前页内容底线，供下一站点接续判断
     */
    static float draw(PdfDocument pdf, PdfFont font, PdfFont bold,
                      Page1Data p1, StationData s, int idx,
                      float prevBottom, boolean forceNewPage) {
        if (s == null) s = new StationData();
        String secTitle = idx == 1 ? "三、各充电站清单" : "三、各充电站清单（续）";

        int n = LABELS.length;
        int lastIdx = n - 1;   // 可提现费用行索引

        String[] values = {s.stationName, s.stationAddress, s.stationType, s.feeMode,
                s.fixedFee != null ? s.fixedFee : s.feeRate,
                s.monthCharge, s.monthElecFee, s.monthServFee,
                s.normalCharge, s.normalElecFee, s.normalServFee,
                s.operatorCharge, s.operatorElecFee, s.operatorServFee,
                s.tedaCharge, s.tedaElecFee, s.tedaServFee,
                s.platformFee, s.withdrawable};

        float[] xs = {L, 231f, 350f, R};
        float[] w = colWidths(xs);

        // 行高自适应：基础(普通30/双行32/可提现36) 与 内容折行行数(内容列×16、备注列×14) 取大
        float[] rowH = new float[n];
        for (int i = 0; i < n; i++) {
            float base = (i == lastIdx) ? STA_LAST : (DOUBLE[i] ? STA_DOUBLE : STA_ROW);
            float need = Math.max(
                    lineCount(values[i], font, FS_ROW, w[1]) * STA_LH,
                    NOTES[i] != null ? lineCount(NOTES[i], font, FS_ROW, w[2]) * NOTE_LH : 0f);
            rowH[i] = Math.max(base, need);
        }

        PdfCanvas cv;
        float curTop;
        // 接续条件：上一内容下方剩余空间能放下 标题块 + 表头 + 2 行
        boolean samePage = !forceNewPage
                && prevBottom - MIN_Y >= TITLE_BLOCK + STA_HEAD + 2 * STA_ROW;
        if (!samePage) {
            // 新页：页眉 + 节标题在模板固定位置
            PdfPage page = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
            cv = new PdfCanvas(page);
            drawHeader(cv, font, bold, p1);
            drawFooter(cv, font);
            drawText(cv, bold, L, 674.5f, secTitle, FS_SEC);
            curTop = 649f;
        } else {
            // 接续上一页：节标题画在上一个内容下方
            cv = new PdfCanvas(pdf.getLastPage());
            drawText(cv, bold, L, prevBottom - 16f, secTitle, FS_SEC);
            curTop = prevBottom - TITLE_BLOCK;
        }

        // 分段画（跨页；首段带表头，续页无表头无页眉）
        int segStart = 0, segIdx = 0;
        float segBottom = 0;
        while (segStart < n) {
            // 找当前页能放下的行数（首段带表头，续页无表头）
            float headH = (segIdx == 0) ? STA_HEAD : 0f;
            float yy = curTop - headH;
            int segEnd = segStart;
            while (segEnd < n) {
                float next = yy - rowH[segEnd];
                if (next < MIN_Y && segEnd > segStart) break;   // 至少画一行
                yy = next;
                segEnd++;
            }

            // 算行线
            float[] ry = new float[segEnd - segStart + 1];
            float t = curTop - headH;
            ry[0] = t;
            for (int j = segStart; j < segEnd; j++) { t -= rowH[j]; ry[j - segStart + 1] = t; }
            segBottom = t;
            drawTable(cv, xs, curTop, segBottom, ry);

            // 表头文本（首段，黑体加粗）
            if (segIdx == 0) {
                drawText(cv, bold, 95f, curTop - 12f, "项目", FS_ROW);
                drawText(cv, bold, 236f, curTop - 12f, "内容/金额", FS_ROW);
                drawText(cv, bold, 355f, curTop - 12f, "备注", FS_ROW);
            }

            // 数据行文本（标签列不折行，与模板单行顶格排一致）
            float rt = curTop - headH;
            for (int j = segStart; j < segEnd; j++) {
                drawCellText(cv, font, 95f, rt, rowH[j], Float.MAX_VALUE, LABELS[j], FS_ROW, STA_ROW);
                drawCellText(cv, font, 233f, rt, rowH[j], w[1], values[j], FS_ROW, STA_LH);
                if (NOTES[j] != null)
                    drawCellText(cv, font, 355f, rt, rowH[j], w[2],
                            NOTES[j], FS_ROW, NOTE_LH);
                rt -= rowH[j];
            }

            // 还有剩余行 → 换页（续页无页眉无节标题）
            if (segEnd < n) {
                drawFooter(cv, font);
                PdfPage np = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
                cv = new PdfCanvas(np);
                drawFooter(cv, font);
                curTop = CONT_TOP;
            }
            segStart = segEnd;
            segIdx++;
        }
        // 返回当前页内容底线，供下一站点接续判断
        return segBottom;
    }
}
