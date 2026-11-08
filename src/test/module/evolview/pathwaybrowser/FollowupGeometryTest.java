package module.evolview.pathwaybrowser;
import module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.sl.usermodel.ShapeType;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class FollowupGeometryTest {
    public static void main(String[] args) throws Exception {
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("temp"));
        for (int flip=0;flip<4;flip++) for (double scaleX : new double[]{1.0, 4.0}) for (int rot : new int[]{0, 30, 45, 90}) {
            try (XMLSlideShow ppt = new XMLSlideShow()) {
                ppt.setPageSize(new Dimension(900, 500));
                var slide = ppt.createSlide();
                var group = slide.createGroup();
                group.setAnchor(new Rectangle2D.Double(80, 100, 100 * scaleX, 50));
                group.setFlipHorizontal((flip&1)!=0); group.setFlipVertical((flip&2)!=0);
                group.setInteriorAnchor(new Rectangle2D.Double(0, 0, 100, 100));
                var nested = group.createGroup();
                nested.setAnchor(new Rectangle2D.Double(0, 0, 100, 100));
                nested.setInteriorAnchor(new Rectangle2D.Double(0, 0, 100, 100));
                nested.setRotation(15);
                var rect = nested.createAutoShape();
                rect.setShapeType(ShapeType.RECT);
                rect.setAnchor(new Rectangle2D.Double(30, 30, 40, 20));
                rect.setRotation(rot);
                ((org.openxmlformats.schemas.presentationml.x2006.main.CTShape)rect.getXmlObject()).getNvSpPr().getCNvPr().setName("A_comp");
                rect.setFillColor(Color.RED);
                rect.setLineColor(null);
                var image = new BufferedImage(900, 500, BufferedImage.TYPE_INT_ARGB);
                var g = image.createGraphics();
                Rectangle2D[] captured={null};
                g.setRenderingHint(org.apache.poi.sl.draw.Drawable.DRAW_FACTORY,new org.apache.poi.sl.draw.DrawFactory(){
                    @Override public org.apache.poi.sl.draw.Drawable getDrawable(org.apache.poi.sl.usermodel.Shape<?,?> shape) {
                        var delegate=super.getDrawable(shape);
                        if(shape!=rect)return delegate;
                        return new org.apache.poi.sl.draw.DrawShape(shape){
                            @Override public void applyTransform(Graphics2D graphics){delegate.applyTransform(graphics);}
                            @Override public void draw(Graphics2D graphics){
                                var anchor=org.apache.poi.sl.draw.DrawShape.getAnchor(graphics,rect);
                                captured[0]=graphics.getTransform().createTransformedShape(anchor).getBounds2D();
                                delegate.draw(graphics);
                            }
                            @Override public void drawContent(Graphics2D graphics){delegate.drawContent(graphics);}
                        };
                    }
                });
                slide.draw(g); g.dispose();
                for (int y=0;y<500;y++) for(int x=0;x<900;x++) {
                    var c = new Color(image.getRGB(x,y), true);
                    if (c.getRed()<180||c.getGreen()>80||c.getBlue()>80) image.setRGB(x,y,0);
                }
                var actual = PptxSlideRenderer.scanNonTransparentBounds(image);
                var expected = PptxSlideRenderer.computeRotatedBounds(rect);
                double deviation = Math.max(Math.max(Math.abs(actual.getMinX()-expected.getMinX()),Math.abs(actual.getMinY()-expected.getMinY())),Math.max(Math.abs(actual.getMaxX()-expected.getMaxX()),Math.abs(actual.getMaxY()-expected.getMaxY())));
                System.out.printf("sx=%.1f sy=0.5 childrot=%d actual=%s computed=%s maxDelta=%.2f%n",scaleX,rot,actual,expected,deviation);
                System.out.println("captured="+captured[0]);
                var file=java.nio.file.Path.of("temp", "geometry-"+flip+"-"+scaleX+"-"+rot+".pptx");
                try(var output=java.nio.file.Files.newOutputStream(file)){ppt.write(output);}
                var counts=module.evolview.pathwaybrowser.io.PathwayComponentCountData.fromRawTable(java.util.List.of("Name","A"), java.util.List.of(java.util.List.of("Human","1")));
                var prepared=PptxSlideRenderer.prepareSlide(file.toString(),counts,()->false);
                var cluster=prepared.geneSprites().get("A").instanceClusters().getFirst();
                if(Math.abs(cluster.getMinX()-captured[0].getMinX())>0.01 || Math.abs(cluster.getMinY()-captured[0].getMinY())>0.01 || Math.abs(cluster.getWidth()-captured[0].getWidth())>0.01)throw new AssertionError("Wrong prepared cluster: "+cluster+" versus "+captured[0]);
            }
        }
    }
}
