package com.ysh.util.pdf.debug;

import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.util.Collections;
import java.util.Set;

/** 调试探针：打印指定页每段文字的基线起止 x、字号（反推模板字体大小）。用法：FontProbe <pdf> [页码] */
public class FontProbe {
    public static void main(String[] args) throws Exception {
        try (PdfDocument pdf = new PdfDocument(new PdfReader(args[0]))) {
            int pg = args.length > 1 ? Integer.parseInt(args[1]) : 2;
            new PdfCanvasProcessor(new IEventListener() {
                @Override
                public void eventOccurred(IEventData data, EventType type) {
                    if (!(data instanceof TextRenderInfo)) return;
                    TextRenderInfo t = (TextRenderInfo) data;
                    Vector s = t.getBaseline().getStartPoint();
                    Vector e = t.getBaseline().getEndPoint();
                    System.out.printf("x=%.1f y=%.1f endX=%.1f w=%.1f size=%.2f | %s%n",
                            s.get(Vector.I1), s.get(Vector.I2), e.get(Vector.I1),
                            e.get(Vector.I1) - s.get(Vector.I1), t.getFontSize(), t.getText());
                }
                @Override
                public Set<EventType> getSupportedEvents() {
                    return Collections.singleton(EventType.RENDER_TEXT);
                }
            }).processPageContent(pdf.getPage(pg));
        }
    }
}
