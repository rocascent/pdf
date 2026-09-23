package com.ysh.util.pdf.exporter.taineng;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.settlement.Page1Data;

import static com.ysh.util.pdf.exporter.taineng.PdfDrawer.*;

/** 泰能充第 1 页：运营商信息表 + 合计信息表（整页自画，动态行高，表 2 可跨页）。 */
final class Page1Drawer {

    private Page1Drawer() {
    }

    static void draw(PdfDocument pdf, PdfFont font, PdfFont bold, Page1Data p) {
        if (p == null) p = new Page1Data();
        PdfPage page = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
        PdfCanvas cv = new PdfCanvas(page);
        drawHeader(cv, font, bold, p);
        drawFooter(cv, font);

        // ==================== 一、运营商信息（6 行 2 列，不换页） ====================
        final float T1_TOP = 649f, T1_HEAD = 16f, T1_ROW = 16f;
        drawText(cv, bold, L, 674.5f, "一、运营商信息", FS_SEC);
        float[] xs = {85f, 323f, 571f};
        float[] widths = colWidths(xs);

        String[] labels = {"运营商公司全称", "统一社会信用代码", "开户行", "账号", "企业地址", "联系人"};
        String[] values = {p.operatorName, p.creditCode, p.bank, p.account, p.address, p.contact};

        float bottom = T1_TOP - T1_HEAD;
        float[] rowH = new float[6];
        for (int i = 0; i < 6; i++) {
            float fs = (i == 2 || i == 3) ? FS_HEAD : FS_ROW;   // 开户行/账号 10.5
            // 行高随内容折行自适应（标签/内容取最大行数）
            int lines = Math.max(lineCount(labels[i], font, fs, widths[0]),
                    lineCount(values[i], font, fs, widths[1]));
            rowH[i] = lines * T1_ROW;
            bottom -= rowH[i];
        }
        float[] rowY = new float[7];
        float y = T1_TOP - T1_HEAD;
        rowY[0] = y;
        for (int i = 0; i < 6; i++) { y -= rowH[i]; rowY[i + 1] = y; }
        drawTable(cv, xs, T1_TOP, bottom, rowY);

        drawCellText(cv, bold, 90f, T1_TOP, T1_HEAD, widths[0], "项目", FS_ROW, T1_HEAD);
        drawCellText(cv, bold, 328f, T1_TOP, T1_HEAD, widths[1], "内容", FS_ROW, T1_HEAD);

        float rowTop = T1_TOP - T1_HEAD;
        for (int i = 0; i < 6; i++) {
            float fs = (i == 2 || i == 3) ? FS_HEAD : FS_ROW;   // 开户行/账号 10.5
            drawCellText(cv, font, 90f, rowTop, rowH[i], widths[0], labels[i], fs, T1_ROW);
            drawCellText(cv, font, 328f, rowTop, rowH[i], widths[1], values[i], fs, T1_ROW);
            rowTop -= rowH[i];
        }

        // ==================== 二、合计信息（18 行，可跨页；首段带表头，续页无表头无页眉） ====================
        final float T2_ROW = 28f;
        drawText(cv, bold, L, 486f, "二、合计信息", FS_SEC);
        float[] xs2 = {L, 233f, 350f, R};
        float[] w2 = colWidths(xs2);

        String[] labels2 = {"总充电量（kWh）", "总充电电费（元）", "总充电服务费（元）",
                "普通机构充电量（kWh）", "普通机构充电电费（元）", "普通机构充电服务费（元）",
                "运营商机构充电量（kWh）", "运营商机构充电电费（元）", "运营商机构充电服务费（元）",
                "泰达电力机构充电量（kWh）", "泰达电力机构充电电费（元）", "泰达电力机构充电服务费（元）",
                "微信付款合计（元）", "微信手续费（元）", "支付宝付款合计（元）",
                "支付宝手续费（元）", "平台手续费（元）", "总计可提现费用（元）"};
        String[] values2 = {p.totalCharge, p.totalElecFee, p.totalServFee,
                p.normalCharge, p.normalElecFee, p.normalServFee,
                p.operatorCharge, p.operatorElecFee, p.operatorServFee,
                p.tedaCharge, p.tedaElecFee, p.tedaServFee,
                p.wechatPayTotal, p.wechatFee, p.alipayPayTotal,
                p.alipayFee, p.platformFee, p.totalWithdrawable};
        String[] notes2 = {"普通用户充电电量合计", "普通用户充电电费合计", "普通用户充电服务费合计",
                "普通机构充电电量合计", "普通机构充电电费合计", "普通机构充电服务费合计",
                "运营商机构充电电量合计", "运营商机构充电电费合计", "运营商机构充电服务费合计",
                "泰达电力机构充电电量合计", "泰达电力机构充电电费合计", "泰达电力机构充电服务费合计",
                "普通用户微信付款合计", "普通用户微信手续费之和", "普通用户支付宝付款之和",
                "普通用户支付宝手续费之和", "平台手续费之和", "可提现费用合计"};
        int n2 = labels2.length;

        float curTop = 461f;   // 表顶（节标题 486 下方 25）
        int segStart = 0, segIdx = 0;
        while (segStart < n2) {
            // 找当前页能放下的行数（行高固定 28）
            float headH = (segIdx == 0) ? T2_ROW : 0f;
            float yy = curTop - headH;
            int segEnd = segStart;
            while (segEnd < n2) {
                float next = yy - T2_ROW;
                if (next < MIN_Y && segEnd > segStart) break;   // 至少画一行
                yy = next;
                segEnd++;
            }
            // 首段带表头，续页无表头
            float[] ry = new float[segEnd - segStart + 1];
            float t = curTop - headH;
            ry[0] = t;
            for (int j = segStart; j < segEnd; j++) { t -= T2_ROW; ry[j - segStart + 1] = t; }
            float segBottom = t;
            drawTable(cv, xs2, curTop, segBottom, ry);

            if (segIdx == 0) {
                drawCellText(cv, bold, 94.7f, curTop, headH, w2[0], "项目", FS_ROW, headH);
                drawCellText(cv, bold, 237.8f, curTop, headH, w2[1], "内容/金额", FS_ROW, headH);
                drawCellText(cv, bold, 355.7f, curTop, headH, w2[2], "备注", FS_ROW, headH);
            }
            float rt = curTop - headH;
            for (int j = segStart; j < segEnd; j++) {
                // 标签列不折行（模板为单行顶格排；最长"泰达电力机构充电服务费（元）"约 140pt 略超列宽）
                drawCellText(cv, font, 95f, rt, T2_ROW, Float.MAX_VALUE, labels2[j], FS_ROW, T2_ROW);
                drawCellText(cv, font, 235f, rt, T2_ROW, w2[1], values2[j], FS_ROW, T2_ROW);
                drawCellText(cv, font, 355f, rt, T2_ROW, w2[2], notes2[j], FS_ROW, T2_ROW);
                rt -= T2_ROW;
            }

            // 还有剩余行 → 换页（续页无页眉无节标题）
            if (segEnd < n2) {
                drawFooter(cv, font);
                PdfPage np = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
                cv = new PdfCanvas(np);
                drawFooter(cv, font);
                curTop = CONT_TOP;
            }
            segStart = segEnd;
            segIdx++;
        }
    }
}
