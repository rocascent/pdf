package com.ysh.util.pdf.exporter.taineng;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.taineng.TaiNengPage1Data;

import static com.ysh.util.pdf.exporter.taineng.TaiNengDrawer.*;

/** 泰能充第 1 页：运营商信息表 + 合计信息表（整页自画，动态行高，表 2 可跨页）。 */
final class TaiNengPage1Drawer {

    private TaiNengPage1Drawer() {
    }

    static void draw(PdfDocument pdf, PdfFont font, PdfFont bold, TaiNengPage1Data p) {
        if (p == null) p = new TaiNengPage1Data();
        PdfPage page = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
        PdfCanvas cv = new PdfCanvas(page);
        drawHeader(cv, font, bold, p);
        drawFooter(cv, font);

        float[] xs = {L, 224.1f, 357.17f, R};
        float[] widths = colWidths(xs);

        // ==================== 一、运营商信息（5 行，不换页） ====================
        drawText(cv, bold, L, 739.89f, "一、运营商信息", FS_SEC);
        float tblTop = 728.89f;
        float headH = ROW_H;

        String[] labels = {"运营商公司全称", "统一社会信用代码", "企业地址", "联系人", "联系电话"};
        String[] values = {p.operatorName, p.creditCode, p.address, p.contact, p.phone};
        String[] notes = {"与营业执照登记名称一致。", "运营商统一社会信用代码。",
                "", "结算业务联系人。", "结算业务联系电话。"};

        float[] rowH = new float[labels.length];
        for (int i = 0; i < labels.length; i++) {
            int lines = rowLines(font, FS_ROW, widths, labels[i], values[i], notes[i]);
            rowH[i] = Math.max(ROW_H, lines * ROW_H);
        }
        float bottom = tblTop - headH;
        for (float rh : rowH) bottom -= rh;

        float[] rowY = new float[labels.length + 1];
        float y = tblTop - headH;
        rowY[0] = y;
        for (int i = 0; i < labels.length; i++) { y -= rowH[i]; rowY[i + 1] = y; }
        fillGray(cv, L, tblTop - headH, R - L, headH);
        drawTable(cv, xs, tblTop, bottom, rowY);

        drawCellText(cv, bold, 123.05f, tblTop, headH, widths[0], "项目", FS_TBL_HEAD, headH);
        drawCellText(cv, bold, 281.63f, tblTop, headH, widths[1], "内容", FS_TBL_HEAD, headH);
        drawCellText(cv, bold, 447.22f, tblTop, headH, widths[2], "备注", FS_TBL_HEAD, headH);

        float rowTop = tblTop - headH;
        for (int i = 0; i < labels.length; i++) {
            drawCellText(cv, font, 47f, rowTop, rowH[i], widths[0], labels[i], FS_ROW, ROW_H);
            drawCellText(cv, font, 232f, rowTop, rowH[i], widths[1], values[i], FS_ROW, ROW_H);
            if (!notes[i].isEmpty())
                drawCellText(cv, font, 364.17f, rowTop, rowH[i], widths[2], notes[i], FS_ROW, ROW_H);
            rowTop -= rowH[i];
        }

        // ==================== 二、合计信息（18 行，可跨页） ====================
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
        String[] notes2 = {"各电站对应项目合计，含机构用户。", "各电站对应项目合计，含机构用户。",
                "各电站对应项目合计，含机构用户。",
                "各电站该类机构用户对应项目合计。", "各电站该类机构用户对应项目合计。",
                "各电站该类机构用户对应项目合计。",
                "各电站该类机构用户对应项目合计。", "各电站该类机构用户对应项目合计。",
                "各电站该类机构用户对应项目合计。",
                "各电站该类机构用户对应项目合计。", "各电站该类机构用户对应项目合计。",
                "各电站该类机构用户对应项目合计。",
                "各电站对应支付渠道付款小计之和。", "各电站微信手续费之和。",
                "各电站对应支付渠道付款小计之和。",
                "各电站支付宝手续费之和。", "各电站平台手续费之和。",
                "本单所列各电站可提现费用之和。"};
        int n2 = labels2.length;
        int totalIdx = n2 - 1;   // 总计行索引

        // 算行高
        float[] rowH2 = new float[n2];
        for (int i = 0; i < n2; i++) {
            int lines = rowLines(font, FS_ROW, widths, labels2[i], values2[i], notes2[i]);
            rowH2[i] = Math.max(ROW_H, lines * ROW_H);
        }

        float secY = bottom - 22f;    // 节标题基线
        drawText(cv, bold, L, secY, "二、合计信息", FS_SEC);
        float curTop = secY - 11f;     // 表格顶线

        // 分段画（跨页）
        int segStart = 0;
        float segBottom = 0;
        while (segStart < n2) {
            // 找当前页能放下的行数
            float yy = curTop - headH;
            int segEnd = segStart;
            while (segEnd < n2) {
                // 最后一行（画完即结束）需预留底部注释空间，防止注释压到页脚
                float minY = (segEnd == n2 - 1) ? MIN_Y + NOTE_RESERVE : MIN_Y;
                float next = yy - rowH2[segEnd];
                if (next < minY && segEnd > segStart) break;   // 至少画一行
                yy = next;
                segEnd++;
            }

            // 算行线
            float[] ry = new float[segEnd - segStart + 1];
            float t = curTop - headH;
            ry[0] = t;
            for (int j = segStart; j < segEnd; j++) { t -= rowH2[j]; ry[j - segStart + 1] = t; }
            segBottom = t;

            // 灰底 + 表格线
            fillGray(cv, L, curTop - headH, R - L, headH);
            drawTable(cv, xs, curTop, segBottom, ry);

            // 强调线（总计行在当前段时画）
            if (segStart <= totalIdx && totalIdx < segEnd) {
                float emphY = segBottom + rowH2[totalIdx];   // 总计行行顶
                hLineBold(cv, L, R, emphY);
            }

            // 表头文本
            drawCellText(cv, bold, 123.05f, curTop, headH, widths[0], "项目", FS_TBL_HEAD, headH);
            drawCellText(cv, bold, 265.88f, curTop, headH, widths[1], "数量 / 金额", FS_TBL_HEAD, headH);
            drawCellText(cv, bold, 447.22f, curTop, headH, widths[2], "备注", FS_TBL_HEAD, headH);

            // 数据行文本
            float rt = curTop - headH;
            for (int j = segStart; j < segEnd; j++) {
                PdfFont f = (j == totalIdx) ? bold : font;
                drawCellText(cv, f, 47f, rt, rowH2[j], widths[0], labels2[j], FS_ROW, ROW_H);
                drawCellText(cv, f, 232f, rt, rowH2[j], widths[1], values2[j], FS_ROW, ROW_H);
                drawCellText(cv, f, 364.17f, rt, rowH2[j], widths[2], notes2[j], FS_ROW, ROW_H);
                rt -= rowH2[j];
            }

            // 还有剩余行 → 换页
            if (segEnd < n2) {
                drawFooter(cv, font);
                PdfPage np = pdf.addNewPage(new PageSize(PAGE_W, PAGE_H));
                cv = new PdfCanvas(np);
                drawHeader(cv, font, bold, p);
                drawFooter(cv, font);
                curTop = CONT_TOP;
            }
            segStart = segEnd;
        }

        // 底部注（跟着最后一段的底线）
        drawText(cv, font, L, segBottom - 18.5f,
                "注：三类机构统计均为对应充电总量、充电电费和充电服务费的分类明细，不重复累加。",
                FS_NOTE);
    }
}
