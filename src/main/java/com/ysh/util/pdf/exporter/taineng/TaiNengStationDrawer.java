package com.ysh.util.pdf.exporter.taineng;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.taineng.TaiNengPage1Data;
import com.ysh.util.pdf.dto.taineng.TaiNengStationData;

import static com.ysh.util.pdf.exporter.taineng.TaiNengDrawer.*;

/**
 * 泰能充站点页：单个充电站结算信息表（整页自画，动态行高，可跨页）。
 * <p>第 1 个站点节标题"三、各充电站清单"，第 2 个起"三、各充电站清单（续）"。
 * <p>电站类型/固定手续费/平台手续费 3 行为双行高行（基础 32.64），备注列含 \n 双行文字（行距 12.32）。
 */
final class TaiNengStationDrawer {

    private static final float DOUBLE_H = 32.64f;   // 双行高行的基础行高
    private static final float NOTE_LH = 12.32f;     // 备注列多行文字的行距（模板实测）

    private static final String[] LABELS = {"电站名称", "电站地址", "电站类型",
            "平台手续费模式", "固定手续费／平台手续费费率", "月充电量（kWh）",
            "月充电电费（元）", "月充电服务费（元）", "普通机构充电量（kWh）",
            "普通机构充电电费（元）", "普通机构充电服务费（元）", "运营商机构充电量（kWh）",
            "运营商机构充电电费（元）", "运营商机构充电服务费（元）", "泰达电力机构充电量（kWh）",
            "泰达电力机构充电电费（元）", "泰达电力机构充电服务费（元）", "微信付款小计（元）",
            "微信手续费率", "微信手续费（元）", "支付宝付款小计（元）", "支付宝手续费率",
            "支付宝手续费（元）", "平台手续费（元）", "可提现费用（元）"};

    private static final boolean[] DOUBLE = new boolean[25];
    static { DOUBLE[2] = DOUBLE[4] = DOUBLE[23] = true; }

    private static final String[] NOTES = {
            null, null,
            "普通：结算电费和服务费。\n未报装用电：只结算服务费。",
            "固定金额或服务费费率。",
            "固定金额模式列示金额（元）；\n服务费费率模式列示费率（%）。",
            "本月本站总充电量，含机构用户。",
            "本月本站充电电费，含机构用户。",
            "本月本站充电服务费，含机构用户。",
            "已计入本站月充电量。",
            "已计入本站月充电电费。",
            "已计入本站月充电服务费。",
            "已计入本站月充电量。",
            "已计入本站月充电电费。",
            "已计入本站月充电服务费。",
            "已计入本站月充电量。",
            "已计入本站月充电电费。",
            "已计入本站月充电服务费。",
            "本月本站通过微信支付的付款金额。",
            "本月本站微信支付适用费率。",
            "本月本站微信支付对应的手续费。",
            "本月本站通过支付宝支付的付款金额。",
            "本月本站支付宝支付适用费率。",
            "本月本站支付宝支付对应的手续费。",
            "固定金额模式：按固定金额计收。\n费率模式：月充电服务费×平台手续费费率。",
            null};

    private TaiNengStationDrawer() {
    }

    /**
     * 画一个站点（每站最多 1 页起始，可跨页）。
     *
     * @param prevBottom   上一个内容画到的底线（本站之前），0 表示无
     * @param forceNewPage 强制从新页开始（第 1 个站点），否则空间足够时接续上一页
     * @return 本站画完后当前页内容底线（含底部注释占位）
     */
    static float draw(PdfDocument pdf, PdfFont font, PdfFont bold,
                      TaiNengPage1Data p1, TaiNengStationData s, int idx,
                      float prevBottom, boolean forceNewPage) {
        if (s == null) s = new TaiNengStationData();
        String secTitle = idx == 1 ? "三、各充电站清单" : "三、各充电站清单（续）";

        PdfCanvas cv;
        float tblTop;
        // 接续条件：上一内容下方剩余空间能放下 标题块 + 表头 + 2 行
        boolean samePage = !forceNewPage
                && prevBottom - MIN_Y >= TITLE_BLOCK + ROW_H + 2 * ROW_H;
        if (!samePage) {
            // 新页：标题在模板固定位置
            PdfPage page = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
            cv = new PdfCanvas(page);
            drawHeader(cv, font, bold, p1);
            drawFooter(cv, font);
            drawText(cv, bold, L, 739.89f, secTitle, FS_SEC);
            drawText(cv, font, L, 719.79f, "第 " + idx + " 个站点结算信息", 9.1f);
            tblTop = 708.89f;
        } else {
            // 接续上一页：标题画在上一个内容下方
            cv = new PdfCanvas(pdf.getLastPage());
            drawText(cv, bold, L, prevBottom - 16f, secTitle, FS_SEC);
            drawText(cv, font, L, prevBottom - 36f, "第 " + idx + " 个站点结算信息", 9.1f);
            tblTop = prevBottom - TITLE_BLOCK;
        }

        float[] xs = {L, 216.09f, 326.15f, R};
        float[] widths = colWidths(xs);
        float headH = ROW_H;
        int n = LABELS.length;
        int lastIdx = n - 1;   // 可提现费用行索引

        String[] values = {s.stationName, s.stationAddress, s.stationType, s.feeMode,
                s.fixedFee != null ? s.fixedFee : s.feeRate,
                s.monthCharge, s.monthElecFee, s.monthServFee,
                s.normalCharge, s.normalElecFee, s.normalServFee,
                s.operatorCharge, s.operatorElecFee, s.operatorServFee,
                s.tedaCharge, s.tedaElecFee, s.tedaServFee,
                s.wechatPayTotal, s.wechatFeeRate, s.wechatFee,
                s.alipayPayTotal, s.alipayFeeRate, s.alipayFee,
                s.platformFee, s.withdrawable};

        // 算行高
        float[] rowH = new float[n];
        for (int i = 0; i < n; i++) {
            float base = DOUBLE[i] ? DOUBLE_H : ROW_H;
            int labelL = lineCount(LABELS[i], font, FS_ROW, widths[0]);
            int valueL = lineCount(values[i], font, FS_ROW, widths[1]);
            int noteL = NOTES[i] != null ? lineCount(NOTES[i], font, FS_ROW, widths[2]) : 1;
            float rh = Math.max(base, Math.max(
                    labelL * ROW_H, Math.max(valueL * ROW_H, noteL * NOTE_LH)));
            rowH[i] = rh;
        }

        // 分段画（跨页）
        float curTop = tblTop;
        int segStart = 0;
        float segBottom = 0;
        while (segStart < n) {
            // 找当前页能放下的行数
            float yy = curTop - headH;
            int segEnd = segStart;
            while (segEnd < n) {
                // 最后一行（画完即结束）需预留底部注释空间，防止注释压到页脚
                float minY = (segEnd == n - 1) ? MIN_Y + NOTE_RESERVE : MIN_Y;
                float next = yy - rowH[segEnd];
                if (next < minY && segEnd > segStart) break;   // 至少画一行
                yy = next;
                segEnd++;
            }

            // 算行线
            float[] ry = new float[segEnd - segStart + 1];
            float t = curTop - headH;
            ry[0] = t;
            for (int j = segStart; j < segEnd; j++) { t -= rowH[j]; ry[j - segStart + 1] = t; }
            segBottom = t;

            // 灰底 + 表格线
            fillGray(cv, L, curTop - headH, R - L, headH);
            drawTable(cv, xs, curTop, segBottom, ry);

            // 表头文本
            drawCellText(cv, bold, 119.25f, curTop, headH, widths[0], "项目", FS_STA_HEAD, headH);
            drawCellText(cv, bold, 246.92f, curTop, headH, widths[1], "内容 / 金额", FS_STA_HEAD, headH);
            drawCellText(cv, bold, 431.91f, curTop, headH, widths[2], "备注", FS_STA_HEAD, headH);

            // 数据行文本（可提现费用行加粗）
            float rt = curTop - headH;
            for (int j = segStart; j < segEnd; j++) {
                PdfFont f = (j == lastIdx) ? bold : font;
                drawCellText(cv, f, 47f, rt, rowH[j], widths[0], LABELS[j], FS_ROW, ROW_H);
                drawCellText(cv, f, 224f, rt, rowH[j], widths[1], values[j], FS_ROW, ROW_H);
                if (NOTES[j] != null)
                    drawCellText(cv, font, 333.15f, rt, rowH[j], widths[2],
                            NOTES[j], FS_ROW, NOTE_LH);
                rt -= rowH[j];
            }

            // 还有剩余行 → 换页
            if (segEnd < n) {
                drawFooter(cv, font);
                PdfPage np = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
                cv = new PdfCanvas(np);
                drawHeader(cv, font, bold, p1);
                drawFooter(cv, font);
                curTop = CONT_TOP;
            }
            segStart = segEnd;
        }

        // 底部注释（跟着最后一段的底线）
        float noteY = segBottom - 18.5f;
        drawText(cv, font, L, noteY,
                "机构账号使用范围：普通机构限用户指定的固定充电站；运营商机构限所属运营商的充电站；泰达电力机构可在支持泰能充平台的全部充电站",
                FS_STA_NOTE);
        drawText(cv, font, L, noteY - 12f, "使用。", FS_STA_NOTE);
        // 返回当前页内容底线（含注释两行占位），供下一站点接续判断
        return segBottom - 32.5f;
    }
}
