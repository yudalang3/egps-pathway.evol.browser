package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Hover behavior, view-only painting, bounded rendering, and real HiDPI previews. */
public final class YyTreeHoverTest {
    private static JSONObject scene(int code) {
        return JSON.parseObject("""
            {"schema_version":1,"tree":{"labels":["<html>&Species\\\"","Human","Mouse"],"root":5,
            "edges":[[5,4,1],[4,1,1],[4,2,2],[5,3,3]]},
            "canvas":{"width":1000,"height":800,"margins":[20,40,80,40]},
            "layers":[{"type":"layout","code":%d},{"type":"leaf_labels","show":true,"align":"right"}]}
            """.formatted(code));
    }

    private static YyTreePreview preview(JSONObject scene) throws Exception {
        AtomicReference<YyTreeBridge.YyTreePanel> panel=new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panel.set(new YyTreeBridge.YyTreePanel(scene)));
        panel.get().prepare();
        AtomicReference<YyTreePreview> result=new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            var preview=new YyTreePreview(panel.get());
            preview.setSize(840,660);
            layout(preview);
            preview.fitToViewport();
            layout(preview);
            preview.cache.start();
            result.set(preview);
        });
        YyTreeBridge.awaitRendering(result.get());
        return result.get();
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(YyTreePreview::initializeTheme);
        for(int code=1;code<=7;code++) {
            var preview=preview(scene(code));
            try {
                SwingUtilities.invokeAndWait(() -> {
                    String original=preview.canvas.scene.toJSONString();
                    BufferedImage before=sceneImage(preview);
                    int scenePaints=preview.canvas.sceneDrawCount.get();
                    for(var node:preview.nodePoints) {
                        move(preview,node.x()*preview.canvas.zoom,node.y()*preview.canvas.zoom);
                        require(preview.hovered!=null && preview.hovered.id()==node.id(),"Node hit failed in layout "+preview.canvas.code);
                        Point2D cross=preview.crosshairPoint();
                        require(cross.distance(node.x()*preview.canvas.zoom,node.y()*preview.canvas.zoom)<1e-8,"Crosshair did not snap");
                        require(preview.canvas.getToolTipText().contains(node.node()==preview.canvas.root?"N/A (root)":Double.toString(node.node().getRealBranchLength())),"Missing incoming branch length");
                    }
                    var leaf=preview.nodePoints.getFirst();
                    move(preview,leaf.x()*preview.canvas.zoom,leaf.y()*preview.canvas.zoom);
                    require(preview.canvas.getToolTipText().contains("&lt;html&gt;&amp;Species&quot;"),"Tooltip HTML was not escaped");
                    preview.snap.doClick(0);
                    move(preview,leaf.x()*preview.canvas.zoom+5,leaf.y()*preview.canvas.zoom);
                    require(preview.hovered==leaf,"Disabling snap removed node highlighting");
                    Point view=preview.scroll.getViewport().getViewPosition();
                    require(Math.abs(preview.crosshairPoint().getX()-Math.round(leaf.x()*preview.canvas.zoom+5))<1,"Unsnapped crosshair left the pointer");
                    require(preview.snappedCoordinates.getText().equals("Snap: —"),"Snap readout remained enabled");
                    preview.crosshair.doClick(0);
                    require(preview.hovered==leaf && preview.canvas.getToolTipText()!=null,"Hiding crosshair removed inspection");
                    preview.snap.doClick(0);
                    preview.crosshair.doClick(0);
                    move(preview,leaf.x()*preview.canvas.zoom+11,leaf.y()*preview.canvas.zoom);
                    require(preview.hovered==leaf,"Within-radius hover missed");
                    move(preview,leaf.x()*preview.canvas.zoom+13,leaf.y()*preview.canvas.zoom);
                    require(preview.hovered==null,"Outside-radius hover snapped");
                    move(preview,leaf.x()*preview.canvas.zoom,leaf.y()*preview.canvas.zoom);
                    mouse(preview,MouseEvent.MOUSE_PRESSED,(int)Math.round(leaf.x()*preview.canvas.zoom),(int)Math.round(leaf.y()*preview.canvas.zoom));
                    require(preview.hovered==null && preview.crosshairPoint()==null && preview.canvas.getToolTipText()==null,"Dragging did not hide hover");
                    preview.navigation.mouseReleased(new MouseEvent(preview.canvas,MouseEvent.MOUSE_RELEASED,0,0,0,0,1,false,MouseEvent.BUTTON1));
                    require(preview.hovered==leaf,"Release did not restore hover");
                    preview.navigation.mouseExited(new MouseEvent(preview.canvas,MouseEvent.MOUSE_EXITED,0,0,0,0,0,false));
                    require(preview.hovered==null && preview.crosshairPoint()==null && preview.canvas.getToolTipText()==null,"Mouse exit left hover active");
                    require(original.equals(preview.canvas.scene.toJSONString()),"Hover changed the scene");
                    if(preview.canvas.code==1) {
                        var node=preview.canvas.nodes.get(2);
                        var label=preview.canvas.labelBounds(node,preview.canvas.metricsHost.getFontMetrics(preview.canvas.labelFont));
                        move(preview,label.getCenterX()*preview.canvas.zoom,label.getCenterY()*preview.canvas.zoom);
                        require(preview.hovered==null,"Label text was included in node snapping");
                        move(preview,(node.getXSelf()+node.getXParent())/2*preview.canvas.zoom,node.getYSelf()*preview.canvas.zoom);
                        require(preview.hovered==null,"Branch midpoint was included in node snapping");
                    }
                    require(Arrays.equals(pixels(before),pixels(sceneImage(preview))),"Hover changed exported drawing pixels");
                    require(preview.canvas.sceneDrawCount.get()==scenePaints+1,"Hover repainted the static scene");
                    require(view.equals(preview.scroll.getViewport().getViewPosition()),"Hover changed scrolling");
                });
            } finally { SwingUtilities.invokeAndWait(preview::disposeInteraction); }
        }
        testOverlap();
        testScrolledHover();
        testLargeTree();
        if(!GraphicsEnvironment.isHeadless()) testWindow();
        require(!egps2.UnifiedAccessPoint.isGULaunched(),"Hover launched the eGPS desktop");
        System.out.println("Seven-layout hover, snap switches, escaped tooltips, overlap, export isolation and viewport cache passed.");
    }

    private static void testOverlap() throws Exception {
        JSONObject scene=scene(1);
        scene.getJSONObject("tree").put("edges",JSON.parseArray("[[5,4,1],[4,1,0],[5,2,2],[5,3,3]]"));
        var preview=preview(scene);
        try {
            SwingUtilities.invokeAndWait(() -> {
                var leaf=preview.nodePoints.getFirst();
                var parent=preview.nodePoints.get(4);
                // Coincident displayed nodes have stable scene-ID precedence.
                var points=preview.canvas.nodes;
                require(points.get(1).getRealBranchLength()==0,"Zero-length fixture failed");
                move(preview,leaf.x()*preview.canvas.zoom,leaf.y()*preview.canvas.zoom);
                require(preview.hovered.id()==1,"Zero-length node hit was unstable");
                move(preview,parent.x()*preview.canvas.zoom,parent.y()*preview.canvas.zoom);
                require(preview.canvas.getToolTipText().contains("Node #5"),"Anonymous root node has no identifier");
            });
        } finally { SwingUtilities.invokeAndWait(preview::disposeInteraction); }
    }

    private static JSONObject largeScene() {
        int n=10000,next=n;
        JSONObject scene=scene(1),tree=scene.getJSONObject("tree");
        JSONArray labels=new JSONArray(),edges=new JSONArray(),names=new JSONArray(),values=new JSONArray();
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        for(int i=1;i<=n;i++) {labels.add("Leaf "+i);names.add("Leaf "+i);values.add(i%2==0?"a":"b");queue.add(i);}
        while(queue.size()>1) {
            int left=queue.remove(),right=queue.remove(),parent=++next;
            edges.add(new JSONArray(java.util.List.of(parent,left,1)));
            edges.add(new JSONArray(java.util.List.of(parent,right,1)));
            queue.add(parent);
        }
        tree.put("labels",labels);tree.put("edges",edges);tree.put("root",queue.remove());
        scene.getJSONObject("canvas").put("width",1600);
        scene.getJSONObject("canvas").put("height",1000);
        scene.getJSONArray("layers").getJSONObject(1).put("show",false);
        JSONObject track=new JSONObject(),columns=new JSONObject(true);
        track.put("type","track");track.put("names",names);track.put("columns",columns);track.put("legend","none");
        for(int i=0;i<20;i++) {
            JSONObject column=new JSONObject();
            column.put("levels",new JSONArray(java.util.List.of("a","b")));
            column.put("colors",new JSONArray(java.util.List.of("#2166ACFF","#B2182BFF")));
            column.put("values",values);columns.put("Track "+i,column);
        }
        scene.getJSONArray("layers").add(track);
        return scene;
    }

    private static void testScrolledHover() throws Exception {
        var preview=preview(scene(1));
        try {
            SwingUtilities.invokeAndWait(() -> {
                wheel(preview,-12);
                var node=preview.nodePoints.get(1);
                Dimension extent=preview.scroll.getViewport().getExtentSize(),size=preview.canvas.getPreferredSize();
                preview.scroll.getViewport().setViewPosition(new Point(
                    Math.max(0,Math.min(size.width-extent.width,(int)(node.x()*preview.canvas.zoom)-extent.width/2)),
                    Math.max(0,Math.min(size.height-extent.height,(int)(node.y()*preview.canvas.zoom)-extent.height/2))));
            });
            YyTreeBridge.awaitRendering(preview);
            SwingUtilities.invokeAndWait(() -> {
                var node=preview.nodePoints.get(1);
                move(preview,node.x()*preview.canvas.zoom+11,node.y()*preview.canvas.zoom);
                require(preview.hovered==node,"Zoomed, scrolled node missed the 12-pixel hit radius");
                move(preview,node.x()*preview.canvas.zoom+13,node.y()*preview.canvas.zoom);
                require(preview.hovered==null,"Zoom enlarged the interface hit radius");
                move(preview,node.x()*preview.canvas.zoom,node.y()*preview.canvas.zoom);
                Point view=preview.scroll.getViewport().getViewPosition();
                preview.scroll.getViewport().setViewPosition(new Point(view.x+40,view.y));
                require(preview.hovered==null && preview.canvas.getToolTipText()==null,"Scroll left a stale node tooltip");
            });
            YyTreeBridge.awaitRendering(preview);
            SwingUtilities.invokeAndWait(() -> require(preview.hovered==null,"Hover was not recomputed after scrolling"));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                preview.disposeInteraction();
                require(!preview.cache.isCurrent() && preview.canvas.preview==null,"Closed preview retained its image or interaction owner");
                require(preview.cache.ready.isCompletedExceptionally(),"Closed preview left a successful render wait active");
            });
        }
    }

    private static void testLargeTree() throws Exception {
        var preview=preview(largeScene());
        try {
            long[] elapsed=new long[240];
            SwingUtilities.invokeAndWait(() -> {
                int count=preview.canvas.sceneDrawCount.get();
                BufferedImage image=new BufferedImage(preview.getWidth(),preview.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=image.createGraphics();
                try {
                    for(int i=0;i<elapsed.length;i++) {
                        long start=System.nanoTime();
                        move(preview,100+i%500,100+i%300);
                        preview.printAll(graphics);
                        elapsed[i]=System.nanoTime()-start;
                    }
                } finally { graphics.dispose(); }
                require(preview.canvas.sceneDrawCount.get()==count,"Steady hover rebuilt the static tree");
                // Rapid changes, including returning to a cached view, must settle on the latest view.
                for(int i=0;i<30;i++) wheel(preview,i%2==0?-1:1);
            });
            YyTreeBridge.awaitRendering(preview);
            SwingUtilities.invokeAndWait(() -> require(preview.cache.isCurrent(),"Latest viewport render was not installed"));
            Arrays.sort(elapsed);
            double p95=elapsed[(int)(elapsed.length*.95)]/1e6;
            JSONObject report=new JSONObject();report.put("leaves",10000);report.put("tracks",20);
            report.put("hover_p95_ms",p95);report.put("hover_static_redraws",0);
            Files.createDirectories(Path.of("logs"));
            String scale=System.getProperty("sun.java2d.uiScale","headless");
            Files.writeString(Path.of("logs/yytree-hover-"+scale+".json"),report.toJSONString());
            System.out.printf(Locale.ROOT,"10000 leaves / 20 tracks: steady hover + paint p95 %.2f ms, no static redraws.%n",p95);
            require(p95<100,"Hover blocked the EDT for over 100 ms");
        } finally { SwingUtilities.invokeAndWait(preview::disposeInteraction); }
    }

    private static void testWindow() throws Exception {
        var bridge=new YyTreeBridge();
        try {
            bridge.showPlot(scene(1).toJSONString(),true);
            AtomicReference<YyTreePreview> shown=new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                for(Window window:Window.getWindows()) if(window instanceof JFrame frame && frame.isDisplayable()
                    && frame.getContentPane() instanceof YyTreePreview preview) shown.set(preview);
                var preview=shown.get();
                assertCachePixels(preview);
                var node=preview.nodePoints.stream().filter(point->point.id()==4).findFirst().orElseThrow();
                move(preview,node.x()*preview.canvas.zoom+5,node.y()*preview.canvas.zoom+3);
                require(preview.hovered==node,"Real window node did not highlight");
                try {
                    Files.createDirectories(Path.of("temp/yytree-qa"));
                    double scale=preview.canvas.getGraphicsConfiguration().getDefaultTransform().getScaleX();
                    String suffix=(int)(scale*100)+"pct.png";
                    BufferedImage image=new BufferedImage((int)Math.ceil(preview.getWidth()*scale),(int)Math.ceil(preview.getHeight()*scale),BufferedImage.TYPE_INT_RGB);
                    Graphics2D g=image.createGraphics();try{g.scale(scale,scale);preview.printAll(g);}finally{g.dispose();}
                    ImageIO.write(image,"png",Path.of("temp/yytree-qa/hover-"+suffix).toFile());
                    JToolTip tip=preview.canvas.createToolTip();tip.setTipText(preview.canvas.getToolTipText());tip.setSize(tip.getPreferredSize());
                    image=new BufferedImage((int)Math.ceil(tip.getWidth()*scale),(int)Math.ceil(tip.getHeight()*scale),BufferedImage.TYPE_INT_RGB);
                    g=image.createGraphics();try{g.scale(scale,scale);tip.printAll(g);}finally{g.dispose();}
                    ImageIO.write(image,"png",Path.of("temp/yytree-qa/hover-tooltip-"+suffix).toFile());
                    preview.snap.doClick(0);
                } catch(Exception e) { throw new RuntimeException(e); }
            });
            bridge.showPlot(scene(2).toJSONString(),true);
            SwingUtilities.invokeAndWait(() -> {
                for(Window window:Window.getWindows()) if(window instanceof JFrame frame && frame.isDisplayable()
                    && frame.getContentPane() instanceof YyTreePreview preview && preview!=shown.get())
                    require(preview.snap.isSelected() && preview.crosshair.isSelected() && preview.hovered==null,"Window interaction state leaked");
            });
        } finally { bridge.closeAll(); }
    }

    private static void layout(Container container) {
        container.doLayout();
        for(Component child:container.getComponents()) if(child instanceof Container nested) layout(nested);
    }
    private static void assertCachePixels(YyTreePreview preview) {
        Rectangle view=preview.scroll.getViewport().getViewRect();
        double scale=preview.canvas.getGraphicsConfiguration().getDefaultTransform().getScaleX();
        var expected=new BufferedImage((int)Math.ceil(view.width*scale),(int)Math.ceil(view.height*scale),BufferedImage.TYPE_INT_RGB);
        var actual=new BufferedImage(expected.getWidth(),expected.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=expected.createGraphics();
        try {
            g.setColor(Color.WHITE);g.fillRect(0,0,expected.getWidth(),expected.getHeight());
            g.scale(scale,scale);g.translate(-view.x,-view.y);g.scale(preview.canvas.zoom,preview.canvas.zoom);
            preview.canvas.drawScene(g);
        } finally {g.dispose();}
        preview.navigation.mouseExited(new MouseEvent(preview.canvas,MouseEvent.MOUSE_EXITED,0,0,0,0,0,false));
        g=actual.createGraphics();
        try {g.scale(scale,scale);g.translate(-view.x,-view.y);preview.canvas.printAll(g);} finally {g.dispose();}
        require(Arrays.equals(pixels(expected),pixels(actual)),"Device-resolution cache changed scene pixels");
    }
    private static void move(YyTreePreview preview,double x,double y) { mouse(preview,MouseEvent.MOUSE_MOVED,(int)Math.round(x),(int)Math.round(y)); }
    private static void mouse(YyTreePreview preview,int id,int x,int y) {
        MouseEvent event=new MouseEvent(preview.canvas,id,0,0,x,y,1,false,id==MouseEvent.MOUSE_PRESSED?MouseEvent.BUTTON1:MouseEvent.NOBUTTON);
        if(id==MouseEvent.MOUSE_MOVED)preview.navigation.mouseMoved(event);else preview.navigation.mousePressed(event);
    }
    private static void wheel(YyTreePreview preview,int rotation) {
        Point view=preview.scroll.getViewport().getViewPosition();
        preview.navigation.mouseWheelMoved(new MouseWheelEvent(preview.canvas,MouseEvent.MOUSE_WHEEL,0,0,view.x+100,view.y+100,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,rotation));
    }
    private static BufferedImage sceneImage(YyTreePreview preview) {
        var image=new BufferedImage(preview.canvas.logicalWidth,preview.canvas.logicalHeight,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();try{g.setColor(Color.WHITE);g.fillRect(0,0,image.getWidth(),image.getHeight());preview.canvas.drawScene(g);}finally{g.dispose();}
        return image;
    }
    private static int[] pixels(BufferedImage image) { return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()); }
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
