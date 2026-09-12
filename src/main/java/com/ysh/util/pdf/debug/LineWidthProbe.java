package com.ysh.util.pdf.debug;

import com.itextpdf.kernel.geom.IShape;
import com.itextpdf.kernel.geom.Subpath;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.PathRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.util.*;

/** 调试工具：提取 PDF 每页全部水平线的线宽。用法：LineWidthProbe <pdf路径> */
public class LineWidthProbe {
    record Seg(float y, float x1, float x2, float w) {}

    public static void main(String[] args) throws Exception {
        try (PdfDocument doc = new PdfDocument(new PdfReader(args[0]))) {
            for (int pg = 1; pg <= doc.getNumberOfPages(); pg++) {
                List<Seg> hLines = new ArrayList<>();
                var page = doc.getPage(pg);
                IEventListener listener = new IEventListener() {
                    @Override
                    public void eventOccurred(IEventData d, EventType t) {
                        if (t != EventType.RENDER_PATH) return;
                        PathRenderInfo pri = (PathRenderInfo) d;
                        var ctm = pri.getCtm();
                        float w = pri.getLineWidth() * (float) Math.sqrt(
                                Math.abs(ctm.get(0) * ctm.get(4) - ctm.get(1) * ctm.get(3)));
                        for (Subpath sp : pri.getPath().getSubpaths()) {
                            for (IShape seg : sp.getSegments()) {
                                var bp = seg.getBasePoints();
                                var ps = bp.get(0);
                                var pe = bp.get(bp.size() - 1);
                                Vector v1 = new Vector((float) ps.x, (float) ps.y, 1).cross(ctm);
                                Vector v2 = new Vector((float) pe.x, (float) pe.y, 1).cross(ctm);
                                float y1 = v1.get(1), y2 = v2.get(1), x1 = v1.get(0), x2 = v2.get(0);
                                if (Math.abs(y1 - y2) < 0.5f && Math.abs(x1 - x2) > 2f) {
                                    hLines.add(new Seg(Math.round(y1),
                                            Math.min(x1, x2), Math.max(x1, x2), w));
                                }
                            }
                        }
                    }

                    @Override
                    public Set<EventType> getSupportedEvents() {
                        return EnumSet.of(EventType.RENDER_PATH);
                    }
                };
                PdfCanvasProcessor proc = new PdfCanvasProcessor(listener);
                for (int i = 0; i < page.getContentStreamCount(); i++) {
                    proc.processContent(page.getContentStream(i).getBytes(), page.getResources());
                }
                System.out.println("== page " + pg + " ==");
                hLines.sort(Comparator.comparing(Seg::y).reversed());
                for (Seg s : hLines) {
                    System.out.printf(java.util.Locale.ROOT, "y=%.0f  %.0f~%.0f  w=%.3f%n",
                            s.y(), s.x1(), s.x2(), s.w());
                }
            }
        }
    }
}
