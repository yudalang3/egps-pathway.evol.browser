package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Initial fit, resize, and scrollbar regressions; also runs against real windows. */
public final class YyTreePreviewFitTest {
    private static String scene(int width, int height) {
        return """
            {"schema_version":1,"tree":{"labels":["Arabidopsis","Rice","Yeast","Worm","Fly","Zebrafish","Frog","Human","Mouse"],"root":17,
              "edges":[[17,11,1],[11,10,1],[10,1,1],[10,2,1],[11,3,2],[17,16,1],[16,12,1],
                [12,4,1],[12,5,1],[16,15,1],[15,6,2],[15,14,1],[14,7,1],[14,13,1],[13,8,1],[13,9,1]]},
             "canvas":{"width":%d,"height":%d,"margins":[20,40,80,40]},
             "layers":[{"type":"leaf_labels","show":true,"align":"right","font_size":13}]}
            """.formatted(width,height);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            YyTreePreview.initializeTheme();
            for (int[] dimensions : new int[][]{{1000,800},{1000,1000},{4000,3000},{20000,20000}}) {
                var preview=new YyTreePreview(new YyTreeBridge.YyTreePanel(JSON.parseObject(scene(dimensions[0],dimensions[1]))));
                resize(preview,820,620);
                assertFits(preview);
                double initial=preview.canvas.zoom;
                resize(preview,620,420);
                require(preview.canvas.zoom<initial,"Automatic resize did not reduce zoom");
                assertFits(preview);
                resize(preview,820,620);
                require(preview.canvas.zoom==initial,"Automatic resize did not restore zoom");
                wheel(preview,0);
                resize(preview,620,420);
                require(preview.canvas.zoom<initial,"Zero wheel event disabled automatic fit");
                resize(preview,820,620);
                wheel(preview,1);
                require(preview.canvas.zoom<=initial,"Zooming out increased a low fit zoom");
                double anchorX=100/preview.canvas.zoom,anchorY=100/preview.canvas.zoom;
                wheel(preview,-20);
                Point position=preview.scroll.getViewport().getViewPosition();
                require(Math.abs((position.x+100)/preview.canvas.zoom-anchorX)<1/preview.canvas.zoom
                    && Math.abs((position.y+100)/preview.canvas.zoom-anchorY)<1/preview.canvas.zoom,
                    "Wheel lost the pointer anchor after automatic fit");
                require(preview.scroll.getHorizontalScrollBar().isVisible()
                    && preview.scroll.getVerticalScrollBar().isVisible(),"Zoom overflow did not show scrollbars");
                double manual=preview.canvas.zoom;
                resize(preview,720,520);
                require(preview.canvas.zoom==manual,"Resize changed manual zoom");
                wheel(preview,200);
                require(!preview.scroll.getHorizontalScrollBar().isVisible()
                    && !preview.scroll.getVerticalScrollBar().isVisible(),"Fitting manual zoom left scrollbars visible");
                assertFits(preview);
                preview.disposeInteraction();
                int listeners=preview.scroll.getViewport().getComponentListeners().length;
                require(listeners==0,"Viewport resize listener leaked");
            }
            var small=new YyTreePreview(new YyTreeBridge.YyTreePanel(JSON.parseObject(scene(300,200))));
            resize(small,1000,800);
            require(small.canvas.zoom==1,"Small canvas was enlarged automatically");
            small.disposeInteraction();
        });
        if (!GraphicsEnvironment.isHeadless()) testWindows();
        System.out.println("Initial fit, automatic/manual resize, scrollbar transitions and low zoom passed.");
    }

    private static void testWindows() throws Exception {
        var bridge=new YyTreeBridge();
        Files.createDirectories(Path.of("temp/yytree-qa"));
        try {
            for (int[] dimensions : new int[][]{{1000,800},{1000,1000},{4000,3000}}) {
                bridge.showPlot(scene(dimensions[0],dimensions[1]),false);
                var shown = new java.util.concurrent.atomic.AtomicReference<YyTreePreview>();
                SwingUtilities.invokeAndWait(() -> {
                    JFrame frame=null;
                    for (Window window:Window.getWindows())
                        if (window instanceof JFrame candidate && candidate.isDisplayable()
                            && candidate.getContentPane() instanceof YyTreePreview) frame=candidate;
                    require(frame!=null,"Preview window not found");
                    var preview=(YyTreePreview)frame.getContentPane();
                    shown.set(preview);
                    assertFits(preview);
                    GraphicsConfiguration configuration=frame.getGraphicsConfiguration();
                    Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(configuration);
                    Rectangle screen=configuration.getBounds();
                    require(frame.getWidth()<=screen.width-insets.left-insets.right
                        && frame.getHeight()<=screen.height-insets.top-insets.bottom,"Window exceeds usable screen");
                    double before=preview.canvas.zoom;
                    frame.setSize(Math.max(200,frame.getWidth()/2),Math.max(200,frame.getHeight()/2));
                    frame.validate();
                    notifyResize(preview);
                    assertFits(preview);
                    require(preview.canvas.zoom<before,"Real window resize did not fit");
                    frame.setSize(Math.min(screen.width-insets.left-insets.right,dimensions[0]+100),
                        Math.min(screen.height-insets.top-insets.bottom,dimensions[1]+100));
                    frame.validate();
                    notifyResize(preview);
                    assertFits(preview);
                });
                YyTreeBridge.awaitRendering(shown.get());
                SwingUtilities.invokeAndWait(() -> {
                    var preview=shown.get();
                    double scale=preview.canvas.getGraphicsConfiguration().getDefaultTransform().getScaleX();
                    BufferedImage image=new BufferedImage((int)Math.ceil(preview.getWidth()*scale),(int)Math.ceil(preview.getHeight()*scale),BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics=image.createGraphics();
                    graphics.scale(scale,scale);
                    try { preview.printAll(graphics); } finally { graphics.dispose(); }
                    try {
                        ImageIO.write(image,"png",Path.of("temp/yytree-qa/initial-fit-"+dimensions[0]+"x"+dimensions[1]
                            +"-"+(int)(scale*100)+"pct.png").toFile());
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        } finally { bridge.closeAll(); }
    }

    private static void resize(YyTreePreview preview,int width,int height) {
        preview.setSize(width,height);
        layout(preview);
        notifyResize(preview);
        layout(preview);
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child:container.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    private static void notifyResize(YyTreePreview preview) {
        var viewport=preview.scroll.getViewport();
        for (ComponentListener listener:viewport.getComponentListeners())
            listener.componentResized(new ComponentEvent(viewport,ComponentEvent.COMPONENT_RESIZED));
    }

    private static void wheel(YyTreePreview preview,int rotation) {
        Point view=preview.scroll.getViewport().getViewPosition();
        preview.navigation.mouseWheelMoved(new MouseWheelEvent(preview.canvas,MouseEvent.MOUSE_WHEEL,0,0,
            view.x+100,view.y+100,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,rotation));
        layout(preview);
    }

    private static void assertFits(YyTreePreview preview) {
        Dimension extent=preview.scroll.getViewport().getExtentSize(),size=preview.canvas.getPreferredSize();
        require(extent.width>0 && extent.height>0 && size.width<=extent.width && size.height<=extent.height,"Canvas is clipped");
        require(preview.canvas.logicalWidth*preview.canvas.zoom<=extent.width+1e-8
            && preview.canvas.logicalHeight*preview.canvas.zoom<=extent.height+1e-8,"Scaled drawing is clipped");
        require(!preview.scroll.getHorizontalScrollBar().isVisible()
            && !preview.scroll.getVerticalScrollBar().isVisible(),"Initial fit shows scrollbars");
        require(preview.scroll.getViewport().getViewPosition().equals(new Point()),"Fitted canvas is displaced");
    }

    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
