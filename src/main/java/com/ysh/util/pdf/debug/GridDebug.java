package com.ysh.util.pdf.debug;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

/** 调试：打印指定 PDF 每页文本行（y + 按列拼接）。 */
public class GridDebug {

    static final class Chunk {
        final float x, y;
        final String text;
        Chunk(float x, float y, String text) { this.x = x; this.y = y; this.text = text; }
    }

    public static void main(String[] args) throws Exception {
        String file = args.length > 0 ? args[0] : "api_test_out.pdf";
        try (PdfDocument pdf = new PdfDocument(new PdfReader(file))) {
            System.out.println("页数=" + pdf.getNumberOfPages());
            for (int p = 1; p <= pdf.getNumberOfPages(); p++) {
                LineCollector lc = new LineCollector();
                PdfCanvasProcessor proc = new PdfCanvasProcessor(lc);
                var page = pdf.getPage(p);
                for (int i = 0; i < page.getContentStreamCount(); i++) {
                    proc.processContent(page.getContentStream(i).getBytes(), page.getResources());
                }
                Map<Float, List<Chunk>> byY = new TreeMap<>();
                for (Chunk c : lc.chunks) byY.computeIfAbsent((float) Math.round(c.y), k -> new ArrayList<>()).add(c);
                System.out.println("===== PAGE " + p + " =====");
                for (Map.Entry<Float, List<Chunk>> e : byY.entrySet()) {
                    e.getValue().sort((a, b) -> Float.compare(a.x, b.x));
                    StringBuilder sb = new StringBuilder();
                    for (Chunk c : e.getValue()) sb.append('[').append((int) c.x).append(']').append(c.text);
                    System.out.println("y=" + e.getKey() + "  " + sb);
                }
            }
        }
    }

    static final class LineCollector implements IEventListener {
        final List<Chunk> chunks = new ArrayList<>();

        @Override
        public void eventOccurred(IEventData data, EventType type) {
            if (type == EventType.RENDER_TEXT) {
                TextRenderInfo tri = (TextRenderInfo) data;
                var base = tri.getBaseline().getStartPoint();
                chunks.add(new Chunk(base.get(0), base.get(1), tri.getText()));
            }
        }

        @Override
        public Set<EventType> getSupportedEvents() {
            return EnumSet.of(EventType.RENDER_TEXT);
        }
    }
}
