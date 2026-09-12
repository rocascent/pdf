package com.ysh.util.pdf.debug;

import com.itextpdf.kernel.geom.IShape;
import com.itextpdf.kernel.geom.Subpath;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.PathRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;

import java.util.*;

/** 调试工具：提取模板指定页的全部内容流文本/线段 + 注释 + AcroForm + XObject。 */
public class DumpPage {
    record Seg(float a, float b, float c) {} // 水平: (y, x1, x2) 垂直: (x, y1, y2)

    public static void main(String[] args) throws Exception {
        int pageNum = Integer.parseInt(args[0]);
        List<String> texts = new ArrayList<>();
        List<Seg> hLines = new ArrayList<>();
        List<Seg> vLines = new ArrayList<>();

        try (PdfDocument doc = new PdfDocument(new PdfReader("out.pdf"))) {
            PdfPage page = doc.getPage(pageNum);
            PdfDictionary pageDict = page.getPdfObject();
            System.out.println("MediaBox=" + pageDict.getAsArray(PdfName.MediaBox)
                    + " CropBox=" + pageDict.getAsArray(PdfName.CropBox)
                    + " Rotate=" + pageDict.getAsInt(PdfName.Rotate));
            System.out.println("content stream count=" + page.getContentStreamCount());

            IEventListener listener = new IEventListener() {
                @Override
                public void eventOccurred(IEventData d, EventType t) {
                    if (t == EventType.RENDER_TEXT) {
                        TextRenderInfo tri = (TextRenderInfo) d;
                        Vector p = tri.getBaseline().getStartPoint();
                        texts.add(String.format(java.util.Locale.ROOT, "%.0f,%.0f | %s",
                                p.get(0), p.get(1), tri.getText()));
                    } else if (t == EventType.RENDER_PATH) {
                        PathRenderInfo pri = (PathRenderInfo) d;
                        com.itextpdf.kernel.geom.Matrix ctm = pri.getCtm(); // 路径坐标是用户空间，需乘 CTM
                        for (Subpath sp : pri.getPath().getSubpaths()) {
                            for (IShape seg : sp.getSegments()) {
                                List<com.itextpdf.kernel.geom.Point> bp = seg.getBasePoints();
                                com.itextpdf.kernel.geom.Point ps = bp.get(0), pe = bp.get(bp.size() - 1);
                                Vector v1 = new Vector((float) ps.x, (float) ps.y, 1).cross(ctm);
                                Vector v2 = new Vector((float) pe.x, (float) pe.y, 1).cross(ctm);
                                float x1 = v1.get(0), y1 = v1.get(1), x2 = v2.get(0), y2 = v2.get(1);
                                if (Math.abs(y1 - y2) < 0.5f && Math.abs(x1 - x2) > 2f) {
                                    hLines.add(new Seg(Math.round(y1), Math.min(x1, x2), Math.max(x1, x2)));
                                } else if (Math.abs(x1 - x2) < 0.5f && Math.abs(y1 - y2) > 2f) {
                                    vLines.add(new Seg(Math.round(x1), Math.min(y1, y2), Math.max(y1, y2)));
                                }
                            }
                        }
                    }
                }

                @Override
                public Set<EventType> getSupportedEvents() {
                    return EnumSet.of(EventType.RENDER_TEXT, EventType.RENDER_PATH);
                }
            };

            PdfCanvasProcessor proc = new PdfCanvasProcessor(listener);
            for (int i = 0; i < page.getContentStreamCount(); i++) {
                proc.processContent(page.getContentStream(i).getBytes(), page.getResources());
            }

            System.out.println("==== AcroForm fields ====");
            com.itextpdf.forms.PdfAcroForm form = com.itextpdf.forms.PdfAcroForm.getAcroForm(doc, false);
            if (form != null && !form.getFormFields().isEmpty()) {
                for (Map.Entry<String, com.itextpdf.forms.fields.PdfFormField> e : form.getFormFields().entrySet()) {
                    var f = e.getValue();
                    String rect = f.getWidgets().isEmpty() ? "?" : f.getWidgets().get(0).getRectangle().toString();
                    System.out.println("FIELD [" + f.getFormType() + "] " + e.getKey() + " rect=" + rect);
                }
            } else {
                System.out.println("NO ACROFORM FIELDS");
            }

            System.out.println("==== Annotations (/Annots) ====");
            PdfArray annots = pageDict.getAsArray(PdfName.Annots);
            if (annots == null || annots.isEmpty()) {
                System.out.println("NO ANNOTATIONS");
            } else {
                for (int i = 0; i < annots.size(); i++) {
                    PdfDictionary a = annots.getAsDictionary(i);
                    if (a == null) continue;
                    System.out.println("ANNOT Subtype=" + a.getAsName(PdfName.Subtype)
                            + " Rect=" + a.getAsArray(PdfName.Rect)
                            + " Contents=" + a.getAsString(PdfName.Contents));
                }
            }

            System.out.println("==== XObject resources ====");
            com.itextpdf.kernel.pdf.PdfDictionary xobj = page.getResources()
                    .getResource(PdfName.XObject);
            if (xobj != null) {
                for (PdfName name : xobj.keySet()) {
                    com.itextpdf.kernel.pdf.PdfDictionary obj = xobj.getAsDictionary(name);
                    System.out.println(name + " -> SubType=" + obj.getAsName(PdfName.Subtype)
                            + " Size=" + obj.getAsInt(PdfName.Width) + "x" + obj.getAsInt(PdfName.Height));
                }
            }

            System.out.println("==== texts (x,y | content) ====");
            for (String t : texts) System.out.println(t);
        }

        System.out.println("==== page " + pageNum + " horizontal lines (y: x1~x2) ====");
        Map<Integer, List<Seg>> byY = new TreeMap<>(Collections.reverseOrder());
        for (Seg s : hLines) byY.computeIfAbsent((int) s.a(), k -> new ArrayList<>()).add(s);
        for (Map.Entry<Integer, List<Seg>> e : byY.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (Seg s : e.getValue()) sb.append(" ").append((int) s.b()).append("~").append((int) s.c());
            System.out.println(e.getKey() + " |" + sb);
        }

        System.out.println("==== vertical lines (x: y1~y2) ====");
        Map<Integer, List<Seg>> byX = new TreeMap<>();
        for (Seg s : vLines) byX.computeIfAbsent((int) s.a(), k -> new ArrayList<>()).add(s);
        for (Map.Entry<Integer, List<Seg>> e : byX.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (Seg s : e.getValue()) sb.append(" ").append((int) s.b()).append("~").append((int) s.c());
            System.out.println(e.getKey() + " |" + sb);
        }
    }
}
