package com.ysh.util.pdf.exporter.reconciliation;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;

import com.ysh.util.pdf.dto.reconciliation.Page1Data;

import static com.ysh.util.pdf.exporter.reconciliation.PdfDrawer.*;

/** 第 1 页：整页自画（页眉 + 抬头小表 + 线上结算收入 + 大写金额 + 机构消费收入 + 签字区 + 页脚）。 */
final class Page1Drawer {

    private static final float PAGE_W = 595.28f;        // A4 页宽
    private static final float BOX_TOP = 730f;          // 抬头小表顶线
    private static final float BOX_ROW_H = 27f;         // 抬头小表单行高
    private static final float BOX_DROP = 2.5f;         // 抬头小表基线下移（对齐模板实测 714）
    private static final float[] BOX_XS = {L, 115f, 297f, 370f, R};
    private static final float[] BOX_COLS = {48f, 119f, 303f, 374f};  // 标签/值/标签/值 x
    private static final float ROW_H = 28f;             // 表 1 / 表 2 数据行高
    private static final float[] X1 = {L, 160f, 356f, R};       // 表 1 列
    private static final float[] X2 = {L, 213f, 330f, 447f, R}; // 表 2 列

    private Page1Drawer() {
    }

    static void fill(PdfDocument pdf, PdfFont font, Page1Data p) {
        if (p == null) return;
        PdfPage page = pdf.addNewPage();

        // 页眉：运营结算 / 金额单位 / 大标题（居中）/ 副标题（居中）/ 分隔线
        drawTitleHeader(page, font,
                (PAGE_W - font.getWidth("运营商月度对账单", FS_TITLE)) / 2f, "运营商月度对账单",
                (PAGE_W - font.getWidth("线上结算汇总及机构用户消费统计", FS)) / 2f,
                "线上结算汇总及机构用户消费统计");
        drawFooterBase(page, font, p.settlementNo);

        // 抬头小表（5 行 4 列：标签/值/标签/值）
        float y = drawOperatorBox(page, font, p);

        // 一、运营商线上结算收入
        y -= 24f;
        drawText(page, font, 42f, y, "一、运营商线上结算收入", FS_SEC);
        y -= 10f;
        String[] h1 = {"费用项目", "各场站线上可分配净额", "本运营商线上清分收入"};
        y = drawHeaderBand(page, font, FS, y, ROW_H, X1, h1);
        String[][] rows1 = {
                {"电费", z(p.totalElecMoney), z(p.clrElecMoney)},
                {"服务费", z(p.totalServMoney), z(p.clrServMoney)},
                {"占位费", z(p.totalTmoutMoney), z(p.clrTmoutMoney)},
                {"合计", p.totalMoney, p.clrMoney}};   // 合计行忘传留空，不补 0.00
        y = drawRows(page, font, X1, y, rows1);

        // 本运营商线上结算金额（大写）：整行方框（上/下/左/右边框，文本在框内中线）
        y -= 20f;
        float capTop = y + 14f, capBot = y - 14f;
        hLineBold(page, L, R, capTop);
        hLineBold(page, L, R, capBot);
        vLineBold(page, L, capBot, capTop);
        vLineBold(page, R, capBot, capTop);
        drawText(page, font, 48f, y, "本运营商线上结算金额（大写）：", FS);
        vLineBold(page, 205f, capBot, capTop);      // 标签与金额值之间的竖线
        drawText(page, font, 210f, y, p.amountWords, FS);
        y = capBot - 34f;

        // 二、机构用户消费收入（线下结算）
        drawText(page, font, 42f, y, "二、机构用户消费收入（线下结算）", FS_SEC);
        y -= 10f;
        String[] h2 = {"机构用户类型", "充电消费收入", "占位费收入", "消费收入合计"};
        y = drawHeaderBand(page, font, FS, y, ROW_H, X2, h2);
        String[][] rows2 = {
                {"普通机构", z(p.normalCharge), z(p.normalOccupy), z(p.normalTotal)},
                {"运营商机构", z(p.operatorCharge), z(p.operatorOccupy), z(p.operatorTotal)},
                {"泰达电力机构", z(p.tedaCharge), z(p.tedaOccupy), z(p.tedaTotal)},
                {"合计", p.totalOrgCharge, p.totalOrgOccupy, p.totalOrgIncome}};
        y = drawRows(page, font, X2, y, rows2);

        // 表 2 下方注记 + 签字区 + 底部注记
        y -= 20f;
        drawText(page, font, 42f, y,
                "充电消费收入为优惠后的电费与服务费之和。机构用户业务在线下结算，不计入本单线上结算金额。", 8.5f);
        drawSignature(page, font, p);
        drawText(page, font, 42f, 70f,
                "本结算单包括场站线上清分汇总、场站结算明细及机构用户充电消费统计附件。", 8.5f);
    }

    /** 抬头小表：5 行（结算单号/结算期间、运营商名称/编号、结算方名称/出单日期、收款户名/开户银行、银行账号），返回底线 y。末行银行账号值合并右侧三列。 */
    private static float drawOperatorBox(PdfPage page, PdfFont font, Page1Data p) {
        float bottom = BOX_TOP - 5 * BOX_ROW_H;
        float midTop = BOX_TOP - 4 * BOX_ROW_H;   // 末行顶线（前 4 行底线）
        hLineBold(page, L, R, BOX_TOP);
        for (int i = 1; i <= 4; i++) hLine(page, L, R, BOX_TOP - i * BOX_ROW_H);
        hLineBold(page, L, R, bottom);
        // 前 4 行竖线完整；末行银行账号值合并右侧三列，只留 115 分栏线
        vLineBold(page, L, midTop, BOX_TOP);
        vLineBold(page, R, midTop, BOX_TOP);
        for (float x : new float[]{115f, 297f, 370f}) vLine(page, x, midTop, BOX_TOP);
        vLineBold(page, L, bottom, midTop);
        vLineBold(page, R, bottom, midTop);
        vLine(page, 115f, bottom, midTop);

        String[][] rows = {
                {"结算单号", p.settlementNo, "结算期间", p.period},
                {"运营商名称", p.operatorName, "运营商编号", p.operatorCode},
                {"结算方名称", p.settlerName, "出单日期", p.issueDate},
                {"收款户名", p.accountName, "开户银行", p.bankName}};
        float[] w = colWidths(BOX_XS);
        for (int r = 0; r < rows.length; r++) {
            float top = BOX_TOP - r * BOX_ROW_H;
            for (int c = 0; c < 4; c++) {
                drawCellText(page, font, BOX_COLS[c], top, BOX_ROW_H, w[c], rows[r][c], FS, BOX_ROW_H, BOX_DROP);
            }
        }
        // 末行：银行账号标签 + 账号值横跨右侧三列（119 → R）
        drawCellText(page, font, BOX_COLS[0], midTop, BOX_ROW_H, w[0], "银行账号", FS, BOX_ROW_H, BOX_DROP);
        drawCellText(page, font, 119f, midTop, BOX_ROW_H, R - 119f - CELL_PAD, p.accountNumber, FS, BOX_ROW_H, BOX_DROP);
        return bottom;
    }

    /** 定长数据行表（表 1 / 表 2 共用）：最后一行视为合计（顶/底线加粗），返回表底线 y。 */
    private static float drawRows(PdfPage page, PdfFont font, float[] xs, float y, String[][] rows) {
        float right = xs[xs.length - 1];
        float[] w = colWidths(xs);
        for (int i = 0; i < rows.length; i++) {
            boolean last = i == rows.length - 1;
            if (last) hLineBold(page, xs[0], right, y);
            else hLine(page, xs[0], right, y);
            for (int c = 0; c < rows[i].length; c++) {
                drawCellText(page, font, xs[c] + (c == 0 ? 6f : 4f), y, ROW_H, w[c], rows[i][c], FS, ROW_H);
            }
            hLineBold(page, xs[0], right, y - ROW_H);
            vLines(page, xs, y - ROW_H, y);
            y -= ROW_H;
        }
        return y;
    }

    /** 签字区：左结算方 / 右运营商，盖章行上下留空（无框）+ 经办人 + 日期。 */
    private static void drawSignature(PdfPage page, PdfFont font, Page1Data p) {
        drawText(page, font, 42f, 168f, "结算方（盖章）：", FS);
        drawText(page, font, 42f, 140f, "经办人：" + p.settlerHandler, FS);
        drawText(page, font, 42f, 118f,
                "日期：" + p.settlerYear + "年" + p.settlerMonth + "月" + p.settlerDay + "日", FS);
        drawText(page, font, 310f, 168f, "运营商（盖章）：", FS);
        drawText(page, font, 310f, 140f, "经办人：" + p.operatorHandler, FS);
        drawText(page, font, 310f, 118f,
                "日期：" + p.operatorYear + "年" + p.operatorMonth + "月" + p.operatorDay + "日", FS);
    }
}
