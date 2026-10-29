package api.rpython.yytree;

import egps2.utils.common.util.poi.pptx.SLGraphics;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.sl.usermodel.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.JComponent;

/** Local adaptation of the existing POI drawing bridge. */
final class PptxExport {
    static void save(JComponent component, File file) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(); var out = new FileOutputStream(file)) {
            ppt.setPageSize(component.getSize());
            var group = ppt.createSlide().createGroup();
            var bounds = new Rectangle2D.Double(0,0,component.getWidth(),component.getHeight());
            group.setAnchor(bounds); group.setInteriorAnchor(bounds);
            Graphics2D graphics = new Drawing(group,ppt);
            try { component.paint(graphics); } finally { graphics.dispose(); }
            ppt.write(out);
        }
    }

    static final class Drawing extends SLGraphics {
        Drawing(XSLFGroupShape group, XMLSlideShow ppt) { super(group,ppt); }
        @Override public Graphics create() {
            var copy = (Drawing)super.create();
            copy.setTransform(getTransform());
            copy.setRenderingHints(getRenderingHints());
            return copy;
        }
        @Override public void drawString(String text, float x, float y) {
            if (text == null || text.isEmpty()) return;
            var metrics = getFont().getLineMetrics(text,getFontRenderContext());
            double width = getFont().getStringBounds(text,getFontRenderContext()).getWidth();
            double height = metrics.getAscent()+metrics.getDescent();
            var transform = getTransform();
            Point2D center = transform.transform(new Point2D.Double(x+width/2,y-metrics.getAscent()+height/2),null);
            var box = (XSLFTextBox)getShapeGroup().createTextBox();
            box.setAnchor(new Rectangle2D.Double(center.getX()-width/2,center.getY()-height/2,width,height));
            box.setInsets(new Insets2D(0,0,0,0));
            box.setWordWrap(false); box.setVerticalAlignment(VerticalAlignment.TOP);
            box.setTextAutofit(TextShape.TextAutofit.NONE);
            box.setRotation(Math.toDegrees(Math.atan2(transform.getShearY(),transform.getScaleX())));
            box.setText(text);
            var paragraph = box.getTextParagraphs().get(0);
            paragraph.setSpaceBefore(0.0); paragraph.setSpaceAfter(0.0);
            var run = paragraph.getTextRuns().get(0);
            run.setFontFamily(getFont().getFamily()); run.setFontSize((double)getFont().getSize2D());
            run.setBold(getFont().isBold()); run.setItalic(getFont().isItalic());
            run.setFontColor(getColor());
        }
        @Override protected void applyStroke(SimpleShape<?,?> shape) {
            super.applyStroke(shape);
            if(getStroke() instanceof BasicStroke stroke && stroke.getDashArray()!=null)
                shape.setStrokeStyle(stroke.getDashArray()[0]<=1 ? StrokeStyle.LineDash.DOT : StrokeStyle.LineDash.DASH);
        }
    }
}

