package com.ysh.util.pdf.exporter.reconciliation;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.pdf.PdfDocument;

import com.ysh.util.pdf.dto.reconciliation.Page1Data;

import static com.ysh.util.pdf.exporter.reconciliation.PdfDrawer.drawText;
import static com.ysh.util.pdf.exporter.reconciliation.PdfDrawer.fillWhite;
import static com.ysh.util.pdf.exporter.reconciliation.PdfDrawer.z;

/** 第 1 页：固定坐标填充（坐标取自 api_test_out.pdf 实测）。 */
final class Page1Drawer {

    private Page1Drawer() {
    }

    static void fill(PdfDocument pdf, PdfFont font, Page1Data p) {
        if (p == null) return;
        int pg = 1;
        float fs = 9f;

        // 抬头小表（模板已印标签，只画值）
        drawText(pdf, font, pg, 119f, 714f, p.settlementNo, fs);
        drawText(pdf, font, pg, 374f, 714f, p.period, fs);
        drawText(pdf, font, pg, 119f, 689f, p.operatorName, fs);
        drawText(pdf, font, pg, 374f, 689f, p.operatorCode, fs);
        drawText(pdf, font, pg, 119f, 664f, p.settlerName, fs);
        drawText(pdf, font, pg, 374f, 664f, p.issueDate, fs);
        drawText(pdf, font, pg, 119f, 639f, p.accountName, fs);
        drawText(pdf, font, pg, 374f, 639f, p.bankName, fs);
        drawText(pdf, font, pg, 119f, 614f, p.accountNumber, fs);

        // 表 1 运营商线上结算收入（净额 x=157 / 收入 x=352；电费525 服务费497 占位费469 合计441）
        drawText(pdf, font, pg, 157f, 525f, z(p.totalElecMoney), fs);
        drawText(pdf, font, pg, 352f, 525f, z(p.clrElecMoney), fs);
        drawText(pdf, font, pg, 157f, 497f, z(p.totalServMoney), fs);
        drawText(pdf, font, pg, 352f, 497f, z(p.clrServMoney), fs);
        drawText(pdf, font, pg, 157f, 469f, z(p.totalTmoutMoney), fs);
        drawText(pdf, font, pg, 352f, 469f, z(p.clrTmoutMoney), fs);
        // 合计行：忘传留空，不补 0.00
        drawText(pdf, font, pg, 157f, 441f, p.totalMoney, fs);
        drawText(pdf, font, pg, 352f, 441f, p.clrMoney, fs);

        // 大写金额
        drawText(pdf, font, pg, 226f, 405f, p.amountWords, fs);

        // 表 2 机构用户消费收入（x=187/311/433；普通317 运营商290 泰达263 合计236）
        drawText(pdf, font, pg, 187f, 317f, z(p.normalCharge), fs);
        drawText(pdf, font, pg, 311f, 317f, z(p.normalOccupy), fs);
        drawText(pdf, font, pg, 433f, 317f, z(p.normalTotal), fs);
        drawText(pdf, font, pg, 187f, 290f, z(p.operatorCharge), fs);
        drawText(pdf, font, pg, 311f, 290f, z(p.operatorOccupy), fs);
        drawText(pdf, font, pg, 433f, 290f, z(p.operatorTotal), fs);
        drawText(pdf, font, pg, 187f, 263f, z(p.tedaCharge), fs);
        drawText(pdf, font, pg, 311f, 263f, z(p.tedaOccupy), fs);
        drawText(pdf, font, pg, 433f, 263f, z(p.tedaTotal), fs);
        drawText(pdf, font, pg, 187f, 236f, p.totalOrgCharge, fs);
        drawText(pdf, font, pg, 311f, 236f, p.totalOrgOccupy, fs);
        drawText(pdf, font, pg, 433f, 236f, p.totalOrgIncome, fs);

        // 签字区：左结算方 / 右运营商
        drawText(pdf, font, pg, 80f, 156f, p.settlerHandler, fs);
        drawText(pdf, font, pg, 73f, 138f, p.settlerYear, fs);
        drawText(pdf, font, pg, 111f, 138f, p.settlerMonth, fs);
        drawText(pdf, font, pg, 138f, 138f, p.settlerDay, fs);
        drawText(pdf, font, pg, 336f, 156f, p.operatorHandler, fs);
        drawText(pdf, font, pg, 329f, 138f, p.operatorYear, fs);
        drawText(pdf, font, pg, 367f, 138f, p.operatorMonth, fs);
        drawText(pdf, font, pg, 394f, 138f, p.operatorDay, fs);

        // 页脚结算单号（"结算单号："模板已印）：白块盖掉模板预印的填空横线，只留单号
        fillWhite(pdf.getPage(pg), 81f, 22f, 97f, 14f);
        drawText(pdf, font, pg, 84f, 29f, p.settlementNo, fs);
    }
}
