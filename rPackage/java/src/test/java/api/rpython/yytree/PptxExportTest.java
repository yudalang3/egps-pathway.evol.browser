package api.rpython.yytree;
import org.apache.poi.xslf.usermodel.*;
import java.awt.*;
import java.awt.geom.*;
public final class PptxExportTest {
    public static void main(String[] args) throws Exception {
        try(var ppt=new XMLSlideShow()) {
            var group=ppt.createSlide().createGroup();
            var parent=new PptxExport.Drawing(group,ppt);
            var child=(PptxExport.Drawing)parent.create();
            child.translate(100,200); child.rotate(Math.PI/2);
            child.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(!parent.getTransform().isIdentity() || parent.getRenderingHint(RenderingHints.KEY_ANTIALIASING)!=null)
                throw new AssertionError("Child state leaked");
            child.setFont(new Font("Arial",Font.BOLD,12).deriveFont(12.5f));
            child.drawString("Editable",10f,20f);
            var text=(XSLFTextBox)group.getShapes().get(0);
            if(Math.abs(text.getRotation()-90)>1e-6 || text.getTextParagraphs().get(0).getTextRuns().get(0).getFontSize()!=12.5)
                throw new AssertionError("Rotation/font precision lost");
            double width=child.getFont().getStringBounds("Editable",child.getFontRenderContext()).getWidth();
            var metrics=child.getFont().getLineMetrics("Editable",child.getFontRenderContext());
            double height=metrics.getAscent()+metrics.getDescent();
            var expected=child.getTransform().transform(new Point2D.Double(10+width/2,20-metrics.getAscent()+height/2),null);
            if(expected.distance(text.getAnchor().getCenterX(),text.getAnchor().getCenterY())>.01)
                throw new AssertionError("Text transform lost");
            System.out.println("PPTX state isolation, editable text, transform and fractional font passed.");
        }
    }
}
