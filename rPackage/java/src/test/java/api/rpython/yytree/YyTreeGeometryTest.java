package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import module.evolview.model.tree.GraphicsNode;
import javax.swing.SwingUtilities;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Independent CLI regression entry point; no eGPS frame or R callback is needed. */
public final class YyTreeGeometryTest {
    private static final String SCENE = """
        {"schema_version":1,
         "tree":{"labels":["A","B","C"],"root":5,
           "edges":[[5,4,1],[4,1,0.12345678912345],[4,2,2],[5,3,3]]},
         "canvas":{"width":1000,"height":800,"margins":[20,40,80,40]},
         "layers":[{"type":"layout","code":1},
           {"type":"leaf_render","size":6,"fill":"#0000FFFF"},
           {"type":"leaf_render","df":[{"name":"A","fill":"#FF0000FF"}]}]}
        """;

    public static void main(String[] args) throws Exception {
        String originalTheme=javax.swing.UIManager.getLookAndFeel().getClass().getName();
        new YyTreeBridge().probeRuntime();
        for (int code=1; code<=7; code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            AtomicReference<YyTreeBridge.YyTreePanel> reference=new AtomicReference<>();
            SwingUtilities.invokeAndWait(()->reference.set(new YyTreeBridge.YyTreePanel(scene)));
            YyTreeBridge.YyTreePanel panel=reference.get();
            panel.prepare();
            require(panel.leaves.size()==3,"Leaf count changed");
            require(panel.leaves.getFirst().getName().equals("A"),"Input order changed");
            GraphicsNode a=panel.named.get("A"),b=panel.named.get("B"),c=panel.named.get("C");
            require(panel.mrca("A","B")==a.getParent(),"Wrong MRCA");
            require(panel.mrca("A","C")==panel.root,"Root MRCA failed");
            require(a.getRealBranchLength()==0.12345678912345,"Branch precision lost");
            require(panel.sizes.get(a)==6,"Partial style reset earlier radius");
            require(panel.fills.get(a).getRed()==255,"Partial style did not override color");
            require(panel.fills.get(b).getBlue()==255,"Local style leaked to another leaf");
            JSONObject annotations=JSON.parseObject("""
                {"type":"inner_node_annotation","shape":"rect","df":[
                  {"name1":"A","name2":"B","text":"AB","shape":"ellipse"},
                  {"name1":"A","name2":"C","text":"AC","shape":"triangle"}]}
                """);
            var annotationRows=panel.rows(annotations);
            require(annotationRows.get(0)!=annotationRows.get(1),"Annotation rows share a mutable object");
            require(annotationRows.get(0).getString("text").equals("AB"),"First annotation was overwritten");
            require(annotationRows.get(1).getString("shape").equals("triangle"),"Second annotation shape lost");
            require(!annotations.containsKey("text") && annotations.getString("shape").equals("rect"),"Row merge changed layer defaults");
            JSONObject styles=JSON.parseObject("""
                {"type":"leaf_render","df":[{"name":"A","size":12},{"name":"B","fill":"#FFD700FF"}]}
                """);
            panel.applyStyle(styles);
            require(panel.sizes.get(a)==12 && panel.sizes.get(b)==6,"One style row leaked size into another row");
            require(!styles.containsKey("size"),"Style merge changed the layer");
            if(code==2)require(panel.tips.get(a).x==panel.tips.get(b).x,"Rectangular tips do not align");
            if(code==4)for(GraphicsNode node:panel.leaves) {
                Point2D point=panel.tips.get(node);
                require(Math.abs(point.distance(panel.centerX,panel.centerY)-panel.outer)<1e-8,"Circular tips do not align");
            }
            if(code==3 || code==4) for(GraphicsNode node:panel.nodes.values()) {
                double angle=-Math.toRadians(node.getAngleIfNeeded());
                Point2D expected=new Point2D.Double(panel.centerX+node.getRadicusIfNeeded()*Math.cos(angle),panel.centerY+node.getRadicusIfNeeded()*Math.sin(angle));
                require(expected.distance(node.getXSelf(),node.getYSelf())<1e-7,"Circular center disagrees with MTV coordinates");
                if(node!=panel.root) {
                    GraphicsNode parent=(GraphicsNode)node.getParent();
                    double radius=Point2D.distance(panel.centerX,panel.centerY,parent.getXSelf(),parent.getYSelf());
                    expected=new Point2D.Double(panel.centerX+radius*Math.cos(angle),panel.centerY+radius*Math.sin(angle));
                    require(expected.distance(node.getXParent(),node.getYParent())<1e-7,"Circular branch disconnected from parent arc");
                }
            }
            if(code==6)for(GraphicsNode node:panel.leaves) {
                require(panel.tips.get(node).distance(panel.arch(panel.outer,node.getDoubleVariable()))<1e-8,"Arch tips do not align");
            }
            JSONObject relative=JSON.parseObject("{\"mode\":\"relative\",\"x\":0.4,\"y\":0.25}");
            JSONObject percent=JSON.parseObject("{\"mode\":\"percent\",\"x\":40,\"y\":25}");
            JSONObject pixel=JSON.parseObject("{\"mode\":\"pixel\",\"x\":400,\"y\":200}");
            require(panel.coordinate(relative,"x",true)==panel.coordinate(percent,"x",true),"Percent mismatch");
            require(panel.coordinate(relative,"y",false)==panel.coordinate(pixel,"y",false),"Pixel mismatch");
            require(panel.coordinate(relative,"x",true)==panel.coordinate(pixel,"x",true),"Relative mismatch");
        }
        for(int code=3;code<=6;code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"left\"}"));
            scene.getJSONObject("tree").getJSONArray("labels").set(1,"Long leaf name");
            int[] left=labelPixels(prepare(scene));
            scene.getJSONArray("layers").getJSONObject(3).put("align","right");
            int[] right=labelPixels(prepare(scene));
            require(!Arrays.equals(left,right),"Curved layout "+code+" ignored label alignment");
            double[] textX=new double[3];int index=0;
            for(String align:new String[]{"left","center","right"}) {
                scene.getJSONArray("layers").getJSONObject(3).put("align",align);
                var panel=prepare(scene);var metrics=panel.metricsHost.getFontMetrics(panel.labelFont);
                for(GraphicsNode leaf:panel.leaves) {
                    var bounds=panel.labelBounds(leaf,metrics);var anchor=panel.anchor(leaf,6);
                    double start=anchor.x-(anchor.x<panel.centerX?panel.labelWidth-10:0);
                    double fraction=align.equals("left")?0:align.equals("center")?.5:1;
                    require(Math.abs(bounds.getX()-start-fraction*(panel.labelWidth-10-bounds.getWidth()))<1e-7,"Incorrect text alignment geometry");
                }
                textX[index++]=panel.labelBounds(panel.named.get("A"),metrics).getX();
            }
            require(textX[0]<textX[1] && textX[1]<textX[2],"Text alignment anchors are not ordered");
        }
        for(int code:new int[]{5,6}) for(int[] size:new int[][]{{400,1400},{1400,400},{800,800}}) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONObject("canvas").put("width",size[0]);
            scene.getJSONObject("canvas").put("height",size[1]);
            YyTreeBridge.YyTreePanel panel=prepare(scene);
            for(GraphicsNode node:panel.nodes.values()) {
                require(node.getXSelf()>=40 && node.getXSelf()<=size[0]-40,"Arch node outside horizontal margins");
                require(node.getYSelf()>=20 && node.getYSelf()<=size[1]-80,"Arch node outside vertical margins");
                if(node!=panel.root) {
                    GraphicsNode parent=(GraphicsNode)node.getParent();
                    Point2D expected=panel.arch(parent.getRadicusIfNeeded(),node.getDoubleVariable());
                    require(expected.distance(node.getXParent(),node.getYParent())<1e-7,"Arch branch disconnected from parent contour");
                }
            }
            for(GraphicsNode leaf:panel.leaves) {
                Point2D tip=panel.tips.get(leaf);
                require(tip.getX()>=40 && tip.getX()<=size[0]-40,"Aligned arch tip outside margins");
            }
        }
        for(int code=1;code<=7;code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            for(Object edge:scene.getJSONObject("tree").getJSONArray("edges")) ((com.alibaba.fastjson.JSONArray)edge).set(2,0);
            var panel=prepare(scene);
            for(GraphicsNode node:panel.nodes.values()) {
                require(Double.isFinite(node.getXSelf()) && Double.isFinite(node.getYSelf()),"Zero-length tree produced nonfinite coordinates");
                require(node.getRealBranchLength()==0,"Display calculation changed zero branch length");
            }
        }
        for(int code=3;code<=6;code++) for(int[] size:new int[][]{{400,1400},{1400,400},{800,800}}) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONObject("canvas").put("width",size[0]);scene.getJSONObject("canvas").put("height",size[1]);
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"center\"}"));
            scene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"heatmap\",\"titles\":[\"one\",\"two\"],\"width\":16,\"legend\":\"none\"}"));
            var panel=prepare(scene);
            for(GraphicsNode leaf:panel.leaves) {
                var bounds=panel.labelBounds(leaf,panel.metricsHost.getFontMetrics(panel.labelFont));
                require(bounds.getMinX()>=0 && bounds.getMaxX()<=size[0] && bounds.getMinY()>=0 && bounds.getMaxY()<=size[1],"Curved label outside canvas");
                bounds=panel.band(leaf,leaf,panel.outer+panel.labelWidth+12,32).getBounds2D();
                require(bounds.getMinX()>=0 && bounds.getMaxX()<=size[0] && bounds.getMinY()>=0 && bounds.getMaxY()<=size[1],"Curved track outside canvas");
                Point2D midpoint=panel.anchor(leaf,panel.labelWidth+28);
                require(panel.band(leaf,leaf,panel.outer+panel.labelWidth+12,32).intersects(midpoint.getX()-.01,midpoint.getY()-.01,.02,.02),"Curved track is not centered on leaf "+leaf.getName()+" in layout "+code);
            }
        }
        for(int code=1;code<=7;code++) {
            JSONObject scene=JSON.parseObject(SCENE,Feature.OrderedField);
            scene.getJSONArray("layers").getJSONObject(0).put("code",code);
            scene.getJSONArray("layers").add(JSON.parseObject("""
                {"type":"track","names":["C","A"],"width":20,"legend":"none",
                 "columns":{"group":{"levels":["x","z"],"colors":["#FF0000FF","#0000FFFF"],"values":["z","x"]}}}
                """,Feature.OrderedField));
            JSONObject heat=JSON.parseObject("""
                {"type":"heatmap","names":["B","A","C"],"width":20,"legend":"none",
                 "titles":["value"],"cells":[["#00FF00FF"],[null],["#FFD700FF"]],"limits":[0,1],"palette":[]}
                """);
            for(int i=0;i<256;i++) heat.getJSONArray("palette").add("#000000FF");
            scene.getJSONArray("layers").add(heat);
            var panel=prepare(scene);
            var image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_ARGB);
            var graphics=image.createGraphics();graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,panel.logicalWidth,panel.logicalHeight);
            panel.drawTracks(graphics);graphics.dispose();
            int[] category={0xFF0000,0xFFFFFF,0x0000FF},numeric={0xFFFFFF,0x00FF00,0xFFD700};
            int index=0;
            for(GraphicsNode leaf:panel.leaves) {
                Point2D first=trackSample(panel,leaf,22),second=trackSample(panel,leaf,46);
                require((image.getRGB((int)first.getX(),(int)first.getY())&0xFFFFFF)==category[index],"Category row/name matching or missing leaf failed in layout "+code);
                require((image.getRGB((int)second.getX(),(int)second.getY())&0xFFFFFF)==numeric[index],"Heatmap row/name matching, missing value or track order failed in layout "+code);
                index++;
            }
        }
        JSONObject fontScene=JSON.parseObject(SCENE,Feature.OrderedField);
        int defaultSize=prepare(fontScene).labelFont.getSize();
        fontScene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"left\",\"font_size\":36}"));
        fontScene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"leaf_labels\",\"show\":true,\"align\":\"right\"}"));
        require(prepare(fontScene).labelFont.getSize()==defaultSize,"Last label settings retained an earlier font override");
        JSONObject legendScene=JSON.parseObject(SCENE,Feature.OrderedField);
        legendScene.getJSONArray("layers").add(JSON.parseObject("{\"type\":\"heatmap\",\"titles\":[\"x\"],\"width\":16,\"legend\":\"top_right\",\"limits\":[-1e200,1e200]}"));
        var legendPanel=prepare(legendScene);
        int requiredWidth=legendPanel.metricsHost.getFontMetrics(legendPanel.labelFont.deriveFont(10f)).stringWidth("-1.00e+200")+28;
        require(legendPanel.legendWidths.get("top_right")>=requiredWidth,"Scientific heatmap legend labels exceed their reserved width");
        require(!egps2.UnifiedAccessPoint.isGULaunched(),"The eGPS desktop was launched");
        require(originalTheme.equals(javax.swing.UIManager.getLookAndFeel().getClass().getName()),"Probe or layout initialized the preview theme");
        require(java.awt.Frame.getFrames().length==0,"Probe or layout opened a window");
        System.out.println("Seven-layout geometry, MRCA, style, precision and coordinate checks passed.");
    }
    private static YyTreeBridge.YyTreePanel prepare(JSONObject scene) throws Exception {
        AtomicReference<YyTreeBridge.YyTreePanel> reference=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->reference.set(new YyTreeBridge.YyTreePanel(scene)));
        reference.get().prepare();return reference.get();
    }
    private static int[] labelPixels(YyTreeBridge.YyTreePanel panel) {
        BufferedImage image=new BufferedImage(panel.logicalWidth,panel.logicalHeight,BufferedImage.TYPE_INT_ARGB);
        var graphics=image.createGraphics();graphics.setFont(panel.labelFont);panel.drawLabels(graphics);graphics.dispose();
        return image.getRGB(0,0,panel.logicalWidth,panel.logicalHeight,null,0,panel.logicalWidth);
    }
    private static Point2D trackSample(YyTreeBridge.YyTreePanel panel,GraphicsNode leaf,double offset) {
        if(panel.code==5 || panel.code==6) {
            double p=Math.max(1,Math.min(panel.archTotal-1,leaf.getDoubleVariable()));
            return panel.arch(panel.outer+offset,p);
        }
        Point2D.Double point=panel.anchor(leaf,offset);
        if(panel.code==1 || panel.code==2 || panel.code==7)
            point.y=Math.max(panel.treeBounds.getMinY()+1,Math.min(panel.treeBounds.getMaxY()-1,point.y));
        return point;
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
